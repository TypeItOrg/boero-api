package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.REFRESH_TOKEN_INVALID;
import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.REFRESH_TOKEN_REUSE;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class TokenRefreshException extends ResponseStatusException {
  public TokenRefreshException(String message) {
    super(HttpStatus.UNAUTHORIZED, message);
  }

  public static TokenRefreshException invalid() {
    return new TokenRefreshException(REFRESH_TOKEN_INVALID);
  }

  public static TokenRefreshException reuse() {
    return new TokenRefreshException(REFRESH_TOKEN_REUSE);
  }
}
