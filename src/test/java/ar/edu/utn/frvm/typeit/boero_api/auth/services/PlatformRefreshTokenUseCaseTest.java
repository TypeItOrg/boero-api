package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static ar.edu.utn.frvm.typeit.boero_api.support.AuthTestData.jwtProperties;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformRefreshToken;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformSession;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.TokenRefreshException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformAccountRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformRefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformSessionRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.RefreshTokenRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class PlatformRefreshTokenUseCaseTest {

  @Mock private PlatformRefreshTokenRepository refreshTokenRepository;
  @Mock private PlatformSessionRepository sessionRepository;
  @Mock private PlatformAccountRepository accountRepository;
  @Mock private JwtService jwtService;
  @Mock private CacheManager cacheManager;

  private PlatformRefreshTokenUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new PlatformRefreshTokenUseCase(
            refreshTokenRepository,
            sessionRepository,
            accountRepository,
            jwtService,
            jwtProperties(),
            cacheManager);
  }

  @Test
  @DisplayName("Should revoke the platform token family and its sessions on reuse")
  void execute_revokesFamilyAndSessionsOnReuse() {
    final String rawToken = "reused-platform-token";
    final String hash = JwtService.hashToken(rawToken);
    final UUID sessionId = UUID.randomUUID();
    final PlatformRefreshToken token = token(hash, "family-1", sessionId, UUID.randomUUID());
    token.setRevoked(true);
    when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));
    when(refreshTokenRepository.findByFamilyId("family-1")).thenReturn(List.of(token));

    assertThatThrownBy(() -> useCase.execute(new RefreshTokenRequest(rawToken)))
        .isInstanceOf(TokenRefreshException.class);

    verify(refreshTokenRepository).revokeByFamilyId("family-1");
    verify(sessionRepository)
        .deactivateByIds(
            argThat(ids -> ids.size() == 1 && ids.contains(sessionId)), any(LocalDateTime.class));
  }

  @Test
  @DisplayName("Should reject refresh for a disabled platform account")
  void execute_rejectsDisabledAccount() {
    final String rawToken = "disabled-platform-account";
    final String hash = JwtService.hashToken(rawToken);
    final UUID sessionId = UUID.randomUUID();
    final UUID accountId = UUID.randomUUID();
    final PlatformRefreshToken token = token(hash, "family-1", sessionId, accountId);
    final PlatformSession session =
        PlatformSession.builder().platformAccountId(accountId).active(true).build();
    session.setId(sessionId);
    final PlatformAccount account =
        PlatformAccount.builder()
            .id(accountId)
            .email("disabled@example.com")
            .name("Disabled")
            .lastName("Account")
            .password("encoded")
            .enabled(false)
            .build();
    when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));
    when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    assertThatThrownBy(() -> useCase.execute(new RefreshTokenRequest(rawToken)))
        .isInstanceOf(TokenRefreshException.class);

    verify(refreshTokenRepository, never()).save(any());
  }

  private static PlatformRefreshToken token(
      final String hash, final String familyId, final UUID sessionId, final UUID accountId) {
    return PlatformRefreshToken.builder()
        .platformSessionId(sessionId)
        .platformAccountId(accountId)
        .tokenHash(hash)
        .familyId(familyId)
        .expiresAt(LocalDateTime.now().plusDays(7))
        .build();
  }
}
