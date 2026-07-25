package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.ROLE_NOT_ASSIGNABLE;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class RoleNotAssignableException extends ApplicationException {
  public RoleNotAssignableException() {
    super(ErrorCategory.INVALID_INPUT, ROLE_NOT_ASSIGNABLE);
  }
}
