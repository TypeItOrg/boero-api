package ar.edu.utn.frvm.typeit.boero_api.academic.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;
import java.io.Serial;

public class AcademicNotFoundException extends ApplicationException {

  @Serial private static final long serialVersionUID = 1L;

  public AcademicNotFoundException(final String message) {
    super(ErrorCategory.NOT_FOUND, message);
  }
}
