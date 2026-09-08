package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;
import java.util.UUID;

public class EnrollmentApplicationNotFoundException extends ApplicationException {

  public EnrollmentApplicationNotFoundException(UUID applicationId) {
    super(
        ErrorCategory.NOT_FOUND,
        "No se encontró la solicitud de inscripción con ID " + applicationId);
  }
}
