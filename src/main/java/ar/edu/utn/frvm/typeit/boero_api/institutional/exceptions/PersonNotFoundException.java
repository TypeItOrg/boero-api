package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class PersonNotFoundException extends ApplicationException {
  public PersonNotFoundException() {
    super(ErrorCategory.NOT_FOUND, "La persona especificada no existe.");
  }
}
