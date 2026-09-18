package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StartEnrollmentApplicationRequest(
    @Schema(nullable = true) UUID trainingPathId,
    @Schema(nullable = true) UUID studyPlanId,
    @Schema(nullable = true) UUID academicYearId) {
  public StartEnrollmentApplicationRequest() {
    this(null, null, null);
  }

  public StartEnrollmentApplicationRequest(final UUID studyPlanId, final UUID academicYearId) {
    this(null, studyPlanId, academicYearId);
  }

  public UUID getTrainingPathId() {
    return trainingPathId;
  }

  public UUID getStudyPlanId() {
    return studyPlanId;
  }

  public UUID getAcademicYearId() {
    return academicYearId;
  }
}
