package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class InvalidEnrollmentPeriodDatesException extends ApplicationException {
  public InvalidEnrollmentPeriodDatesException() {
    super(ErrorCategory.INVALID_INPUT, EnrollmentMessages.PERIOD_DATES_INVALID);
  }
}
