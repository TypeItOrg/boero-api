package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.DocumentDefinition;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Schema(
    requiredProperties = {
      "id",
      "institutionId",
      "name",
      "instructions",
      "allowedFormats",
      "active",
      "revision",
      "affectedTrainingPaths",
      "affectedDrafts"
    })
public record DocumentDefinitionResponse(
    UUID id,
    UUID institutionId,
    String name,
    String instructions,
    List<String> allowedFormats,
    boolean active,
    long revision,
    @Schema(nullable = true) @Nullable Long affectedTrainingPaths,
    @Schema(nullable = true) @Nullable Long affectedDrafts) {
  public DocumentDefinitionResponse withImpact(final long paths, final long drafts) {
    return new DocumentDefinitionResponse(
        id, institutionId, name, instructions, allowedFormats, active, revision, paths, drafts);
  }

  public static DocumentDefinitionResponse from(final DocumentDefinition value) {
    return new DocumentDefinitionResponse(
        value.getId(),
        value.getInstitution().getId(),
        value.getName(),
        value.getInstructions(),
        value.getAllowedFormats(),
        value.isActive(),
        value.getRevision(),
        null,
        null);
  }
}
