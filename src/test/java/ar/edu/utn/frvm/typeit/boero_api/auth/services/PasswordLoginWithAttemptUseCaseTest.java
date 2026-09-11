package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InvalidCredentialsException;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.InvalidLoginAttemptException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.PasswordLoginRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.AuthResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.TokenResponse;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.UserPayload;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class PasswordLoginWithAttemptUseCaseTest {

  @Mock private LoginAttemptService loginAttemptService;
  @Mock private UserRepository userRepository;
  @Mock private CredentialsAuthenticator credentialsAuthenticator;
  @Mock private AuthenticationSessionIssuer sessionIssuer;
  @Mock private AuthRateLimitService rateLimitService;
  @Mock private ar.edu.utn.frvm.typeit.boero_api.auth.config.AuthRateLimitProperties rateLimits;
  @Mock private HttpServletRequest httpRequest;

  private PasswordLoginWithAttemptUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new PasswordLoginWithAttemptUseCase(
            loginAttemptService,
            userRepository,
            credentialsAuthenticator,
            sessionIssuer,
            rateLimitService,
            rateLimits);
    org.mockito.Mockito.lenient().when(rateLimits.passwordMax()).thenReturn(10);
    org.mockito.Mockito.lenient()
        .when(rateLimits.passwordWindow())
        .thenReturn(java.time.Duration.ofMinutes(1));
    org.mockito.Mockito.lenient().when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");
    org.mockito.Mockito.lenient().when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
  }

  @Test
  @DisplayName("Should claim the attempt between verification and session issuance")
  void execute_claimsAttemptBetweenVerificationAndIssuance() {
    final UUID institutionId = UUID.randomUUID();
    final User user = userWith(institutionId);
    final LoginAttempt attempt =
        new LoginAttempt("attempt", user.getId(), institutionId, false, Instant.now());
    when(loginAttemptService.resolve("attempt")).thenReturn(attempt);
    when(userRepository.findWithPersonAndInstitutionById(user.getId()))
        .thenReturn(Optional.of(user));
    when(credentialsAuthenticator.authenticate(any(), any()))
        .thenReturn(
            UsernamePasswordAuthenticationToken.authenticated(
                user, "secret", user.getAuthorities()));
    when(sessionIssuer.issue(any(), any(), any(), any(boolean.class), any()))
        .thenReturn(
            AuthResponse.builder()
                .user(UserPayload.from(user, user.getPerson().getId(), Set.of()))
                .tokens(
                    TokenResponse.builder().accessToken("access").refreshToken("refresh").build())
                .build());

    final AuthResponse response =
        useCase.execute(new PasswordLoginRequest("attempt", "secret", false), httpRequest);

    assertThat(response.tokens().accessToken()).isEqualTo("access");
    final InOrder order = inOrder(loginAttemptService, credentialsAuthenticator, sessionIssuer);
    order.verify(loginAttemptService).resolve("attempt");
    order.verify(credentialsAuthenticator).authenticate(any(), any());
    order.verify(loginAttemptService).claim("attempt");
    order.verify(sessionIssuer).issue(any(), any(), any(), any(boolean.class), any(String.class));
  }

  @Test
  @DisplayName("Should reject login when the attempt institution no longer matches")
  void execute_rejectsInstitutionMismatch() {
    final User user = userWith(UUID.randomUUID());
    final LoginAttempt attempt =
        new LoginAttempt("attempt", user.getId(), UUID.randomUUID(), false, Instant.now());
    when(loginAttemptService.resolve("attempt")).thenReturn(attempt);
    when(userRepository.findWithPersonAndInstitutionById(user.getId()))
        .thenReturn(Optional.of(user));

    assertThatThrownBy(
            () ->
                useCase.execute(new PasswordLoginRequest("attempt", "secret", false), httpRequest))
        .isInstanceOf(InvalidLoginAttemptException.class);
  }

  @Test
  @DisplayName("Should propagate invalid credentials without invalidating state twice")
  void execute_propagatesBadPassword() {
    final UUID institutionId = UUID.randomUUID();
    final User user = userWith(institutionId);
    final LoginAttempt attempt =
        new LoginAttempt("attempt", user.getId(), institutionId, false, Instant.now());
    when(loginAttemptService.resolve("attempt")).thenReturn(attempt);
    when(userRepository.findWithPersonAndInstitutionById(user.getId()))
        .thenReturn(Optional.of(user));
    when(credentialsAuthenticator.authenticate(any(), any()))
        .thenThrow(new InvalidCredentialsException());

    assertThatThrownBy(
            () -> useCase.execute(new PasswordLoginRequest("attempt", "wrong", false), httpRequest))
        .isInstanceOf(InvalidCredentialsException.class);
    verify(loginAttemptService, never()).claim(any());
    verify(sessionIssuer, never())
        .issue(any(), any(), any(), any(boolean.class), any(String.class));
  }

  @Test
  @DisplayName("Should reject login without claiming when the account is disabled")
  void execute_rejectsDisabledAccountWithoutClaiming() {
    final UUID institutionId = UUID.randomUUID();
    final User user = userWith(institutionId);
    user.updateAccess(false);
    final LoginAttempt attempt =
        new LoginAttempt("attempt", user.getId(), institutionId, false, Instant.now());
    when(loginAttemptService.resolve("attempt")).thenReturn(attempt);
    when(userRepository.findWithPersonAndInstitutionById(user.getId()))
        .thenReturn(Optional.of(user));
    when(credentialsAuthenticator.authenticate(any(), any()))
        .thenReturn(
            UsernamePasswordAuthenticationToken.authenticated(
                user, "secret", user.getAuthorities()));

    assertThatThrownBy(
            () ->
                useCase.execute(new PasswordLoginRequest("attempt", "secret", false), httpRequest))
        .isInstanceOf(InvalidCredentialsException.class);
    verify(loginAttemptService, never()).claim(any());
    verify(sessionIssuer, never())
        .issue(any(), any(), any(), any(boolean.class), any(String.class));
  }

  @Test
  @DisplayName("Should reject login without claiming when the institution is inactive")
  void execute_rejectsInactiveInstitutionWithoutClaiming() {
    final UUID institutionId = UUID.randomUUID();
    final User user = userWith(institutionId);
    user.getInstitution().updateStatus(false);
    final LoginAttempt attempt =
        new LoginAttempt("attempt", user.getId(), institutionId, false, Instant.now());
    when(loginAttemptService.resolve("attempt")).thenReturn(attempt);
    when(userRepository.findWithPersonAndInstitutionById(user.getId()))
        .thenReturn(Optional.of(user));
    when(credentialsAuthenticator.authenticate(any(), any()))
        .thenReturn(
            UsernamePasswordAuthenticationToken.authenticated(
                user, "secret", user.getAuthorities()));

    assertThatThrownBy(
            () ->
                useCase.execute(new PasswordLoginRequest("attempt", "secret", false), httpRequest))
        .isInstanceOf(InvalidCredentialsException.class);
    verify(loginAttemptService, never()).claim(any());
    verify(sessionIssuer, never())
        .issue(any(), any(), any(), any(boolean.class), any(String.class));
  }

  private static User userWith(final UUID institutionId) {
    final Institution institution = Institution.builder().id(institutionId).build();
    return User.builder()
        .id(UUID.randomUUID())
        .institution(institution)
        .person(
            Person.builder()
                .id(UUID.randomUUID())
                .institution(institution)
                .documentNumber("12345678")
                .build())
        .password("hash")
        .build();
  }
}
