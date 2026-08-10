package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;
import java.util.Map;

public class AcademicConflictException extends ApplicationException {
  public static AcademicConflictException forField(final String field, final String message) {
    return new AcademicConflictException(message, Map.of(field, message));
  }

  public AcademicConflictException(final String message) {
    super(ErrorCategory.CONFLICT, message);
  }

  public AcademicConflictException(final String message, final Map<String, String> fieldErrors) {
    super(ErrorCategory.CONFLICT, message, fieldErrors);
  }
}
