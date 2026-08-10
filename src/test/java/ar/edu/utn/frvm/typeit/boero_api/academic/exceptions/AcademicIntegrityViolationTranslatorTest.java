package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class AcademicIntegrityViolationTranslatorTest {

  @Test
  @DisplayName("Should map a duplicated plan-space order to displayOrder")
  void mapsDuplicateOrderToDisplayOrder() {
    final var translated =
        AcademicIntegrityViolationTranslator.translate(
            violation("study_plan_spaces_plan_level_order_unique"));

    assertThat(translated).isInstanceOf(AcademicConflictException.class);
    assertThat(((AcademicConflictException) translated).fieldErrors())
        .containsEntry("displayOrder", AcademicMessages.DUPLICATE_ORDER);
  }

  @Test
  @DisplayName("Should map a study-plan missing start constraint to effectiveFrom")
  void mapsStudyPlanMissingStartToEffectiveFrom() {
    final var translated =
        AcademicIntegrityViolationTranslator.translate(
            violation("study_plans_end_requires_start_check"));

    assertThat(translated).isInstanceOf(AcademicValidationException.class);
    assertThat(((AcademicValidationException) translated).fieldErrors())
        .containsEntry("effectiveFrom", AcademicMessages.STUDY_PLAN_START_DATE_REQUIRED);
  }

  private static DataIntegrityViolationException violation(final String constraintName) {
    final var cause =
        new ConstraintViolationException(
            "constraint violation", new SQLException(), constraintName);
    return new DataIntegrityViolationException("integrity violation", cause);
  }
}
