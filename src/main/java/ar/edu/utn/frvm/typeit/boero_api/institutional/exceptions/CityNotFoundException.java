package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionMessages.CITY_NOT_FOUND;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class CityNotFoundException extends ApplicationException {
  public CityNotFoundException() {
    super(ErrorCategory.INVALID_INPUT, CITY_NOT_FOUND);
  }
}
