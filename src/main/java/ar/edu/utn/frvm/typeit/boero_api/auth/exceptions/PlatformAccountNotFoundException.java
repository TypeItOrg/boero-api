package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.PLATFORM_ACCOUNT_NOT_FOUND;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class PlatformAccountNotFoundException extends ApplicationException {
  public PlatformAccountNotFoundException() {
    super(ErrorCategory.NOT_FOUND, PLATFORM_ACCOUNT_NOT_FOUND);
  }
}
