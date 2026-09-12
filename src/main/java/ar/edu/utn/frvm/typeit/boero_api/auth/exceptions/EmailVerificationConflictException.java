package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class EmailVerificationConflictException extends ApplicationException {
  public EmailVerificationConflictException() {
    super(ErrorCategory.CONFLICT, AuthMessages.EMAIL_VERIFICATION_CONFLICT);
  }
}
