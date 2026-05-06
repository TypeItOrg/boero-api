package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UserDisabledException extends RuntimeException {
  public UserDisabledException() {
    super(AuthMessages.USER_DISABLED);
  }
}
