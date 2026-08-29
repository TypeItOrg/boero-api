package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import java.util.UUID;

public class EnrollmentApplicationNotFoundException extends RuntimeException {

  public EnrollmentApplicationNotFoundException(UUID applicationId) {
    super("No se encontró la solicitud de inscripción con ID " + applicationId);
  }
}
