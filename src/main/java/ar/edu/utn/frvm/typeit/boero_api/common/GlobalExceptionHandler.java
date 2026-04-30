package ar.edu.utn.frvm.typeit.boero_api.common;

import static ar.edu.utn.frvm.typeit.boero_api.common.CommonConstants.INTERNAL_SERVER_ERROR_MESSAGE;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ExceptionPayload> handleRuntimeException(RuntimeException ex) {
    ResponseStatus responseStatus =
        AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class);

    HttpStatus status =
        responseStatus != null ? responseStatus.value() : HttpStatus.INTERNAL_SERVER_ERROR;

    String message = responseStatus != null ? ex.getMessage() : INTERNAL_SERVER_ERROR_MESSAGE;

    return ResponseEntity.status(status)
        .body(ExceptionPayload.builder().status(status.value()).message(message).build());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ExceptionPayload> handleException(Exception ex) {
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

    return ResponseEntity.status(status)
        .body(
            ExceptionPayload.builder()
                .status(status.value())
                .message(INTERNAL_SERVER_ERROR_MESSAGE)
                .build());
  }
}
