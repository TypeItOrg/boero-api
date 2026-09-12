package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.PASSKEY_ALREADY_REGISTERED;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class DuplicatePasskeyCredentialException extends ApplicationException {
  public DuplicatePasskeyCredentialException() {
    super(ErrorCategory.CONFLICT, PASSKEY_ALREADY_REGISTERED);
  }
}
