package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.RATE_LIMIT_EXCEEDED;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class RateLimitExceededException extends ApplicationException {
  public RateLimitExceededException() {
    super(ErrorCategory.RATE_LIMITED, RATE_LIMIT_EXCEEDED);
  }
}
