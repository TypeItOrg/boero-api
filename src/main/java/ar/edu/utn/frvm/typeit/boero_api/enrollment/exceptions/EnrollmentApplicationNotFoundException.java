package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class EnrollmentApplicationNotFoundException extends ApplicationException {

  public EnrollmentApplicationNotFoundException() {
    super(ErrorCategory.NOT_FOUND, EnrollmentMessages.APPLICATION_NOT_FOUND);
  }
}
