package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InvalidCredentialsException;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.LoginRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.AuthResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.security.InstitutionalUsername;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorityResolver;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

  @Mock private CredentialsAuthenticator credentialsAuthenticator;
  @Mock private LoginSessionPersistenceService loginSessionPersistenceService;
  @Mock private JwtService jwtService;
  @Mock private HttpServletRequest httpRequest;
  @Mock private AuthorityResolver authorityResolver;

  private LoginUseCase loginUseCase;

  @BeforeEach
  void setUp() {
    loginUseCase =
        new LoginUseCase(
            credentialsAuthenticator,
            loginSessionPersistenceService,
            jwtService,
            authorityResolver);
  }

  @Test
  @DisplayName("Should authenticate using the institutional principal format")
  void execute_authenticatesWithInstitutionalPrincipal() {
    UUID institutionId = UUID.randomUUID();
    LoginRequest request = loginRequest(institutionId, false);
    User user = userWith(institutionId, "12345678");
    stubSuccessfulAuth(user);

    loginUseCase.execute(request, httpRequest);

    var principalCaptor = ArgumentCaptor.forClass(String.class);
    verify(credentialsAuthenticator).authenticate(principalCaptor.capture(), eq("secret"));
    assertThat(principalCaptor.getValue())
        .isEqualTo(InstitutionalUsername.format(institutionId, "12345678"));
  }

  @Test
  @DisplayName("Should delegate session creation with correct metadata")
  void execute_delegatesSessionCreationWithCorrectMetadata() {
    UUID institutionId = UUID.randomUUID();
    LoginRequest request = loginRequest(institutionId, false);
    User user = userWith(institutionId, "12345678");
    when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
    when(httpRequest.getRemoteAddr()).thenReturn("192.0.2.1");
    stubCredentialsAuthenticator(user);

    loginUseCase.execute(request, httpRequest);

    verify(loginSessionPersistenceService).create(user.getId(), "192.0.2.1", "Mozilla/5.0", false);
  }

  @Test
  @DisplayName("Should use the remote address resolved by the servlet container")
  void execute_usesContainerResolvedRemoteAddress() {
    UUID institutionId = UUID.randomUUID();
    LoginRequest request = loginRequest(institutionId, false);
    User user = userWith(institutionId, "12345678");
    when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");
    when(httpRequest.getRemoteAddr()).thenReturn("10.0.0.1");
    stubCredentialsAuthenticator(user);

    loginUseCase.execute(request, httpRequest);

    verify(loginSessionPersistenceService).create(user.getId(), "10.0.0.1", "JUnit", false);
  }

  @Test
  @DisplayName("Should return the refresh token created by the persistence service")
  void execute_returnsRefreshTokenCreatedByPersistenceService() {
    UUID institutionId = UUID.randomUUID();
    LoginRequest request = loginRequest(institutionId, false);
    User user = userWith(institutionId, "12345678");
    stubSuccessfulAuth(user);

    AuthResponse response = loginUseCase.execute(request, httpRequest);

    assertThat(response.tokens().refreshToken()).isEqualTo("generated-refresh-token");
  }

  @Test
  @DisplayName("Should pass rememberMe to session persistence")
  void execute_passesRememberMeToSessionPersistence() {
    UUID institutionId = UUID.randomUUID();
    LoginRequest request = loginRequest(institutionId, true);
    User user = userWith(institutionId, "12345678");
    stubSuccessfulAuth(user);

    loginUseCase.execute(request, httpRequest);

    verify(loginSessionPersistenceService).create(eq(user.getId()), any(), any(), eq(true));
  }

  @Test
  @DisplayName("Should return an AuthResponse containing the user and both tokens")
  void execute_returnsAuthResponseWithUserAndTokens() {
    UUID institutionId = UUID.randomUUID();
    LoginRequest request = loginRequest(institutionId, false);
    User user = userWith(institutionId, "12345678");
    when(jwtService.generateAccessToken(any(InstitutionalAccessTokenInput.class)))
        .thenReturn("generated-access-token");
    stubSuccessfulAuth(user);

    AuthResponse response = loginUseCase.execute(request, httpRequest);

    assertThat(response).isNotNull();
    assertThat(response.tokens().accessToken()).isEqualTo("generated-access-token");
    assertThat(response.tokens().refreshToken()).isNotBlank();
    assertThat(response.user().documentNumber()).isEqualTo("12345678");
    assertThat(response.user().institutionId()).isEqualTo(institutionId);
  }

  @Test
  @DisplayName("Should throw InvalidCredentialsException on bad credentials")
  void execute_propagatesBadCredentials() {
    UUID institutionId = UUID.randomUUID();
    LoginRequest request = loginRequest(institutionId, false);
    when(credentialsAuthenticator.authenticate(any(), any()))
        .thenThrow(new InvalidCredentialsException());

    assertThatThrownBy(() -> loginUseCase.execute(request, httpRequest))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("Should propagate DisabledException when user is disabled")
  void execute_propagatesDisabled() {
    UUID institutionId = UUID.randomUUID();
    LoginRequest request = loginRequest(institutionId, false);
    when(credentialsAuthenticator.authenticate(any(), any()))
        .thenThrow(new DisabledException("off"));

    assertThatThrownBy(() -> loginUseCase.execute(request, httpRequest))
        .isInstanceOf(DisabledException.class);
  }

  private static LoginRequest loginRequest(UUID institutionId, boolean rememberMe) {
    return new LoginRequest("12345678", "secret", institutionId, rememberMe);
  }

  private static User userWith(UUID institutionId, String documentNumber) {
    return User.builder()
        .id(UUID.randomUUID())
        .institution(Institution.builder().id(institutionId).build())
        .person(Person.builder().documentNumber(documentNumber).build())
        .password("encoded-hash")
        .build();
  }

  private void stubSuccessfulAuth(User user) {
    when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");
    when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
    stubCredentialsAuthenticator(user);
  }

  private void stubCredentialsAuthenticator(User user) {
    when(credentialsAuthenticator.authenticate(any(), any()))
        .thenAnswer(
            invocation -> {
              return UsernamePasswordAuthenticationToken.authenticated(
                  user, invocation.getArgument(1), user.getAuthorities());
            });
    when(loginSessionPersistenceService.create(any(), any(), any(), anyBoolean()))
        .thenReturn(
            new LoginSessionPersistenceService.Result(
                UUID.randomUUID(), "generated-refresh-token"));
  }
}
