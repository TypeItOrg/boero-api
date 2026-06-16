package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionMessages.SLUG_ALREADY_EXISTS;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class SlugAlreadyExistsException extends ResponseStatusException {
  public SlugAlreadyExistsException() {
    super(HttpStatus.CONFLICT, SLUG_ALREADY_EXISTS);
  }
}
