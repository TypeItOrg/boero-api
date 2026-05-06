package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InstitutionNotFoundException extends RuntimeException {
  public InstitutionNotFoundException() {
    super(AuthMessages.INSTITUTION_NOT_FOUND);
  }
}
