package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.enums.ApprovalMode;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.RequirementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateStudyPlanSpaceRequest(
    @NotNull UUID academicSpaceId,
    UUID academicLevelId,
    @NotNull RequirementType requirementType,
    @Min(value = 1, message = "El orden debe ser positivo.") int displayOrder,
    @NotNull ApprovalMode approvalMode) {}
