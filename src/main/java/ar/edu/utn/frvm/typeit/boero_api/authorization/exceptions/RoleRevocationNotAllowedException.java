package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.ROLE_REVOCATION_NOT_ALLOWED;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class RoleRevocationNotAllowedException extends ApplicationException {

  public RoleRevocationNotAllowedException() {
    super(ErrorCategory.AUTHORIZATION, ROLE_REVOCATION_NOT_ALLOWED);
  }
}
