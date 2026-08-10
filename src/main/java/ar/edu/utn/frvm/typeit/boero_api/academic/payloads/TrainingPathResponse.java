package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPath;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(requiredProperties = {"id", "institutionId", "name", "description", "active"})
public record TrainingPathResponse(
    UUID id,
    UUID institutionId,
    String name,
    @Schema(nullable = true) String description,
    boolean active) {

  public static TrainingPathResponse from(final TrainingPath path) {
    return new TrainingPathResponse(
        path.getId(),
        path.getInstitution().getId(),
        path.getName(),
        path.getDescription(),
        path.isActive());
  }
}
