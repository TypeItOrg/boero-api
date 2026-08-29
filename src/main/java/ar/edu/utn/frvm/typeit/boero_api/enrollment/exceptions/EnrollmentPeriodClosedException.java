package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

public class EnrollmentPeriodClosedException extends RuntimeException {

  public EnrollmentPeriodClosedException() {
    super("No existe un período de inscripción habilitado actualmente.");
  }
}
