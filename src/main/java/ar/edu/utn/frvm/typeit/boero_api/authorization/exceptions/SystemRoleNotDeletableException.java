package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class SystemRoleNotDeletableException extends ApplicationException {

  public SystemRoleNotDeletableException() {
    super(ErrorCategory.INVALID_INPUT, "Los roles del sistema no se pueden eliminar.");
  }
}
