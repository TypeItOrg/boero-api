package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.INVALID_CREDENTIALS;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidCredentialsException extends ResponseStatusException {
  public InvalidCredentialsException() {
    super(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
  }
}
