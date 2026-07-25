package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.REFRESH_TOKEN_INVALID;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class InvalidRefreshTokenException extends ApplicationException {

  public InvalidRefreshTokenException() {
    super(ErrorCategory.AUTHENTICATION, REFRESH_TOKEN_INVALID);
  }
}
