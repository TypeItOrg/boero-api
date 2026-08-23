package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.PERMISSION_DELEGATION_NOT_ALLOWED;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class PermissionDelegationNotAllowedException extends ApplicationException {

  public PermissionDelegationNotAllowedException() {
    super(ErrorCategory.AUTHORIZATION, PERMISSION_DELEGATION_NOT_ALLOWED);
  }
}
