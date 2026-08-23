package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
    requiredProperties = {
      "id",
      "institutionId",
      "institutionName",
      "name",
      "description",
      "active",
      "deletedAt"
    })
public record TrainingPathResponse(
    UUID id,
    UUID institutionId,
    String institutionName,
    String name,
    @Schema(nullable = true) String description,
    boolean active,
    @Schema(nullable = true) LocalDateTime deletedAt) {

  public static TrainingPathResponse from(final TrainingPath path) {
    return new TrainingPathResponse(
        path.getId(),
        path.getInstitution().getId(),
        path.getInstitution().getName(),
        path.getName(),
        path.getDescription(),
        path.isActive(),
        path.getDeletedAt());
  }
}
