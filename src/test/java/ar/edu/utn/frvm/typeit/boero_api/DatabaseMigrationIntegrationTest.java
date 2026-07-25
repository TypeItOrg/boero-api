package ar.edu.utn.frvm.typeit.boero_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.frvm.typeit.boero_api.support.IntegrationTest;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@IntegrationTest
class DatabaseMigrationIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"));

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @Autowired private Flyway flyway;
  @Autowired private JdbcTemplate jdbcTemplate;

  @DynamicPropertySource
  static void infrastructureProperties(final DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    registry.add("spring.flyway.enabled", () -> true);
    registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/dev");
    registry.add("spring.flyway.sql-migration-prefix", () -> "");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @Test
  @DisplayName("Should migrate an empty PostgreSQL database and validate the JPA model")
  void shouldMigrateSchemaAndDevelopmentData() {
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("20260725041357");
    assertThat(tableCount()).isEqualTo(20);
    assertThat(institutionCount()).isPositive();
    assertThat(tenantRelationshipConstraintCount()).isEqualTo(7);
    assertThat(activePersonDocumentIndexCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should enforce document uniqueness only for active people")
  void shouldEnforceDocumentUniquenessOnlyForActivePeople() {
    UUID institutionId = firstInstitutionId();
    UUID deletedPersonId = UUID.randomUUID();
    UUID activePersonId = UUID.randomUUID();
    String documentNumber = randomDocumentNumber();

    insertPerson(deletedPersonId, institutionId, documentNumber, true);
    try {
      insertPerson(activePersonId, institutionId, documentNumber, false);
      assertThatThrownBy(
              () -> insertPerson(UUID.randomUUID(), institutionId, documentNumber, false))
          .isInstanceOf(DataIntegrityViolationException.class);
    } finally {
      jdbcTemplate.update(
          "DELETE FROM people WHERE person_id IN (?, ?)", deletedPersonId, activePersonId);
    }
  }

  private Integer tableCount() {
    return jdbcTemplate.queryForObject(
        """
        SELECT COUNT(*)
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name <> 'flyway_schema_history'
        """,
        Integer.class);
  }

  private Integer institutionCount() {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM institutions", Integer.class);
  }

  private UUID firstInstitutionId() {
    return jdbcTemplate.queryForObject(
        "SELECT institution_id FROM institutions ORDER BY institution_id LIMIT 1", UUID.class);
  }

  private String randomDocumentNumber() {
    return jdbcTemplate.queryForObject(
        "SELECT LPAD((floor(random() * 100000000))::bigint::text, 8, '0')", String.class);
  }

  private void insertPerson(
      UUID personId, UUID institutionId, String documentNumber, boolean deleted) {
    jdbcTemplate.update(
        """
        INSERT INTO people (
          person_id, institution_id, document_number, first_name, last_name,
          created_at, updated_at, deleted
        ) VALUES (?, ?, ?, 'Ana', 'Garcia', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
        """,
        personId,
        institutionId,
        documentNumber,
        deleted);
  }

  private Integer tenantRelationshipConstraintCount() {
    return jdbcTemplate.queryForObject(
        """
        SELECT COUNT(*)
        FROM pg_constraint
        WHERE conname IN (
          'people_address_institution_fk',
          'users_person_institution_fk',
          'students_person_institution_fk',
          'guardian_profiles_person_institution_fk',
          'student_guardians_student_institution_fk',
          'student_guardians_guardian_institution_fk',
          'person_role_assignments_person_institution_fk'
        )
        """,
        Integer.class);
  }

  private Integer activePersonDocumentIndexCount() {
    return jdbcTemplate.queryForObject(
        """
        SELECT COUNT(*)
        FROM pg_indexes
        WHERE schemaname = 'public'
          AND tablename = 'people'
          AND indexname = 'people_active_document_number_unique'
          AND indexdef LIKE '%WHERE (deleted = false)%'
        """,
        Integer.class);
  }
}
