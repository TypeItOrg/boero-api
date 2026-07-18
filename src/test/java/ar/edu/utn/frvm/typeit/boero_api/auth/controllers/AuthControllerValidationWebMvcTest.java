package ar.edu.utn.frvm.typeit.boero_api.auth.controllers;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frvm.typeit.boero_api.auth.services.GetActiveSessionsUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.GetCurrentUserUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.IsSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.JwtService;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.LoginUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.LogoutUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.RefreshTokenUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.RegisterUserUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.PathMatcher;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerValidationWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PathMatcher pathMatcher;
  @MockitoBean private AuthenticationEntryPoint authenticationEntryPoint;
  @MockitoBean private RegisterUserUseCase registerUserUseCase;
  @MockitoBean private LoginUseCase loginUseCase;
  @MockitoBean private RefreshTokenUseCase refreshTokenUseCase;
  @MockitoBean private LogoutUseCase logoutUseCase;
  @MockitoBean private GetActiveSessionsUseCase getActiveSessionsUseCase;
  @MockitoBean private GetCurrentUserUseCase getCurrentUserUseCase;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;
  @MockitoBean private IsSessionActiveUseCase isSessionActiveUseCase;

  @MockitoBean
  private ar.edu.utn.frvm.typeit.boero_api.auth.services.IsPlatformSessionActiveUseCase
      isPlatformSessionActiveUseCase;

  @Test
  @DisplayName("Should reject register when document number is invalid")
  void shouldRejectRegisterWhenDocumentNumberIsInvalid() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Ana",
                      "lastName": "Garcia",
                      "documentNumber": "123",
                      "password": "password123",
                      "institutionId": "22222222-2222-2222-2222-222222222222"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Se encontraron errores de validación."))
        .andExpect(
            jsonPath("$.fieldErrors.documentNumber")
                .value("El número de documento debe tener exactamente 8 dígitos numéricos."));

    verifyNoInteractions(registerUserUseCase);
  }

  @Test
  @DisplayName("Should reject register when institution is missing")
  void shouldRejectRegisterWhenInstitutionIsMissing() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Ana",
                      "lastName": "Garcia",
                      "documentNumber": "12345678",
                      "password": "password123"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Se encontraron errores de validación."))
        .andExpect(jsonPath("$.fieldErrors.institutionId").value("La institución es requerida."));

    verifyNoInteractions(registerUserUseCase);
  }

  @Test
  @DisplayName("Should reject register when birth date is missing")
  void shouldRejectRegisterWhenBirthDateIsMissing() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Ana",
                      "lastName": "Garcia",
                      "documentNumber": "12345678",
                      "password": "password123",
                      "institutionId": "22222222-2222-2222-2222-222222222222"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Se encontraron errores de validación."))
        .andExpect(
            jsonPath("$.fieldErrors.birthDate").value("La fecha de nacimiento es requerida."));

    verifyNoInteractions(registerUserUseCase);
  }

  @Test
  @DisplayName("Should reject register when password is too short")
  void shouldRejectRegisterWhenPasswordIsTooShort() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Ana",
                      "lastName": "Garcia",
                      "documentNumber": "12345678",
                      "password": "short",
                      "institutionId": "22222222-2222-2222-2222-222222222222"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Se encontraron errores de validación."))
        .andExpect(
            jsonPath("$.fieldErrors.password")
                .value("La contraseña debe tener al menos 8 caracteres."));

    verifyNoInteractions(registerUserUseCase);
  }

  @Test
  @DisplayName("Should reject login when document number is invalid")
  void shouldRejectLoginWhenDocumentNumberIsInvalid() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "documentNumber": "ABC",
                      "password": "password123",
                      "institutionId": "22222222-2222-2222-2222-222222222222"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Se encontraron errores de validación."))
        .andExpect(
            jsonPath("$.fieldErrors.documentNumber")
                .value("El número de documento debe tener exactamente 8 dígitos numéricos."));

    verifyNoInteractions(loginUseCase);
  }

  @Test
  @DisplayName("Should reject login when password is missing")
  void shouldRejectLoginWhenPasswordIsMissing() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "documentNumber": "12345678",
                      "institutionId": "22222222-2222-2222-2222-222222222222"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Se encontraron errores de validación."))
        .andExpect(jsonPath("$.fieldErrors.password").value("La contraseña es requerida."));

    verifyNoInteractions(loginUseCase);
  }

  @Test
  @DisplayName("Should reject refresh when refresh token is blank")
  void shouldRejectRefreshWhenRefreshTokenIsBlank() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "refreshToken": ""
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Se encontraron errores de validación."))
        .andExpect(
            jsonPath("$.fieldErrors.refreshToken")
                .value("El token de actualización es requerido."));

    verifyNoInteractions(refreshTokenUseCase);
  }
}
