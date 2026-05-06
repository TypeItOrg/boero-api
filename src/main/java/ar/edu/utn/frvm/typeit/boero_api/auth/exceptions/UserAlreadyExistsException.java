package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends RuntimeException {
  public UserAlreadyExistsException() {
    super(AuthMessages.USER_ALREADY_EXISTS);
  }
}
