package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class ActiveEnrollmentApplicationExistsException extends ApplicationException {

  public ActiveEnrollmentApplicationExistsException() {
    super(ErrorCategory.CONFLICT, EnrollmentMessages.ACTIVE_PATH_APPLICATION_EXISTS);
  }
}
