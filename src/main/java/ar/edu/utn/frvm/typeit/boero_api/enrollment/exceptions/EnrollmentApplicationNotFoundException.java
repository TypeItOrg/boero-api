package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

public class EnrollmentApplicationNotFoundException extends EnrollmentNotFoundException {
  public EnrollmentApplicationNotFoundException() {
    super(ErrorCategory.NOT_FOUND, EnrollmentMessages.ENROLLMENT_APPLICATION_NOT_FOUND);
  }

  public EnrollmentApplicationNotFoundException(final UUID applicationId) {
    super(ErrorCategory.NOT_FOUND, EnrollmentMessages.APPLICATION_ID_NOT_FOUND + applicationId);
  }
}
