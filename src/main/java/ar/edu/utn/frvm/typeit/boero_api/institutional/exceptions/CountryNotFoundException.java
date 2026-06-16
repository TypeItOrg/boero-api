package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CountryNotFoundException extends ResponseStatusException {
  public CountryNotFoundException() {
    super(HttpStatus.NOT_FOUND, "El país especificado no existe.");
  }
}
