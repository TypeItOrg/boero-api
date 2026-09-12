package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;
import java.util.UUID;

public class ApplicationNotEditableException extends ApplicationException {

  public ApplicationNotEditableException(UUID applicationId) {
    super(
        ErrorCategory.CONFLICT,
        "La solicitud con ID " + applicationId + " no se encuentra en estado editable.");
  }
}
