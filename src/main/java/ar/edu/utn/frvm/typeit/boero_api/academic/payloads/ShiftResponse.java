package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Shift;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(requiredProperties = {"id", "institutionId", "name", "description", "active", "deletedAt"})
public record ShiftResponse(
    UUID id,
    UUID institutionId,
    String name,
    @Schema(nullable = true) String description,
    boolean active,
    @Schema(nullable = true) LocalDateTime deletedAt) {

  public static ShiftResponse from(final Shift shift) {
    return new ShiftResponse(
        shift.getId(),
        shift.getInstitution().getId(),
        shift.getName(),
        shift.getDescription(),
        shift.isActive(),
        shift.getDeletedAt());
  }
}
