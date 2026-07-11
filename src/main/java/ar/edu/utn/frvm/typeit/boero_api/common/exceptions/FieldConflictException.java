package ar.edu.utn.frvm.typeit.boero_api.common.exceptions;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class FieldConflictException extends ResponseStatusException {

  private final Map<String, String> fieldErrors;

  public FieldConflictException(final String field, final String message) {
    super(HttpStatus.CONFLICT, message);
    this.fieldErrors = Map.of(field, message);
  }

  public Map<String, String> getFieldErrors() {
    return fieldErrors;
  }
}
