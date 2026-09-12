package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.RECENT_AUTH_REQUIRED;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class RecentAuthRequiredException extends ApplicationException {

  public static final String CODE = "RECENT_AUTHENTICATION_REQUIRED";

  public RecentAuthRequiredException() {
    super(ErrorCategory.AUTHORIZATION, RECENT_AUTH_REQUIRED, CODE);
  }
}
