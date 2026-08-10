package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.Instrument;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(requiredProperties = {"id", "institutionId", "name", "description", "active"})
public record InstrumentResponse(
    UUID id,
    UUID institutionId,
    String name,
    @Schema(nullable = true) String description,
    boolean active) {

  public static InstrumentResponse from(final Instrument instrument) {
    return new InstrumentResponse(
        instrument.getId(),
        instrument.getInstitution().getId(),
        instrument.getName(),
        instrument.getDescription(),
        instrument.isActive());
  }
}
