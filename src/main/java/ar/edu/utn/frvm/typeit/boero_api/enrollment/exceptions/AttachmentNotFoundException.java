package ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;
import java.util.UUID;

public class AttachmentNotFoundException extends ApplicationException {
  public AttachmentNotFoundException(UUID attachmentId) {
    super(ErrorCategory.NOT_FOUND, "No se encontró el archivo adjunto con ID " + attachmentId);
  }

  public AttachmentNotFoundException(String message) {
    super(ErrorCategory.NOT_FOUND, message);
  }
}
