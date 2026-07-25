package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.REFRESH_TOKEN_REUSE;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class RefreshTokenReuseException extends ApplicationException {

  public RefreshTokenReuseException() {
    super(ErrorCategory.AUTHENTICATION, REFRESH_TOKEN_REUSE);
  }
}
