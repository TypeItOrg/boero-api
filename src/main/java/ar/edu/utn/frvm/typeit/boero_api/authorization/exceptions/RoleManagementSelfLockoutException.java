package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class RoleManagementSelfLockoutException extends ApplicationException {

  public RoleManagementSelfLockoutException() {
    super(
        ErrorCategory.CONFLICT,
        "No podés quitarte los permisos necesarios para administrar roles institucionales.");
  }
}
