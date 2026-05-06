package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TokenRefreshException extends RuntimeException {
  public TokenRefreshException(String message) {
    super(message);
  }

  public static TokenRefreshException invalid() {
    return new TokenRefreshException(AuthMessages.REFRESH_TOKEN_INVALID);
  }

  public static TokenRefreshException reuse() {
    return new TokenRefreshException(AuthMessages.REFRESH_TOKEN_REUSE);
  }
}
