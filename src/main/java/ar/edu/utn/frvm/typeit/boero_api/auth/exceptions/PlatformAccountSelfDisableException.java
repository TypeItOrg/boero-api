package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.PLATFORM_ACCOUNT_SELF_DISABLE;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PlatformAccountSelfDisableException extends ResponseStatusException {
  public PlatformAccountSelfDisableException() {
    super(HttpStatus.CONFLICT, PLATFORM_ACCOUNT_SELF_DISABLE);
  }
}
