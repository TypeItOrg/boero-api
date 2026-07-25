package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class PermissionDelegationNotAllowedException extends ApplicationException {

  public PermissionDelegationNotAllowedException() {
    super(ErrorCategory.AUTHORIZATION, "No podés delegar permisos que no poseés.");
  }
}
