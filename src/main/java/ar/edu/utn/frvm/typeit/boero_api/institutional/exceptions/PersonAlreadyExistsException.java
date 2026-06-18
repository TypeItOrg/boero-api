package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PersonAlreadyExistsException extends ResponseStatusException {
  public PersonAlreadyExistsException() {
    super(HttpStatus.CONFLICT, InstitutionMessages.PERSON_ALREADY_EXISTS);
  }
}
