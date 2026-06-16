package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static ar.edu.utn.frvm.typeit.boero_api.support.AuthTestData.jwtProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.JwtProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.RefreshToken;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.UserSession;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.TokenRefreshException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.RefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserSessionRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.RefreshTokenRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.AuthResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private UserSessionRepository userSessionRepository;
  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;

  private RefreshTokenUseCase refreshTokenUseCase;
  private JwtProperties jwtProperties;

  @BeforeEach
  void setUp() {
    jwtProperties = jwtProperties();
    refreshTokenUseCase =
        new RefreshTokenUseCase(
            refreshTokenRepository,
            userSessionRepository,
            userRepository,
            jwtService,
            jwtProperties);
  }

  @Test
  @DisplayName("Should rotate refresh token and return new auth response")
  void execute_rotatesTokenSuccessfully() {
    String rawToken = "raw-refresh-token";
    String tokenHash = JwtService.hashToken(rawToken);
    UUID sessionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    RefreshToken current = activeToken(tokenHash, "family-1", sessionId);
    UserSession session = activeSession(sessionId, userId, false);
    User user = userWith(userId);

    when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(current));
    when(userSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
    when(userRepository.findWithPersonAndInstitutionById(userId)).thenReturn(Optional.of(user));
    when(jwtService.generateAccessToken(any(InstitutionalAccessTokenInput.class)))
        .thenReturn("new-access-token");

    AuthResponse response = refreshTokenUseCase.execute(new RefreshTokenRequest(rawToken));

    assertThat(response.tokens().accessToken()).isEqualTo("new-access-token");
    assertThat(response.tokens().refreshToken()).isNotBlank();
    assertThat(response.tokens().refreshToken()).isNotEqualTo(rawToken);
    assertThat(current.isRevoked()).isTrue();

    var newTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository, times(2)).save(newTokenCaptor.capture());
    RefreshToken saved = savedNewToken(newTokenCaptor, current);
    assertThat(saved.getFamilyId()).isEqualTo("family-1");
    assertThat(saved.getSessionId()).isEqualTo(sessionId);
    assertThat(saved.isRevoked()).isFalse();
    assertThat(saved.getTokenHash())
        .isEqualTo(JwtService.hashToken(response.tokens().refreshToken()));
  }

  @Test
  @DisplayName("Should throw when the refresh token hash is not found")
  void execute_throwsWhenTokenHashNotFound() {
    String rawToken = "unknown-token";
    when(refreshTokenRepository.findByTokenHash(JwtService.hashToken(rawToken)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> refreshTokenUseCase.execute(new RefreshTokenRequest(rawToken)))
        .isInstanceOf(TokenRefreshException.class);
  }

  @Test
  @DisplayName("Should revoke entire family and all sessions on token reuse detection")
  void execute_revokesEntireFamilyAndSessionsOnTokenReuse() {
    String rawToken = "already-revoked-token";
    String tokenHash = JwtService.hashToken(rawToken);
    UUID sessionId = UUID.randomUUID();

    RefreshToken revokedToken = activeToken(tokenHash, "family-1", sessionId);
    revokedToken.setRevoked(true);

    RefreshToken sibling = activeToken(JwtService.hashToken("sibling"), "family-1", sessionId);

    when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedToken));
    when(refreshTokenRepository.findByFamilyId("family-1"))
        .thenReturn(List.of(revokedToken, sibling));

    assertThatThrownBy(() -> refreshTokenUseCase.execute(new RefreshTokenRequest(rawToken)))
        .isInstanceOf(TokenRefreshException.class)
        .hasMessageContaining("reutilización");

    verify(refreshTokenRepository).revokeByFamilyId("family-1");
    verify(userSessionRepository)
        .deactivateByIds(
            argThat(ids -> ids.size() == 1 && ids.contains(sessionId)), any(LocalDateTime.class));
  }

  @Test
  @DisplayName("Should throw when the refresh token is expired")
  void execute_throwsWhenTokenIsExpired() {
    String rawToken = "expired-token";
    String tokenHash = JwtService.hashToken(rawToken);
    UUID sessionId = UUID.randomUUID();

    RefreshToken expired =
        RefreshToken.builder()
            .sessionId(sessionId)
            .tokenHash(tokenHash)
            .familyId("family-1")
            .expiresAt(LocalDateTime.now().minusDays(1))
            .build();

    when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expired));

    assertThatThrownBy(() -> refreshTokenUseCase.execute(new RefreshTokenRequest(rawToken)))
        .isInstanceOf(TokenRefreshException.class);

    verify(userSessionRepository, never()).findById(any());
  }

  @Test
  @DisplayName("Should throw when the associated session is not active")
  void execute_throwsWhenSessionIsInactive() {
    String rawToken = "valid-token-inactive-session";
    String tokenHash = JwtService.hashToken(rawToken);
    UUID sessionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    RefreshToken token = activeToken(tokenHash, "family-1", sessionId);
    UserSession inactiveSession = activeSession(sessionId, userId, false);
    inactiveSession.setActive(false);

    when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
    when(userSessionRepository.findById(sessionId)).thenReturn(Optional.of(inactiveSession));

    assertThatThrownBy(() -> refreshTokenUseCase.execute(new RefreshTokenRequest(rawToken)))
        .isInstanceOf(TokenRefreshException.class);

    verify(userRepository, never()).findWithPersonAndInstitutionById(any());
  }

  @Test
  @DisplayName("Should throw when the user associated to the session is not found")
  void execute_throwsWhenUserNotFound() {
    String rawToken = "valid-token-missing-user";
    String tokenHash = JwtService.hashToken(rawToken);
    UUID sessionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    RefreshToken token = activeToken(tokenHash, "family-1", sessionId);
    UserSession session = activeSession(sessionId, userId, false);

    when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
    when(userSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
    when(userRepository.findWithPersonAndInstitutionById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> refreshTokenUseCase.execute(new RefreshTokenRequest(rawToken)))
        .isInstanceOf(TokenRefreshException.class);
  }

  @Test
  @DisplayName("Should use extended expiration for new token when session has rememberMe")
  void execute_usesRememberMeExpirationWhenSessionHasRememberMe() {
    String rawToken = "remember-me-token";
    String tokenHash = JwtService.hashToken(rawToken);
    UUID sessionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    RefreshToken current = activeToken(tokenHash, "family-1", sessionId);
    UserSession session = activeSession(sessionId, userId, true);
    User user = userWith(userId);

    when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(current));
    when(userSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
    when(userRepository.findWithPersonAndInstitutionById(userId)).thenReturn(Optional.of(user));
    when(jwtService.generateAccessToken(any(InstitutionalAccessTokenInput.class)))
        .thenReturn("access");

    refreshTokenUseCase.execute(new RefreshTokenRequest(rawToken));

    var captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository, times(2)).save(captor.capture());
    RefreshToken newToken = savedNewToken(captor, current);

    LocalDateTime expectedExpiry =
        LocalDateTime.now().plus(jwtProperties.rememberMeTokenExpiration());
    assertThat(newToken.getExpiresAt()).isCloseTo(expectedExpiry, within(5, ChronoUnit.SECONDS));
  }

  private static RefreshToken savedNewToken(
      ArgumentCaptor<RefreshToken> captor, RefreshToken current) {
    return captor.getAllValues().stream().filter(t -> t != current).findFirst().orElseThrow();
  }

  private static RefreshToken activeToken(String hash, String familyId, UUID sessionId) {
    return RefreshToken.builder()
        .sessionId(sessionId)
        .tokenHash(hash)
        .familyId(familyId)
        .expiresAt(LocalDateTime.now().plusDays(7))
        .build();
  }

  private static UserSession activeSession(UUID id, UUID userId, boolean rememberMe) {
    UserSession session =
        UserSession.builder()
            .userId(userId)
            .ipAddress("192.0.2.1")
            .userAgent("JUnit")
            .rememberMe(rememberMe)
            .build();
    session.setId(id);
    return session;
  }

  private static User userWith(UUID userId) {
    return User.builder()
        .id(userId)
        .institution(Institution.builder().id(UUID.randomUUID()).build())
        .person(Person.builder().documentNumber("12345678").build())
        .password("encoded")
        .build();
  }
}
