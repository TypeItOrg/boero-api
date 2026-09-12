package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class EmailVerificationCooldownException extends ApplicationException {
  public EmailVerificationCooldownException() {
    super(ErrorCategory.CONFLICT, AuthMessages.EMAIL_VERIFICATION_COOLDOWN);
  }
}
