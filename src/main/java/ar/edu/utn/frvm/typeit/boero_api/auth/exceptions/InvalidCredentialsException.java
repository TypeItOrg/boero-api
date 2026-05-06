package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidCredentialsException extends RuntimeException {
  public InvalidCredentialsException() {
    super(AuthMessages.INVALID_CREDENTIALS);
  }
}
