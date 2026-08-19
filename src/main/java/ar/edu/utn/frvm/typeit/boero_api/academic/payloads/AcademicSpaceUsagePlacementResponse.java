package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlanSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.ApprovalMode;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.RequirementType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
    requiredProperties = {
      "studyPlanSpaceId",
      "academicLevelId",
      "academicLevelName",
      "requirementType",
      "approvalMode",
      "displayOrder"
    })
public record AcademicSpaceUsagePlacementResponse(
    UUID studyPlanSpaceId,
    @Schema(nullable = true) UUID academicLevelId,
    @Schema(nullable = true) String academicLevelName,
    RequirementType requirementType,
    ApprovalMode approvalMode,
    int displayOrder) {

  public static AcademicSpaceUsagePlacementResponse from(final StudyPlanSpace space) {
    final var level = space.getAcademicLevel();
    return new AcademicSpaceUsagePlacementResponse(
        space.getId(),
        level == null ? null : level.getId(),
        level == null ? null : level.getName(),
        space.getRequirementType(),
        space.getApprovalMode(),
        space.getDisplayOrder());
  }
}
