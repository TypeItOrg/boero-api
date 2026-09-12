package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.LOGIN_STATE_INCONSISTENT;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class LoginStateInconsistentException extends ApplicationException {
  public LoginStateInconsistentException() {
    super(ErrorCategory.AUTHENTICATION, LOGIN_STATE_INCONSISTENT);
  }
}
