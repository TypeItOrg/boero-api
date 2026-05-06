package ar.edu.utn.frvm.typeit.boero_api.auth.filters;

import java.security.Principal;
import java.util.UUID;

public record JwtAuthenticatedUser(
    UUID userId,
    String documentNumber,
    UUID institutionId,
    UUID sessionId,
    String jti)
    implements Principal {

  @Override
  public String getName() {
    return documentNumber;
  }
}
