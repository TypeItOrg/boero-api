package ar.edu.utn.frvm.typeit.boero_api.common.exceptions;

import static ar.edu.utn.frvm.typeit.boero_api.security.handlers.SecurityErrorMessages.DEFAULT_FORBIDDEN_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("Should map AccessDeniedException to forbidden payload")
  void shouldMapAccessDeniedExceptionToForbiddenPayload() {
    ExceptionPayload payload =
        handler.handleAccessDeniedException(new AccessDeniedException(DEFAULT_FORBIDDEN_MESSAGE));

    assertThat(payload.status()).isEqualTo(403);
    assertThat(payload.message()).isEqualTo(DEFAULT_FORBIDDEN_MESSAGE);
    assertThat(payload.fieldErrors()).isNull();
  }

  @Test
  @DisplayName("Should map ResponseStatusException to its status and reason")
  void shouldMapResponseStatusExceptionToPayload() {
    ResponseEntity<ExceptionPayload> response =
        handler.handleResponseStatusException(
            new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "La ciudad especificada no existe."));

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(400);
    assertThat(response.getBody().message()).isEqualTo("La ciudad especificada no existe.");
  }

  @Test
  @DisplayName("Should map HttpMessageNotReadableException to bad request payload")
  void shouldMapHttpMessageNotReadableExceptionToBadRequest() {
    ExceptionPayload payload =
        handler.handleHttpMessageNotReadable(
            new HttpMessageNotReadableException(
                "", (org.springframework.http.HttpInputMessage) null));

    assertThat(payload.status()).isEqualTo(400);
    assertThat(payload.message()).isEqualTo("El formato del cuerpo de la solicitud es inválido.");
    assertThat(payload.fieldErrors()).isNull();
  }
}
