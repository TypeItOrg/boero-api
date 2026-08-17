package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Instrument;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(requiredProperties = {"id", "institutionId", "name", "description", "active", "deletedAt"})
public record InstrumentResponse(
    UUID id,
    UUID institutionId,
    String name,
    @Schema(nullable = true) String description,
    boolean active,
    @Schema(nullable = true) LocalDateTime deletedAt) {

  public static InstrumentResponse from(final Instrument instrument) {
    return new InstrumentResponse(
        instrument.getId(),
        instrument.getInstitution().getId(),
        instrument.getName(),
        instrument.getDescription(),
        instrument.isActive(),
        instrument.getDeletedAt());
  }
}
