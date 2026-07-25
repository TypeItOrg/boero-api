package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class RoleWithAssignmentsException extends ApplicationException {

  public RoleWithAssignmentsException() {
    super(ErrorCategory.CONFLICT, "El rol no se puede eliminar mientras tenga usuarios asignados.");
  }
}
