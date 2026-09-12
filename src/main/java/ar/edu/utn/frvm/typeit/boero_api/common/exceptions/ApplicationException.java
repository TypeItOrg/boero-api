package ar.edu.utn.frvm.typeit.boero_api.common.exceptions;

import java.util.Map;

public abstract class ApplicationException extends RuntimeException {

  private final ErrorCategory category;
  private final Map<String, String> fieldErrors;
  private final String code;

  protected ApplicationException(final ErrorCategory category, final String message) {
    this(category, message, null, null);
  }

  protected ApplicationException(
      final ErrorCategory category, final String message, final Map<String, String> fieldErrors) {
    this(category, message, fieldErrors, null);
  }

  protected ApplicationException(
      final ErrorCategory category, final String message, final String code) {
    this(category, message, null, code);
  }

  protected ApplicationException(
      final ErrorCategory category,
      final String message,
      final Map<String, String> fieldErrors,
      final String code) {
    super(message);
    this.category = category;
    this.fieldErrors = fieldErrors == null ? null : Map.copyOf(fieldErrors);
    this.code = code;
  }

  public ErrorCategory category() {
    return category;
  }

  public Map<String, String> fieldErrors() {
    return fieldErrors;
  }

  public String code() {
    return code;
  }
}
