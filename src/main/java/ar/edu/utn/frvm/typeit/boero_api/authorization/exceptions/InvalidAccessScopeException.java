package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class InvalidAccessScopeException extends ApplicationException {
  public InvalidAccessScopeException() {
    super(ErrorCategory.INVALID_INPUT, AuthorizationMessages.INVALID_ACCESS_SCOPE);
  }
}
