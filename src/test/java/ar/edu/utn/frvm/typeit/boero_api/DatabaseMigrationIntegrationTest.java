package ar.edu.utn.frvm.typeit.boero_api;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.support.IntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("20260713200200");
    assertThat(tableCount()).isEqualTo(20);
    assertThat(institutionCount()).isPositive();
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
}
