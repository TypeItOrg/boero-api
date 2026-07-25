package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class DuplicateRoleNameException extends ApplicationException {

  public DuplicateRoleNameException() {
    super(ErrorCategory.CONFLICT, "Ya existe un rol con ese nombre en la institución.");
  }
}
