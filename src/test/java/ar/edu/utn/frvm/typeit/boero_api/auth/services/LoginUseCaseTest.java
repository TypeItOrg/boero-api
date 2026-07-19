package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static ar.edu.utn.frvm.typeit.boero_api.support.AuthTestData.jwtProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.JwtProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.RefreshToken;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.UserSession;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InvalidCredentialsException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.RefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserSessionRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.LoginRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.AuthResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.security.InstitutionalUsername;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AuthorityResolver;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

  @Mock private AuthenticationManager authenticationManager;
  @Mock private UserSessionRepository userSessionRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private JwtService jwtService;
  @Mock private HttpServletRequest httpRequest;
  @Mock private AuthorityResolver authorityResolver;

  private LoginUseCase loginUseCase;
  private JwtProperties jwtProperties;

  @BeforeEach
  void setUp() {
    jwtProperties = jwtProperties();
    loginUseCase =
        new LoginUseCase(
            authenticationManager,
            userSessionRepository,
            refreshTokenRepository,
            jwtService,
            jwtProperties,
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

    var captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
    verify(authenticationManager).authenticate(captor.capture());
    UsernamePasswordAuthenticationToken submitted = captor.getValue();
    assertThat(submitted.getPrincipal())
        .isEqualTo(InstitutionalUsername.format(institutionId, "12345678"));
    assertThat(submitted.getCredentials()).isEqualTo("secret");
  }

  @Test
  @DisplayName("Should create a session with correct metadata from the request")
  void execute_createsSessionWithCorrectMetadata() {
    UUID institutionId = UUID.randomUUID();
    LoginRequest request = loginRequest(institutionId, false);
    User user = userWith(institutionId, "12345678");
    when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
    when(httpRequest.getRemoteAddr()).thenReturn("192.0.2.1");
    stubAuthManager(user);
    stubSessionSave();

    loginUseCase.execute(request, httpRequest);

    var sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
    verify(userSessionRepository).save(sessionCaptor.capture());
    UserSession savedSession = sessionCaptor.getValue();
    assertThat(savedSession.getUserId()).isEqualTo(user.getId());
    assertThat(savedSession.getIpAddress()).isEqualTo("192.0.2.1");
    assertThat(savedSession.getUserAgent()).isEqualTo("Mozilla/5.0");
    assertThat(savedSession.isRememberMe()).isFalse();
    assertThat(savedSession.isActive()).isTrue();
  }

  @Test
  @DisplayName("Should use the remote address resolved by the servlet container")
  void execute_usesContainerResolvedRemoteAddress() {
    UUID institutionId = UUID.randomUUID();
    LoginRequest request = loginRequest(institutionId, false);
    User user = userWith(institutionId, "12345678");
    when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");
    when(httpRequest.getRemoteAddr()).thenReturn("10.0.0.1");
    stubAuthManager(user);
    stubSessionSave();

    loginUseCase.execute(request, httpRequest);

    var sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
    verify(userSessionRepository).save(sessionCaptor.capture());
    assertThat(sessionCaptor.getValue().getIpAddress()).isEqualTo("10.0.0.1");
  }

  @Test
  @DisplayName("Should save a hashed refresh token, not the raw value")
  void execute_savesRefreshTokenWithHashedValue() {
    UUID institutionId = UUID.randomUUID();
    LoginRequest request = loginRequest(institutionId, false);
    User user = userWith(institutionId, "12345678");
    stubSuccessfulAuth(user);

    AuthResponse response = loginUseCase.execute(request, httpRequest);

    var tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(tokenCaptor.capture());
    RefreshToken saved = tokenCaptor.getValue();

    String rawRefreshToken = response.tokens().refreshToken();
    assertThat(saved.getTokenHash()).isNotEqualTo(rawRefreshToken);
    assertThat(saved.getTokenHash()).isEqualTo(JwtService.hashToken(rawRefreshToken));
    assertThat(saved.getFamilyId()).isNotBlank();
    assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
    assertThat(saved.isRevoked()).isFalse();
  }

  @Test
  @DisplayName("Should use the extended expiration when rememberMe is true")
  void execute_usesRememberMeExpirationWhenFlagIsTrue() {
    UUID institutionId = UUID.randomUUID();
    LoginRequest request = loginRequest(institutionId, true);
    User user = userWith(institutionId, "12345678");
    stubSuccessfulAuth(user);

    loginUseCase.execute(request, httpRequest);

    var sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
    verify(userSessionRepository).save(sessionCaptor.capture());
    assertThat(sessionCaptor.getValue().isRememberMe()).isTrue();

    var tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(tokenCaptor.capture());
    LocalDateTime expectedExpiry =
        LocalDateTime.now().plus(jwtProperties.rememberMeTokenExpiration());
    assertThat(tokenCaptor.getValue().getExpiresAt())
        .isCloseTo(expectedExpiry, within(5, ChronoUnit.SECONDS));
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
    when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

    assertThatThrownBy(() -> loginUseCase.execute(request, httpRequest))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("Should propagate DisabledException when user is disabled")
  void execute_propagatesDisabled() {
    UUID institutionId = UUID.randomUUID();
    LoginRequest request = loginRequest(institutionId, false);
    when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("off"));

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
    stubAuthManager(user);
    stubSessionSave();
  }

  private void stubAuthManager(User user) {
    when(authenticationManager.authenticate(any()))
        .thenAnswer(
            invocation -> {
              UsernamePasswordAuthenticationToken token = invocation.getArgument(0);
              return UsernamePasswordAuthenticationToken.authenticated(
                  user, token.getCredentials(), user.getAuthorities());
            });
  }

  private void stubSessionSave() {
    when(userSessionRepository.save(any(UserSession.class)))
        .thenAnswer(
            invocation -> {
              UserSession session = invocation.getArgument(0);
              session.setId(UUID.randomUUID());
              return session;
            });
  }
}
