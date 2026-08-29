package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;
import java.util.Map;

public class EnrollmentConflictException extends ApplicationException {
  public EnrollmentConflictException(final String message) {
    super(ErrorCategory.CONFLICT, message);
  }

  public EnrollmentConflictException(final String message, final Map<String, String> fieldErrors) {
    super(ErrorCategory.CONFLICT, message, fieldErrors);
  }
}
