package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import java.util.Map;

public class EnrollmentApplicationNotEditableException extends EnrollmentConflictException {
  public EnrollmentApplicationNotEditableException() {
    super(
        EnrollmentMessages.ENROLLMENT_APPLICATION_NOT_EDITABLE,
        Map.of("status", EnrollmentMessages.ENROLLMENT_APPLICATION_NOT_EDITABLE));
  }
}
