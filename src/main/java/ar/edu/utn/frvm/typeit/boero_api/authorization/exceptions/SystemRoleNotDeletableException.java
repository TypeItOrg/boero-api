package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.SYSTEM_ROLE_NOT_DELETABLE;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class SystemRoleNotDeletableException extends ApplicationException {

  public SystemRoleNotDeletableException() {
    super(ErrorCategory.INVALID_INPUT, SYSTEM_ROLE_NOT_DELETABLE);
  }
}
