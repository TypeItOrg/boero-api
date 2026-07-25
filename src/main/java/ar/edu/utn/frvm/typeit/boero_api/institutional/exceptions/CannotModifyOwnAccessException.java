package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class CannotModifyOwnAccessException extends ApplicationException {

  public CannotModifyOwnAccessException() {
    super(ErrorCategory.INVALID_INPUT, "No podés modificar el estado de tu propio acceso.");
  }
}
