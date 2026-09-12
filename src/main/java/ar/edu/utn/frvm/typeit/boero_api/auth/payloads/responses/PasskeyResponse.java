package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PasskeyCredential;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
public record PasskeyResponse(
    UUID id,
    String label,
    @Schema(nullable = true) Instant createdAt,
    @Schema(nullable = true) Instant lastUsedAt) {

  public static PasskeyResponse from(final PasskeyCredential credential) {
    return new PasskeyResponse(
        credential.getId(),
        credential.getLabel(),
        credential.getCreatedAt(),
        credential.getLastUsedAt());
  }
}
