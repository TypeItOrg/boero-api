package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPathDocumentRequirement;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.DocumentRequirementLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(
    requiredProperties = {
      "id",
      "name",
      "instructions",
      "level",
      "allowedFormats",
      "displayOrder",
      "active"
    })
public record DocumentRequirementResponse(
    UUID id,
    String name,
    String instructions,
    DocumentRequirementLevel level,
    List<String> allowedFormats,
    int displayOrder,
    boolean active) {
  public static DocumentRequirementResponse from(TrainingPathDocumentRequirement value) {
    return new DocumentRequirementResponse(
        value.getId(),
        value.getName(),
        value.getInstructions(),
        value.getLevel(),
        value.getAllowedFormats(),
        value.getDisplayOrder(),
        value.isActive());
  }
}
