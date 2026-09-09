package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

@Schema(
    requiredProperties = {
      "studyPlanId",
      "studyPlanName",
      "studyPlanVersion",
      "effectiveFrom",
      "effectiveTo",
      "trainingPathId",
      "trainingPathName",
      "trainingPathDescription"
    })
public record AcademicOfferSummaryResponse(
    UUID studyPlanId,
    String studyPlanName,
    int studyPlanVersion,
    LocalDate effectiveFrom,
    @Schema(nullable = true) LocalDate effectiveTo,
    UUID trainingPathId,
    String trainingPathName,
    @Schema(nullable = true) String trainingPathDescription) {

  public static AcademicOfferSummaryResponse from(final StudyPlan plan) {
    final var trainingPath = plan.getTrainingPath();
    return new AcademicOfferSummaryResponse(
        plan.getId(),
        plan.getName(),
        plan.getVersionNumber(),
        plan.getEffectiveFrom(),
        plan.getEffectiveTo(),
        trainingPath.getId(),
        trainingPath.getName(),
        trainingPath.getDescription());
  }
}
