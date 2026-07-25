package ar.edu.utn.frvm.typeit.boero_api.common.exceptions;

import java.util.Map;

public class FieldConflictException extends ApplicationException {

  public FieldConflictException(final String field, final String message) {
    super(ErrorCategory.CONFLICT, message, Map.of(field, message));
  }
}
