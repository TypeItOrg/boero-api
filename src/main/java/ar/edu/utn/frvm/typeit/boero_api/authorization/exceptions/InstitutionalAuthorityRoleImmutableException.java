package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.INSTITUTIONAL_AUTHORITY_ROLE_IMMUTABLE;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class InstitutionalAuthorityRoleImmutableException extends ApplicationException {

  public InstitutionalAuthorityRoleImmutableException() {
    super(ErrorCategory.INVALID_INPUT, INSTITUTIONAL_AUTHORITY_ROLE_IMMUTABLE);
  }
}
