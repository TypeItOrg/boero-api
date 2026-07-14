package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.LAST_PLATFORM_ADMIN_DISABLE;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class LastPlatformAdminDisableException extends ResponseStatusException {
  public LastPlatformAdminDisableException() {
    super(HttpStatus.CONFLICT, LAST_PLATFORM_ADMIN_DISABLE);
  }
}
