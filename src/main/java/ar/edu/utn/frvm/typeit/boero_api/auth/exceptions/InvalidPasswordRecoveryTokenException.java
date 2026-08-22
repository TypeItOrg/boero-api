package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class InvalidPasswordRecoveryTokenException extends ApplicationException {
  public InvalidPasswordRecoveryTokenException() {
    super(ErrorCategory.INVALID_INPUT, AuthMessages.PASSWORD_RECOVERY_TOKEN_INVALID);
  }
}
