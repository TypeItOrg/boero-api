package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.USER_ALREADY_EXISTS;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UserAlreadyExistsException extends ResponseStatusException {
  public UserAlreadyExistsException() {
    super(HttpStatus.CONFLICT, USER_ALREADY_EXISTS);
  }
}
