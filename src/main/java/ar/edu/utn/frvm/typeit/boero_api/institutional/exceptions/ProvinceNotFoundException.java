package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class ProvinceNotFoundException extends ApplicationException {
  public ProvinceNotFoundException() {
    super(ErrorCategory.NOT_FOUND, "La provincia especificada no existe.");
  }
}
