package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class ActiveEnrollmentApplicationExistsException extends ApplicationException {

  public ActiveEnrollmentApplicationExistsException() {
    super(
        ErrorCategory.CONFLICT,
        "Ya existe una solicitud de inscripción activa para ese trayecto formativo.");
  }
}
