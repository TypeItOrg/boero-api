package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.RATE_LIMIT_UNAVAILABLE;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class RateLimitUnavailableException extends ApplicationException {
  public RateLimitUnavailableException() {
    super(ErrorCategory.SERVICE_UNAVAILABLE, RATE_LIMIT_UNAVAILABLE);
  }
}
