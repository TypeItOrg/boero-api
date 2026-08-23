package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.ROLE_WITH_ASSIGNMENTS;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class RoleWithAssignmentsException extends ApplicationException {

  public RoleWithAssignmentsException() {
    super(ErrorCategory.CONFLICT, ROLE_WITH_ASSIGNMENTS);
  }
}
