package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class MissingRejectionReasonException extends ApplicationException {

  public MissingRejectionReasonException() {
    super(ErrorCategory.INVALID_INPUT, EnrollmentMessages.REJECTION_REASON_REQUIRED);
  }
}
