package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.ROLE_MANAGEMENT_SELF_LOCKOUT;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class RoleManagementSelfLockoutException extends ApplicationException {

  public RoleManagementSelfLockoutException() {
    super(ErrorCategory.CONFLICT, ROLE_MANAGEMENT_SELF_LOCKOUT);
  }
}
