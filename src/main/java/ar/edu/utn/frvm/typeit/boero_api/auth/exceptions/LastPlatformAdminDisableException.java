package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.LAST_PLATFORM_ADMIN_DISABLE;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class LastPlatformAdminDisableException extends ApplicationException {
  public LastPlatformAdminDisableException() {
    super(ErrorCategory.CONFLICT, LAST_PLATFORM_ADMIN_DISABLE);
  }
}
