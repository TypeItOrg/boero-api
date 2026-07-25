package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class RoleNotFoundException extends ApplicationException {

  public RoleNotFoundException() {
    super(ErrorCategory.NOT_FOUND, "Rol no encontrado.");
  }
}
