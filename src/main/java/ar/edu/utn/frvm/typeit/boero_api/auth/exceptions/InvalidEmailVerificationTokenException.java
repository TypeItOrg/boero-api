package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class InvalidEmailVerificationTokenException extends ApplicationException {
  public InvalidEmailVerificationTokenException() {
    super(ErrorCategory.INVALID_INPUT, AuthMessages.EMAIL_VERIFICATION_TOKEN_INVALID);
  }
}
