package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class EnrollmentNotFoundException extends ApplicationException {
  public EnrollmentNotFoundException(final String message) {
    super(ErrorCategory.NOT_FOUND, message);
  }
}
