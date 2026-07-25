package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.AuthorizationMessages.LAST_PERSON_ROLE_REVOCATION;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class LastPersonRoleRevocationException extends ApplicationException {
  public LastPersonRoleRevocationException() {
    super(ErrorCategory.CONFLICT, LAST_PERSON_ROLE_REVOCATION);
  }
}
