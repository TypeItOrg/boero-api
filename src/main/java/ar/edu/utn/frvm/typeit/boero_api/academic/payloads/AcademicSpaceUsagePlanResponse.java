package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.StudyPlanStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(
    requiredProperties = {
      "studyPlanId",
      "name",
      "trainingPathName",
      "effectiveFrom",
      "effectiveTo",
      "status",
      "placements"
    })
public record AcademicSpaceUsagePlanResponse(
    UUID studyPlanId,
    String name,
    String trainingPathName,
    @Schema(nullable = true) LocalDate effectiveFrom,
    @Schema(nullable = true) LocalDate effectiveTo,
    StudyPlanStatus status,
    List<AcademicSpaceUsagePlacementResponse> placements) {

  public static AcademicSpaceUsagePlanResponse from(
      final StudyPlan plan, final List<AcademicSpaceUsagePlacementResponse> placements) {
    return new AcademicSpaceUsagePlanResponse(
        plan.getId(),
        plan.getName(),
        plan.getTrainingPath().getName(),
        plan.getEffectiveFrom(),
        plan.getEffectiveTo(),
        plan.getStatus(),
        placements);
  }
}
