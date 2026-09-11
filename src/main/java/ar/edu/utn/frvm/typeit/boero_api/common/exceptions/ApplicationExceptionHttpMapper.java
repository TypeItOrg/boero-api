package ar.edu.utn.frvm.typeit.boero_api.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ApplicationExceptionHttpMapper {

  public ResponseEntity<ExceptionPayload> toResponse(final ApplicationException exception) {
    final HttpStatus status = statusFor(exception.category());
    return ResponseEntity.status(status)
        .body(
            ExceptionPayload.builder()
                .status(status.value())
                .message(exception.getMessage())
                .fieldErrors(exception.fieldErrors())
                .code(exception.code())
                .build());
  }

  private static HttpStatus statusFor(final ErrorCategory category) {
    return switch (category) {
      case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
      case AUTHENTICATION -> HttpStatus.UNAUTHORIZED;
      case AUTHORIZATION -> HttpStatus.FORBIDDEN;
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      case CONFLICT -> HttpStatus.CONFLICT;
      case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
      case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
    };
  }
}
