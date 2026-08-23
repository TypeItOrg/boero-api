package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicSpace;
import ar.edu.utn.frvm.typeit.boero_api.academic.enums.AcademicSpaceType;
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
      "type",
      "active",
      "deletedAt"
    })
public record AcademicSpaceResponse(
    UUID id,
    UUID institutionId,
    String institutionName,
    String name,
    @Schema(nullable = true) String description,
    AcademicSpaceType type,
    boolean active,
    @Schema(nullable = true) LocalDateTime deletedAt) {

  public static AcademicSpaceResponse from(final AcademicSpace space) {
    return new AcademicSpaceResponse(
        space.getId(),
        space.getInstitution().getId(),
        space.getInstitution().getName(),
        space.getName(),
        space.getDescription(),
        space.getType(),
        space.isActive(),
        space.getDeletedAt());
  }
}
