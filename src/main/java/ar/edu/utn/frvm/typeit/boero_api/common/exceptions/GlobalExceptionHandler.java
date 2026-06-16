package ar.edu.utn.frvm.typeit.boero_api.common.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorMessages.INTERNAL_SERVER_ERROR_MESSAGE;
import static ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorMessages.UNHANDLED_EXCEPTION_MESSAGE;
import static ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorMessages.VALIDATION_ERROR_MESSAGE;
import static ar.edu.utn.frvm.typeit.boero_api.security.handlers.SecurityErrorMessages.DEFAULT_FORBIDDEN_MESSAGE;

import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ExceptionPayload handleException(Exception ex) {
    log.error(UNHANDLED_EXCEPTION_MESSAGE, ex.getMessage());
    return ExceptionPayload.builder()
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .message(INTERNAL_SERVER_ERROR_MESSAGE)
        .build();
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ExceptionPayload> handleResponseStatusException(
      ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode())
        .body(
            ExceptionPayload.builder()
                .status(ex.getStatusCode().value())
                .message(ex.getReason())
                .build());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ExceptionPayload handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    Map<String, String> fieldErrors =
        e.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    fe -> Objects.toString(fe.getDefaultMessage(), ""),
                    (existing, replacement) -> existing));
    return validationErrorPayload(fieldErrors);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ExceptionPayload handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
    return ExceptionPayload.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .message("El formato del cuerpo de la solicitud es inválido.")
        .build();
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ExceptionPayload handleConstraintViolationException(ConstraintViolationException e) {
    Map<String, String> fieldErrors =
        e.getConstraintViolations().stream()
            .collect(
                Collectors.toMap(
                    cv -> lastPropertyName(cv.getPropertyPath()),
                    ConstraintViolation::getMessage,
                    (existing, replacement) -> existing));
    return validationErrorPayload(fieldErrors);
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ExceptionPayload handleAccessDeniedException(AccessDeniedException ex) {
    String message =
        ex.getMessage() != null && !ex.getMessage().isBlank()
            ? ex.getMessage()
            : DEFAULT_FORBIDDEN_MESSAGE;
    return ExceptionPayload.builder().status(HttpStatus.FORBIDDEN.value()).message(message).build();
  }

  @ExceptionHandler(DisabledException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ExceptionPayload handleDisabledException(DisabledException ex) {
    return ExceptionPayload.builder()
        .status(HttpStatus.FORBIDDEN.value())
        .message(AuthMessages.USER_DISABLED)
        .build();
  }

  private static ExceptionPayload validationErrorPayload(Map<String, String> fieldErrors) {
    return ExceptionPayload.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .message(VALIDATION_ERROR_MESSAGE)
        .fieldErrors(fieldErrors)
        .build();
  }

  private static String lastPropertyName(Path propertyPath) {
    String name = propertyPath.toString();
    for (Path.Node node : propertyPath) {
      if (node instanceof Path.PropertyNode propertyNode) {
        name = propertyNode.getName();
      }
    }
    return name;
  }
}
