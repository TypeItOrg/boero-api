package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.WEBAUTHN_VERIFICATION_FAILED;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class WebAuthnVerificationFailedException extends ApplicationException {
  public WebAuthnVerificationFailedException() {
    super(ErrorCategory.AUTHENTICATION, WEBAUTHN_VERIFICATION_FAILED);
  }
}
