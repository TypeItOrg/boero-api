package ar.edu.utn.frvm.typeit.boero_api.auth.controllers;

import static ar.edu.utn.frvm.typeit.boero_api.support.AuthTestData.institutionalPrincipal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedUser;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.LoginRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.RefreshTokenRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.RegisterRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.ActiveSessionResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.AuthResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.TokenResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.UserPayload;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.UserRegisteredResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.UserResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.GetActiveSessionsUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.GetCurrentUserUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.IsSessionActiveUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.JwtService;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.LoginUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.LogoutUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.RefreshTokenUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.RegisterUserUseCase;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.TokenBlacklistService;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.PathMatcher;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWebMvcTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PERSON_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID INSTITUTION_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID SESSION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

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
  @DisplayName("Should register user and return created response")
  void shouldRegisterUserAndReturnCreatedResponse() throws Exception {
    when(registerUserUseCase.execute(any(RegisterRequest.class)))
        .thenReturn(
            UserRegisteredResponse.builder()
                .userId(USER_ID)
                .documentNumber("12345678")
                .institutionId(INSTITUTION_ID)
                .build());

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
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
        .andExpect(jsonPath("$.documentNumber").value("12345678"))
        .andExpect(jsonPath("$.institutionId").value(INSTITUTION_ID.toString()));
  }

  @Test
  @DisplayName("Should login and return auth response")
  void shouldLoginAndReturnAuthResponse() throws Exception {
    when(loginUseCase.execute(any(LoginRequest.class), any())).thenReturn(authResponse());

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "documentNumber": "12345678",
                      "password": "password123",
                      "institutionId": "22222222-2222-2222-2222-222222222222",
                      "rememberMe": true
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.userId").value(USER_ID.toString()))
        .andExpect(jsonPath("$.user.personId").value(PERSON_ID.toString()))
        .andExpect(jsonPath("$.tokens.accessToken").value("access-token"))
        .andExpect(jsonPath("$.tokens.refreshToken").value("refresh-token"));
  }

  @Test
  @DisplayName("Should refresh tokens and return auth response")
  void shouldRefreshTokensAndReturnAuthResponse() throws Exception {
    when(refreshTokenUseCase.execute(any(RefreshTokenRequest.class))).thenReturn(authResponse());

    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "refreshToken": "refresh-token"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.documentNumber").value("12345678"))
        .andExpect(jsonPath("$.tokens.refreshToken").value("refresh-token"));
  }

  @Test
  @DisplayName("Should return current user")
  void shouldReturnCurrentUser() throws Exception {
    JwtAuthenticatedUser principal = principal();
    when(getCurrentUserUseCase.execute(principal))
        .thenReturn(
            UserResponse.builder()
                .user(
                    UserPayload.builder()
                        .userId(USER_ID)
                        .personId(PERSON_ID)
                        .name("Ana")
                        .lastName("Garcia")
                        .documentNumber("12345678")
                        .institutionId(INSTITUTION_ID)
                        .build())
                .build());

    mockMvc
        .perform(get("/api/v1/auth/me").principal(authentication(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.userId").value(USER_ID.toString()))
        .andExpect(jsonPath("$.user.personId").value(PERSON_ID.toString()));
  }

  @Test
  @DisplayName("Should return active sessions")
  void shouldReturnActiveSessions() throws Exception {
    JwtAuthenticatedUser principal = principal();
    ActiveSessionResponse session =
        ActiveSessionResponse.builder()
            .sessionId(SESSION_ID)
            .ipAddress("192.0.2.10")
            .userAgent("Mozilla/5.0")
            .startedAt(LocalDateTime.now())
            .currentSession(true)
            .build();
    PaginatedResponse<ActiveSessionResponse> response =
        PaginatedResponse.<ActiveSessionResponse>builder()
            .items(List.of(session))
            .page(0)
            .size(20)
            .totalItems(1)
            .totalPages(1)
            .build();
    when(getActiveSessionsUseCase.execute(eq(principal), any(Pageable.class))).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/auth/sessions").principal(authentication(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].sessionId").value(SESSION_ID.toString()))
        .andExpect(jsonPath("$.totalItems").value(1));
  }

  @Test
  @DisplayName("Should logout and pass bearer token without prefix")
  void shouldLogoutAndPassBearerTokenWithoutPrefix() throws Exception {
    JwtAuthenticatedUser principal = principal();

    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .principal(authentication(principal))
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
        .andExpect(status().isNoContent());

    verify(logoutUseCase).execute(principal, "access-token");
  }

  private static AuthResponse authResponse() {
    return AuthResponse.builder()
        .user(
            UserPayload.builder()
                .userId(USER_ID)
                .personId(PERSON_ID)
                .name("Ana")
                .lastName("Garcia")
                .documentNumber("12345678")
                .institutionId(INSTITUTION_ID)
                .build())
        .tokens(
            TokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build())
        .build();
  }

  private static UserPayload userPayload() {
    return UserPayload.builder()
        .userId(USER_ID)
        .name("Ana")
        .lastName("Garcia")
        .documentNumber("12345678")
        .institutionId(INSTITUTION_ID)
        .build();
  }

  private static JwtAuthenticatedUser principal() {
    return institutionalPrincipal(USER_ID, INSTITUTION_ID, SESSION_ID);
  }

  private static TestingAuthenticationToken authentication(JwtAuthenticatedUser principal) {
    return new TestingAuthenticationToken(principal, null);
  }
}
