package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.WEBAUTHN_CEREMONY_INVALID;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class WebAuthnCeremonyInvalidException extends ApplicationException {
  public WebAuthnCeremonyInvalidException() {
    super(ErrorCategory.AUTHENTICATION, WEBAUTHN_CEREMONY_INVALID);
  }
}
