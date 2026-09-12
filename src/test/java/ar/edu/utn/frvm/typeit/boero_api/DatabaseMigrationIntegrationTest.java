package ar.edu.utn.frvm.typeit.boero_api;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.support.IntegrationTest;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class DatabaseMigrationIntegrationTest extends DatabaseMigrationTestSupport {

  @Autowired private Flyway flyway;

  @Test
  @DisplayName("Should migrate an empty PostgreSQL database and validate the JPA model")
  void shouldMigrateSchemaAndDevelopmentData() {
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("20260912045658");
    assertThat(nonUtcEventTimestampColumnCount()).isZero();
    assertThat(utcEventTimestampColumnCount()).isPositive();
    assertThat(tableCount()).isEqualTo(38);
    assertThat(institutionCount()).isPositive();
    assertThat(tenantRelationshipConstraintCount()).isEqualTo(8);
    assertThat(activePersonDocumentIndexCount()).isEqualTo(1);
    assertThat(passwordResetTokenUserUniqueIndexCount()).isEqualTo(1);
    assertThat(pgTrgmExtensionCount()).isEqualTo(1);
    assertThat(searchTrigramIndexCount()).isEqualTo(17);
  }

  @Test
  @DisplayName("Should store WebAuthn credential ids as text and cascade passkeys on user delete")
  void shouldStoreCredentialIdsAsTextAndCascadePasskeys() {
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT data_type
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'passkey_credentials'
                  AND column_name = 'credential_id'
                """,
                String.class))
        .isEqualTo("text");
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT delete_rule
                FROM information_schema.referential_constraints
                WHERE constraint_schema = 'public'
                  AND constraint_name IN (
                    SELECT constraint_name
                    FROM information_schema.key_column_usage
                    WHERE table_schema = 'public'
                      AND table_name = 'passkey_credentials'
                      AND column_name = 'user_id'
                  )
                """,
                String.class))
        .isEqualTo("CASCADE");
  }

  @Test
  @DisplayName("Should make accent-insensitive contains searches indexable")
  void shouldMakeContainsSearchesIndexable() {
    jdbcTemplate.execute("SET enable_seqscan = off");
    try {
      final List<String> plan =
          jdbcTemplate.queryForList(
              """
              EXPLAIN (COSTS OFF)
              SELECT institution_id
              FROM institutions
              WHERE boero_normalize_text(name) LIKE '%boero%'
              """,
              String.class);

      assertThat(plan).anyMatch(line -> line.contains("institutions_name_search_trgm_idx"));
    } finally {
      jdbcTemplate.execute("RESET enable_seqscan");
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
          'person_role_assignments_person_institution_fk',
          'course_class_teachers_person_institution_fk'
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

  private Integer passwordResetTokenUserUniqueIndexCount() {
    return jdbcTemplate.queryForObject(
        """
        SELECT COUNT(*)
        FROM pg_indexes
        WHERE schemaname = 'public'
          AND tablename = 'institutional_password_reset_tokens'
          AND indexname = 'institutional_password_reset_tokens_user_unique'
          AND indexdef LIKE 'CREATE UNIQUE INDEX%'
        """,
        Integer.class);
  }

  private Integer pgTrgmExtensionCount() {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM pg_extension WHERE extname = 'pg_trgm'", Integer.class);
  }

  private Integer utcEventTimestampColumnCount() {
    return jdbcTemplate.queryForObject(
        """
        SELECT COUNT(*)
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND column_name IN (
            'created_at', 'updated_at', 'deleted_at',
            'started_at', 'ended_at', 'expires_at', 'used_at'
          )
          AND data_type = 'timestamp with time zone'
        """,
        Integer.class);
  }

  private Integer nonUtcEventTimestampColumnCount() {
    return jdbcTemplate.queryForObject(
        """
        SELECT COUNT(*)
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND column_name IN (
            'created_at', 'updated_at', 'deleted_at',
            'started_at', 'ended_at', 'expires_at', 'used_at'
          )
          AND data_type <> 'timestamp with time zone'
        """,
        Integer.class);
  }

  private Integer searchTrigramIndexCount() {
    return jdbcTemplate.queryForObject(
        """
        SELECT COUNT(*)
        FROM pg_indexes
        WHERE schemaname = 'public'
          AND indexname LIKE '%_search_trgm_idx'
        """,
        Integer.class);
  }
}
