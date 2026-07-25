package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class RoleAssignmentNotAllowedException extends ApplicationException {

  public RoleAssignmentNotAllowedException() {
    super(ErrorCategory.AUTHORIZATION, "No podés asignar roles.");
  }
}
