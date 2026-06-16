package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AuthResponse(UserPayload user, TokenResponse tokens) {

  public static AuthResponse of(User user, UUID personId, String accessToken, String refreshToken) {
    return AuthResponse.builder()
        .user(UserPayload.from(user, personId))
        .tokens(TokenResponse.builder().accessToken(accessToken).refreshToken(refreshToken).build())
        .build();
  }
}
