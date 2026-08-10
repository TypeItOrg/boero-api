package ar.edu.utn.frvm.typeit.boero_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.frvm.typeit.boero_api.search.SearchEntityType;
import ar.edu.utn.frvm.typeit.boero_api.search.SearchService;
import ar.edu.utn.frvm.typeit.boero_api.support.IntegrationTest;
import java.time.LocalDate;
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
  @Autowired private SearchService searchService;

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
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("20260809031336");
    assertThat(tableCount()).isEqualTo(28);
    assertThat(institutionCount()).isPositive();
    assertThat(tenantRelationshipConstraintCount()).isEqualTo(7);
    assertThat(activePersonDocumentIndexCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should search accent-insensitively with unordered prefixes")
  void shouldSearchWithUnorderedPrefixes() {
    final var result = searchService.platformSummary("musica boe", 5);

    assertThat(result.groups())
        .filteredOn(group -> group.entityType() == SearchEntityType.INSTITUTION)
        .singleElement()
        .satisfies(
            group ->
                assertThat(group.items())
                    .singleElement()
                    .satisfies(item -> assertThat(item.title()).contains("Música Felipe Boero")));
  }

  @Test
  @DisplayName("Should allow an open-ended study plan validity")
  void shouldAllowAnOpenEndedStudyPlanValidity() {
    final UUID institutionId = firstInstitutionId();
    final UUID trainingPathId = UUID.randomUUID();
    final UUID studyPlanId = UUID.randomUUID();
    try {
      insertTrainingPath(trainingPathId, institutionId);
      jdbcTemplate.update(
          """
          INSERT INTO study_plans (
            study_plan_id, institution_id, training_path_id, name,
            effective_from, status, created_at, updated_at
          ) VALUES (?, ?, ?, ?, DATE '2031-03-01', 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          """,
          studyPlanId,
          institutionId,
          trainingPathId,
          "Open-ended Plan " + studyPlanId);

      assertThat(
              jdbcTemplate.queryForObject(
                  "SELECT effective_to FROM study_plans WHERE study_plan_id = ?",
                  LocalDate.class,
                  studyPlanId))
          .isNull();
    } finally {
      jdbcTemplate.update("DELETE FROM study_plans WHERE study_plan_id = ?", studyPlanId);
      jdbcTemplate.update("DELETE FROM training_paths WHERE training_path_id = ?", trainingPathId);
    }
  }

  @Test
  @DisplayName("Should reject a study plan end date without a start date")
  void shouldRejectAStudyPlanEndDateWithoutAStartDate() {
    final UUID institutionId = firstInstitutionId();
    final UUID trainingPathId = UUID.randomUUID();
    final UUID studyPlanId = UUID.randomUUID();
    try {
      insertTrainingPath(trainingPathId, institutionId);
      assertThatThrownBy(
              () ->
                  jdbcTemplate.update(
                      """
                      INSERT INTO study_plans (
                        study_plan_id, institution_id, training_path_id, name,
                        effective_to, status, created_at, updated_at
                      ) VALUES (?, ?, ?, ?, DATE '2031-12-15', 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                      """,
                      studyPlanId,
                      institutionId,
                      trainingPathId,
                      "Missing Start Plan " + studyPlanId))
          .isInstanceOf(DataIntegrityViolationException.class);
    } finally {
      jdbcTemplate.update("DELETE FROM study_plans WHERE study_plan_id = ?", studyPlanId);
      jdbcTemplate.update("DELETE FROM training_paths WHERE training_path_id = ?", trainingPathId);
    }
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

  @Test
  @DisplayName("Should allow only one active academic year per institution")
  void shouldAllowOnlyOneActiveAcademicYearPerInstitution() {
    final UUID institutionId = firstInstitutionId();
    final UUID firstYearId = UUID.randomUUID();
    final UUID secondYearId = UUID.randomUUID();
    try {
      insertAcademicYear(firstYearId, institutionId, 2031);
      assertThatThrownBy(() -> insertAcademicYear(secondYearId, institutionId, 2032))
          .isInstanceOf(DataIntegrityViolationException.class);
    } finally {
      jdbcTemplate.update(
          "DELETE FROM academic_years WHERE academic_year_id IN (?, ?)", firstYearId, secondYearId);
    }
  }

  @Test
  @DisplayName("Should reject a study plan space that crosses institutions")
  void shouldRejectCrossInstitutionStudyPlanSpace() {
    final UUID firstInstitutionId = firstInstitutionId();
    final UUID secondInstitutionId = UUID.randomUUID();
    final UUID trainingPathId = UUID.randomUUID();
    final UUID studyPlanId = UUID.randomUUID();
    final UUID academicSpaceId = UUID.randomUUID();
    final UUID studyPlanSpaceId = UUID.randomUUID();
    try {
      insertTestInstitution(secondInstitutionId);
      insertTrainingPath(trainingPathId, firstInstitutionId);
      insertStudyPlan(studyPlanId, firstInstitutionId, trainingPathId);
      insertAcademicSpace(academicSpaceId, secondInstitutionId);
      assertThatThrownBy(
              () ->
                  insertStudyPlanSpace(
                      studyPlanSpaceId, firstInstitutionId, studyPlanId, academicSpaceId))
          .isInstanceOf(DataIntegrityViolationException.class);
    } finally {
      jdbcTemplate.update(
          "DELETE FROM study_plan_spaces WHERE study_plan_space_id = ?", studyPlanSpaceId);
      jdbcTemplate.update(
          "DELETE FROM academic_spaces WHERE academic_space_id = ?", academicSpaceId);
      jdbcTemplate.update("DELETE FROM study_plans WHERE study_plan_id = ?", studyPlanId);
      jdbcTemplate.update("DELETE FROM training_paths WHERE training_path_id = ?", trainingPathId);
      jdbcTemplate.update("DELETE FROM institutions WHERE institution_id = ?", secondInstitutionId);
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

  private void insertTestInstitution(final UUID institutionId) {
    final UUID cityId =
        jdbcTemplate.queryForObject(
            "SELECT city_id FROM institutions ORDER BY institution_id LIMIT 1", UUID.class);
    jdbcTemplate.update(
        """
        INSERT INTO institutions (
          institution_id, city_id, name, slug, active, created_at, updated_at
        ) VALUES (?, ?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        institutionId,
        cityId,
        "Migration Institution " + institutionId,
        "migration-" + institutionId);
  }

  private void insertAcademicYear(final UUID id, final UUID institutionId, final int year) {
    jdbcTemplate.update(
        """
        INSERT INTO academic_years (
          academic_year_id, institution_id, year, start_date, end_date,
          status, created_at, updated_at
        ) VALUES (
          ?, ?, ?, DATE '2031-03-01', DATE '2031-12-01',
          'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """,
        id,
        institutionId,
        year);
  }

  private void insertTrainingPath(final UUID id, final UUID institutionId) {
    jdbcTemplate.update(
        """
        INSERT INTO training_paths (
          training_path_id, institution_id, name, active, created_at, updated_at
        ) VALUES (?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        id,
        institutionId,
        "Migration Path " + id);
  }

  private void insertStudyPlan(final UUID id, final UUID institutionId, final UUID trainingPathId) {
    jdbcTemplate.update(
        """
        INSERT INTO study_plans (
          study_plan_id, institution_id, training_path_id, name, status, created_at, updated_at
        ) VALUES (?, ?, ?, ?, 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        id,
        institutionId,
        trainingPathId,
        "Migration Plan " + id);
  }

  private void insertAcademicSpace(final UUID id, final UUID institutionId) {
    jdbcTemplate.update(
        """
        INSERT INTO academic_spaces (
          academic_space_id, institution_id, name, type, active, created_at, updated_at
        ) VALUES (?, ?, ?, 'SUBJECT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        id,
        institutionId,
        "Migration Space " + id);
  }

  private void insertStudyPlanSpace(
      final UUID id, final UUID institutionId, final UUID studyPlanId, final UUID academicSpaceId) {
    jdbcTemplate.update(
        """
        INSERT INTO study_plan_spaces (
          study_plan_space_id, institution_id, study_plan_id, academic_space_id,
          requirement_type, display_order, approval_mode, created_at, updated_at
        ) VALUES (
          ?, ?, ?, ?, 'REQUIRED', 1, 'PROMOTION', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """,
        id,
        institutionId,
        studyPlanId,
        academicSpaceId);
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
