package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import java.util.UUID;

public class ApplicationNotEditableException extends RuntimeException {

  public ApplicationNotEditableException(UUID applicationId) {
    super("La solicitud con ID " + applicationId + " no se encuentra en estado editable.");
  }
}
