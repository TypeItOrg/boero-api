package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class InvalidFileException extends ApplicationException {
  public InvalidFileException(String message) {
    super(ErrorCategory.INVALID_INPUT, message);
  }
}
