package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static ar.edu.utn.frvm.typeit.boero_api.support.AuthTestData.institutionalPrincipal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.UserSession;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.AccessTokenParseResult;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.RefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserSessionRepository;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
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
class LogoutUseCaseTest {

  @Mock private TokenBlacklistService tokenBlacklistService;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private UserSessionRepository userSessionRepository;
  @Mock private JwtService jwtService;
  @Mock private Claims claims;

  private LogoutUseCase logoutUseCase;

  @BeforeEach
  void setUp() {
    logoutUseCase =
        new LogoutUseCase(
            tokenBlacklistService, refreshTokenRepository, userSessionRepository, jwtService);
  }

  @Test
  @DisplayName("Should blacklist the jti, revoke refresh tokens, and close the session")
  void execute_blacklistsTokenAndClosesSession() {
    UUID sessionId = UUID.randomUUID();
    String tokenId = UUID.randomUUID().toString();
    var principal = institutionalPrincipal(UUID.randomUUID(), UUID.randomUUID(), sessionId);
    UserSession session = activeSession(sessionId);
    Instant futureExpiry = Instant.now().plus(Duration.ofMinutes(10));

    when(jwtService.parseAccessToken("access-token"))
        .thenReturn(new AccessTokenParseResult.Ok(claims));
    when(jwtService.extractTokenId(claims)).thenReturn(tokenId);
    when(claims.getExpiration()).thenReturn(Date.from(futureExpiry));
    when(userSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

    logoutUseCase.execute(principal, "access-token");

    var ttlCaptor = ArgumentCaptor.forClass(Duration.class);
    verify(tokenBlacklistService).blacklist(eq(tokenId), ttlCaptor.capture());
    assertThat(ttlCaptor.getValue()).isPositive();

    verify(refreshTokenRepository).revokeBySessionId(sessionId);
    assertThat(session.isActive()).isFalse();
    assertThat(session.getEndedAt()).isNotNull();
    verify(userSessionRepository).save(session);
  }

  @Test
  @DisplayName("Should close the session even when the access token is already expired")
  void execute_closesSessionWhenTokenIsExpired() {
    UUID sessionId = UUID.randomUUID();
    var principal = institutionalPrincipal(UUID.randomUUID(), UUID.randomUUID(), sessionId);
    UserSession session = activeSession(sessionId);

    when(jwtService.parseAccessToken("expired-token"))
        .thenReturn(new AccessTokenParseResult.Expired());
    when(userSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

    logoutUseCase.execute(principal, "expired-token");

    verify(tokenBlacklistService, never()).blacklist(any(), any());
    verify(refreshTokenRepository).revokeBySessionId(sessionId);
    assertThat(session.isActive()).isFalse();
  }

  @Test
  @DisplayName("Should close the session even when the access token is invalid")
  void execute_closesSessionWhenTokenIsInvalid() {
    UUID sessionId = UUID.randomUUID();
    var principal = institutionalPrincipal(UUID.randomUUID(), UUID.randomUUID(), sessionId);
    UserSession session = activeSession(sessionId);

    when(jwtService.parseAccessToken("garbage")).thenReturn(new AccessTokenParseResult.Invalid());
    when(userSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

    logoutUseCase.execute(principal, "garbage");

    verify(tokenBlacklistService, never()).blacklist(any(), any());
    verify(refreshTokenRepository).revokeBySessionId(sessionId);
    assertThat(session.isActive()).isFalse();
  }

  @Test
  @DisplayName("Should revoke refresh tokens even when the session record is not found")
  void execute_handlesAlreadyClosedSessionGracefully() {
    UUID sessionId = UUID.randomUUID();
    var principal = institutionalPrincipal(UUID.randomUUID(), UUID.randomUUID(), sessionId);

    when(jwtService.parseAccessToken("access-token"))
        .thenReturn(new AccessTokenParseResult.Expired());
    when(userSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

    logoutUseCase.execute(principal, "access-token");

    verify(refreshTokenRepository).revokeBySessionId(sessionId);
    verify(userSessionRepository, never()).save(any());
  }

  private static UserSession activeSession(UUID sessionId) {
    UserSession session =
        UserSession.builder()
            .userId(UUID.randomUUID())
            .ipAddress("192.0.2.1")
            .userAgent("JUnit")
            .build();
    session.setId(sessionId);
    return session;
  }
}
