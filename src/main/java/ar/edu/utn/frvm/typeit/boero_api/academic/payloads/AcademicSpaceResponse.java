package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(requiredProperties = {"id", "institutionId", "name", "description", "type", "active"})
public record AcademicSpaceResponse(
    UUID id,
    UUID institutionId,
    String name,
    @Schema(nullable = true) String description,
    AcademicSpaceType type,
    boolean active) {

  public static AcademicSpaceResponse from(final AcademicSpace space) {
    return new AcademicSpaceResponse(
        space.getId(),
        space.getInstitution().getId(),
        space.getName(),
        space.getDescription(),
        space.getType(),
        space.isActive());
  }
}
