package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;
import java.util.Map;

public class EnrollmentValidationException extends ApplicationException {
  public EnrollmentValidationException(final String message) {
    super(ErrorCategory.INVALID_INPUT, message);
  }

  public EnrollmentValidationException(
      final String message, final Map<String, String> fieldErrors) {
    super(ErrorCategory.INVALID_INPUT, message, fieldErrors);
  }
}
