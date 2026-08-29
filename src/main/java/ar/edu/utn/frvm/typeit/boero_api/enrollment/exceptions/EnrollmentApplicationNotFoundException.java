package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

public class EnrollmentApplicationNotFoundException extends EnrollmentNotFoundException {
  public EnrollmentApplicationNotFoundException() {
    super(EnrollmentMessages.ENROLLMENT_APPLICATION_NOT_FOUND);
  }
}
