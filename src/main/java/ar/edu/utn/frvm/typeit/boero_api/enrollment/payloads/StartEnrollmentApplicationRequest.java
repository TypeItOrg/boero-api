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
        UUID academicYearId,
    @Schema(
            nullable = true,
            description =
                "Person the application is for. Omit to apply for yourself; when set, the caller must be a guardian of that person.")
        UUID applicantPersonId) {
  public StartEnrollmentApplicationRequest() {
    this(null, null, null);
  }

  public StartEnrollmentApplicationRequest(final UUID studyPlanId, final UUID academicYearId) {
    this(studyPlanId, academicYearId, null);
  }

  public UUID getApplicantPersonId() {
    return applicantPersonId;
  }

  public UUID getStudyPlanId() {
    return studyPlanId;
  }

  public UUID getAcademicYearId() {
    return academicYearId;
  }
}
