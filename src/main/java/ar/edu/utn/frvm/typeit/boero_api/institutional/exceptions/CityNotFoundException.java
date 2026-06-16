package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionMessages.CITY_NOT_FOUND;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CityNotFoundException extends ResponseStatusException {
  public CityNotFoundException() {
    super(HttpStatus.BAD_REQUEST, CITY_NOT_FOUND);
  }
}
