package ar.edu.utn.frvm.typeit.boero_api.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

  private final SecurityErrorResponseWriter responseWriter;

  @Override
  public void handle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull AccessDeniedException accessDeniedException)
      throws IOException, ServletException {
    responseWriter.write(
        response, HttpStatus.FORBIDDEN, "No tenés permisos para acceder a este recurso.");
  }
}
