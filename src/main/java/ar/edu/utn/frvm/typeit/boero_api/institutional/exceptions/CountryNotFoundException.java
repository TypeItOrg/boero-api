package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class CountryNotFoundException extends ApplicationException {
  public CountryNotFoundException() {
    super(ErrorCategory.NOT_FOUND, "El país especificado no existe.");
  }
}
