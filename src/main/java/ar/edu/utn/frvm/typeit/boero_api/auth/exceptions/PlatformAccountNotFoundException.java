package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.PLATFORM_ACCOUNT_NOT_FOUND;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PlatformAccountNotFoundException extends ResponseStatusException {
  public PlatformAccountNotFoundException() {
    super(HttpStatus.NOT_FOUND, PLATFORM_ACCOUNT_NOT_FOUND);
  }
}
