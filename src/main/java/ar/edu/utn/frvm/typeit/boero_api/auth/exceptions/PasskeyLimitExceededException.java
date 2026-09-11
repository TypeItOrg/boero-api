package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.PASSKEY_LIMIT_EXCEEDED;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class PasskeyLimitExceededException extends ApplicationException {
  public PasskeyLimitExceededException() {
    super(ErrorCategory.CONFLICT, PASSKEY_LIMIT_EXCEEDED);
  }
}
