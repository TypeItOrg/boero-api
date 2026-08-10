package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class AcademicNotFoundException extends ApplicationException {
  public AcademicNotFoundException(final String message) {
    super(ErrorCategory.NOT_FOUND, message);
  }
}
