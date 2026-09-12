package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class EnrollmentPeriodNotFoundException extends ApplicationException {
  public EnrollmentPeriodNotFoundException() {
    super(ErrorCategory.NOT_FOUND, "Período de inscripción no encontrado.");
  }
}
