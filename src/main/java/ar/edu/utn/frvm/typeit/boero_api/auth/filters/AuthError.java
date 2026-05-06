package ar.edu.utn.frvm.typeit.boero_api.auth.filters;

import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages;

public enum AuthError {
  MISSING_TOKEN(AuthMessages.TOKEN_MISSING),
  EXPIRED_TOKEN(AuthMessages.TOKEN_EXPIRED),
  INVALID_TOKEN(AuthMessages.TOKEN_INVALID),
  BLACKLISTED_TOKEN(AuthMessages.TOKEN_BLACKLISTED),
  INACTIVE_SESSION(AuthMessages.TOKEN_SESSION_INACTIVE);

  private final String message;

  AuthError(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
