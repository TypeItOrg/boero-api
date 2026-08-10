package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlanSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.ApprovalMode;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.RequirementType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
    requiredProperties = {
      "id",
      "studyPlanId",
      "academicSpaceId",
      "academicSpaceName",
      "academicLevelId",
      "academicLevelName",
      "requirementType",
      "displayOrder",
      "approvalMode"
    })
public record StudyPlanSpaceResponse(
    UUID id,
    UUID studyPlanId,
    UUID academicSpaceId,
    String academicSpaceName,
    @Schema(nullable = true) UUID academicLevelId,
    @Schema(nullable = true) String academicLevelName,
    RequirementType requirementType,
    int displayOrder,
    ApprovalMode approvalMode) {

  public static StudyPlanSpaceResponse from(final StudyPlanSpace space) {
    final var level = space.getAcademicLevel();
    return new StudyPlanSpaceResponse(
        space.getId(),
        space.getStudyPlan().getId(),
        space.getAcademicSpace().getId(),
        space.getAcademicSpace().getName(),
        level == null ? null : level.getId(),
        level == null ? null : level.getName(),
        space.getRequirementType(),
        space.getDisplayOrder(),
        space.getApprovalMode());
  }
}
