package ar.edu.utn.frvm.typeit.boero_api.security;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.AuthError;
import ar.edu.utn.frvm.typeit.boero_api.auth.filters.AuthRequestAttributes;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private static final String DEFAULT_MESSAGE =
      "Se requiere autenticación para acceder a este recurso.";

  private final SecurityErrorResponseWriter responseWriter;

  @Override
  public void commence(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull AuthenticationException authException)
      throws IOException, ServletException {
    Object attr = request.getAttribute(AuthRequestAttributes.AUTH_ERROR);
    if (attr instanceof AuthError authError) {
      responseWriter.write(response, HttpStatus.UNAUTHORIZED, authError.getMessage());
      return;
    }
    responseWriter.write(response, HttpStatus.UNAUTHORIZED, DEFAULT_MESSAGE);
  }
}
