package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class EnrollmentPeriodClosedException extends ApplicationException {

  public EnrollmentPeriodClosedException() {
    super(ErrorCategory.CONFLICT, "No existe un período de inscripción habilitado actualmente.");
  }
}
