package ar.edu.utn.frvm.typeit.boero_api.common;

import static ar.edu.utn.frvm.typeit.boero_api.common.CommonConstants.INTERNAL_SERVER_ERROR_MESSAGE;

import java.util.Comparator;
import java.util.Objects;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.validation.FieldError;
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

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ExceptionPayload> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e) {
    FieldError fieldError =
        e.getBindingResult().getFieldErrors().stream()
            .min(
                Comparator.<FieldError>comparingInt(GlobalExceptionHandler::constraintPriority)
                    .thenComparing(FieldError::getField)
                    .thenComparing(
                        fe -> Objects.toString(fe.getDefaultMessage(), ""), String::compareTo))
            .orElseThrow();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ExceptionPayload.builder()
                .message(Objects.requireNonNull(fieldError.getDefaultMessage()))
                .status(HttpStatus.BAD_REQUEST.value())
                .build());
  }

  /**
   * Prioridad estable cuando varias restricciones fallan en el mismo request: el orden entre
   * validadores no está definido en Bean Validation; sin esto el mensaje expuesto puede variar.
   */
  private static int constraintPriority(FieldError fe) {
    String code = constraintCode(fe);
    return switch (code) {
      case "NotNull", "NotBlank", "NotEmpty" -> 0;
      case "Size" -> 1;
      case "Min", "Max", "DecimalMin", "DecimalMax" -> 2;
      case "Pattern" -> 3;
      default -> 50;
    };
  }

  private static String constraintCode(FieldError fe) {
    String[] codes = fe.getCodes();
    if (codes == null || codes.length == 0) {
      return "";
    }
    String first = codes[0];
    int dot = first.indexOf('.');
    return dot > 0 ? first.substring(0, dot) : first;
  }
}
