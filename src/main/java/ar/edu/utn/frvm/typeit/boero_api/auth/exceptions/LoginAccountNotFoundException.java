package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.LOGIN_ACCOUNT_NOT_FOUND;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class LoginAccountNotFoundException extends ApplicationException {
  public LoginAccountNotFoundException() {
    super(ErrorCategory.NOT_FOUND, LOGIN_ACCOUNT_NOT_FOUND);
  }
}
