package ar.edu.utn.frvm.typeit.boero_api.auth.exceptions;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;
import java.util.Map;

public class InvalidCurrentPasswordException extends ApplicationException {

  public InvalidCurrentPasswordException() {
    super(
        ErrorCategory.INVALID_INPUT,
        AuthMessages.INVALID_CURRENT_PASSWORD,
        Map.of("currentPassword", AuthMessages.INVALID_CURRENT_PASSWORD));
  }
}
