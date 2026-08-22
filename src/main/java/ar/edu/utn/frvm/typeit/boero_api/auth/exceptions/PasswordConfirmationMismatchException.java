package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class PasswordConfirmationMismatchException extends ApplicationException {
  public PasswordConfirmationMismatchException() {
    super(ErrorCategory.INVALID_INPUT, AuthMessages.PASSWORD_CONFIRMATION_MISMATCH);
  }
}
