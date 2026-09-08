package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class InvalidEnrollmentPeriodDatesException extends ApplicationException {
  public InvalidEnrollmentPeriodDatesException() {
    super(
        ErrorCategory.INVALID_INPUT,
        "La fecha de inicio del período de inscripción debe ser anterior o igual a la fecha de fin.");
  }
}
