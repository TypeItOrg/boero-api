package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PersonNotFoundException extends ResponseStatusException {
  public PersonNotFoundException() {
    super(HttpStatus.NOT_FOUND, "La persona especificada no existe.");
  }
}
