package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StartEnrollmentApplicationRequest(
    @NotNull(message = EnrollmentMessages.STUDY_PLAN_REQUIRED) @Schema(nullable = true)
        UUID studyPlanId,
    @NotNull(message = EnrollmentMessages.ACADEMIC_YEAR_REQUIRED) @Schema(nullable = true)
        UUID academicYearId) {
  public StartEnrollmentApplicationRequest() {
    this(null, null);
  }

  public UUID getStudyPlanId() {
    return studyPlanId;
  }

  public UUID getAcademicYearId() {
    return academicYearId;
  }
}
