package ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import lombok.Builder;

@Builder
public record AuthResponse(UserPayload user, TokenResponse tokens) {

  public static AuthResponse of(User user, String accessToken, String refreshToken) {
    return AuthResponse.builder()
        .user(UserPayload.from(user))
        .tokens(
            TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build())
        .build();
  }
}
