package ar.edu.utn.frvm.typeit.boero_api.common.exceptions;

import java.util.Map;

public abstract class ApplicationException extends RuntimeException {

  private final ErrorCategory category;
  private final Map<String, String> fieldErrors;

  protected ApplicationException(final ErrorCategory category, final String message) {
    this(category, message, null);
  }

  protected ApplicationException(
      final ErrorCategory category, final String message, final Map<String, String> fieldErrors) {
    super(message);
    this.category = category;
    this.fieldErrors = fieldErrors == null ? null : Map.copyOf(fieldErrors);
  }

  public ErrorCategory category() {
    return category;
  }

  public Map<String, String> fieldErrors() {
    return fieldErrors;
  }
}
