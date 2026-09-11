package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.LOGIN_ATTEMPT_INVALID;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class InvalidLoginAttemptException extends ApplicationException {
  public InvalidLoginAttemptException() {
    super(ErrorCategory.AUTHENTICATION, LOGIN_ATTEMPT_INVALID);
  }
}
