package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
    requiredProperties = {
      "id",
      "institutionId",
      "institutionName",
      "trainingPathId",
      "trainingPathName",
      "name",
      "effectiveFrom",
      "effectiveTo",
      "status",
      "deletedAt"
    })
public record StudyPlanResponse(
    UUID id,
    UUID institutionId,
    String institutionName,
    UUID trainingPathId,
    String trainingPathName,
    String name,
    @Schema(nullable = true) LocalDate effectiveFrom,
    @Schema(nullable = true) LocalDate effectiveTo,
    StudyPlanStatus status,
    @Schema(nullable = true) LocalDateTime deletedAt) {

  public static StudyPlanResponse from(final StudyPlan plan) {
    return new StudyPlanResponse(
        plan.getId(),
        plan.getInstitution().getId(),
        plan.getInstitution().getName(),
        plan.getTrainingPath().getId(),
        plan.getTrainingPath().getName(),
        plan.getName(),
        plan.getEffectiveFrom(),
        plan.getEffectiveTo(),
        plan.getStatus(),
        plan.getDeletedAt());
  }
}
