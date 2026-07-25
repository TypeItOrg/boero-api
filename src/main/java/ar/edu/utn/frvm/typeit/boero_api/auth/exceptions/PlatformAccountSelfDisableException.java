package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.PLATFORM_ACCOUNT_SELF_DISABLE;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class PlatformAccountSelfDisableException extends ApplicationException {
  public PlatformAccountSelfDisableException() {
    super(ErrorCategory.CONFLICT, PLATFORM_ACCOUNT_SELF_DISABLE);
  }
}
