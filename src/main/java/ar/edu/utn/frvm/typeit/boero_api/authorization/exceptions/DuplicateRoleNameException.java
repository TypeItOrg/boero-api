package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.DUPLICATE_ROLE_NAME;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class DuplicateRoleNameException extends ApplicationException {

  public DuplicateRoleNameException() {
    super(ErrorCategory.CONFLICT, DUPLICATE_ROLE_NAME);
  }
}
