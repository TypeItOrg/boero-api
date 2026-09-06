package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class InvalidEnrollmentApplicationStateException extends ApplicationException {

  public InvalidEnrollmentApplicationStateException(final String message) {
    super(ErrorCategory.CONFLICT, message);
  }
}
