package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
    requiredProperties = {
      "id",
      "name",
      "year",
      "trainingPathId",
      "trainingPathName",
      "versionNumber",
      "type",
      "format",
      "instrumental"
    })
public record AcademicSelectionOptionResponse(
    UUID id,
    String name,
    @Schema(nullable = true) Integer year,
    @Schema(nullable = true) UUID trainingPathId,
    @Schema(nullable = true) String trainingPathName,
    @Schema(nullable = true) Integer versionNumber,
    @Schema(nullable = true) String type,
    @Schema(nullable = true) String format,
    @Schema(nullable = true) Boolean instrumental) {}
