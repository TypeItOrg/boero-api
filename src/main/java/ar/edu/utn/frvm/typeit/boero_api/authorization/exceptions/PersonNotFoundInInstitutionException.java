package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.PERSON_NOT_FOUND_IN_INSTITUTION;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PersonNotFoundInInstitutionException extends ResponseStatusException {
  public PersonNotFoundInInstitutionException() {
    super(HttpStatus.NOT_FOUND, PERSON_NOT_FOUND_IN_INSTITUTION);
  }
}
