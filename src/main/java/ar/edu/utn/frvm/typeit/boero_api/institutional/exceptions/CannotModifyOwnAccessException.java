package ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionMessages.CANNOT_MODIFY_OWN_ACCESS;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class CannotModifyOwnAccessException extends ApplicationException {

  public CannotModifyOwnAccessException() {
    super(ErrorCategory.INVALID_INPUT, CANNOT_MODIFY_OWN_ACCESS);
  }
}
