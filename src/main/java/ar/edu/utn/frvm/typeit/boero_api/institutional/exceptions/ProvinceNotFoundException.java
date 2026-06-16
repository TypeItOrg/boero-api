package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ProvinceNotFoundException extends ResponseStatusException {
  public ProvinceNotFoundException() {
    super(HttpStatus.NOT_FOUND, "La provincia especificada no existe.");
  }
}
