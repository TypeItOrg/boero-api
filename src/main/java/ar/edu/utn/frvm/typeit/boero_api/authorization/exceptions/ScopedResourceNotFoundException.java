package ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class ScopedResourceNotFoundException extends ApplicationException {
  public ScopedResourceNotFoundException() {
    super(ErrorCategory.NOT_FOUND, AuthorizationMessages.SCOPED_RESOURCE_NOT_FOUND);
  }
}
