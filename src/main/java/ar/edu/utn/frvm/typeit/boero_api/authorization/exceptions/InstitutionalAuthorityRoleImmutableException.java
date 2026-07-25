package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class InstitutionalAuthorityRoleImmutableException extends ApplicationException {

  public InstitutionalAuthorityRoleImmutableException() {
    super(ErrorCategory.INVALID_INPUT, "El rol de autoridad institucional no se puede modificar.");
  }
}
