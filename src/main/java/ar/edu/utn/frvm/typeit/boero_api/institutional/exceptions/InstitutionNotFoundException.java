package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionMessages.INSTITUTION_NOT_FOUND;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InstitutionNotFoundException extends ResponseStatusException {
  public InstitutionNotFoundException() {
    super(HttpStatus.NOT_FOUND, INSTITUTION_NOT_FOUND);
  }
}
