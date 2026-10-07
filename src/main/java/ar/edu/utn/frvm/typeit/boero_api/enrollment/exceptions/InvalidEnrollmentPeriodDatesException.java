package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;
import java.io.Serial;

public class InvalidEnrollmentPeriodDatesException extends ApplicationException {

  @Serial private static final long serialVersionUID = 1L;

  public InvalidEnrollmentPeriodDatesException() {
    super(ErrorCategory.INVALID_INPUT, EnrollmentMessages.PERIOD_DATES_INVALID);
  }
}
