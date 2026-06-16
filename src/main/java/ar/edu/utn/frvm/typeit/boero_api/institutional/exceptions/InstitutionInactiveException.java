package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionMessages.INSTITUTION_INACTIVE;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InstitutionInactiveException extends ResponseStatusException {
  public InstitutionInactiveException() {
    super(HttpStatus.NOT_FOUND, INSTITUTION_INACTIVE);
  }
}
