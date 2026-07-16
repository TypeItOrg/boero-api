package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.JwtProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformRefreshToken;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformSession;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.TokenRefreshException;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformAccountRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformRefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformSessionRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.requests.RefreshTokenRequest;
import ar.edu.utn.frvm.typeit.boero_api.auth.payloads.responses.PlatformAuthResponse;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformRefreshTokenUseCase {

  private static final String ACTIVE_SESSIONS_CACHE = "activePlatformSessions";

  private final PlatformRefreshTokenRepository platformRefreshTokenRepository;
  private final PlatformSessionRepository platformSessionRepository;
  private final PlatformAccountRepository platformAccountRepository;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;
  private final CacheManager cacheManager;
  private final PlatformRefreshReplayCache replayCache;

  @Transactional(noRollbackFor = TokenRefreshException.class)
  public PlatformAuthResponse execute(final RefreshTokenRequest request) {
    final String hash = JwtService.hashToken(request.refreshToken());
    final PlatformRefreshToken current =
        platformRefreshTokenRepository
            .findByTokenHash(hash)
            .orElseThrow(TokenRefreshException::invalid);

    if (current.isRevoked()) {
      final Optional<PlatformRefreshReplay> replay = replayCache.get(hash);
      if (replay.isPresent()) {
        findActiveSession(current);
        final PlatformAccount account = findEnabledAccount(current);
        return PlatformAuthResponse.of(
            account, replay.get().accessToken(), replay.get().refreshToken());
      }
      handleReuse(current);
    }

    if (current.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw TokenRefreshException.invalid();
    }

    final PlatformSession session = findActiveSession(current);
    final PlatformAccount account = findEnabledAccount(current);

    current.setRevoked(true);
    platformRefreshTokenRepository.save(current);

    String rawRefresh = UUID.randomUUID().toString();
    PlatformRefreshToken next =
        PlatformRefreshToken.builder()
            .platformSessionId(session.getId())
            .platformAccountId(account.getId())
            .tokenHash(JwtService.hashToken(rawRefresh))
            .familyId(current.getFamilyId())
            .expiresAt(
                LocalDateTime.now().plus(jwtProperties.refreshExpiration(session.isRememberMe())))
            .build();
    platformRefreshTokenRepository.save(next);

    String accessToken =
        jwtService.generatePlatformAccessToken(
            PlatformAccessTokenInput.builder()
                .platformAccountId(account.getId())
                .email(account.getEmail())
                .sessionId(session.getId())
                .build());
    final PlatformAuthResponse response = PlatformAuthResponse.of(account, accessToken, rawRefresh);
    replayCache.put(hash, new PlatformRefreshReplay(accessToken, rawRefresh));
    return response;
  }

  private PlatformSession findActiveSession(final PlatformRefreshToken current) {
    final PlatformSession session =
        platformSessionRepository
            .findById(current.getPlatformSessionId())
            .orElseThrow(TokenRefreshException::invalid);

    if (!session.isActive()) {
      throw TokenRefreshException.invalid();
    }

    return session;
  }

  private PlatformAccount findEnabledAccount(final PlatformRefreshToken current) {
    final PlatformAccount account =
        platformAccountRepository
            .findById(current.getPlatformAccountId())
            .orElseThrow(TokenRefreshException::invalid);

    if (!account.isEnabled()) {
      throw TokenRefreshException.invalid();
    }

    return account;
  }

  private void handleReuse(PlatformRefreshToken current) {
    var inFamily = platformRefreshTokenRepository.findByFamilyId(current.getFamilyId());
    Set<UUID> sessionIds = new HashSet<>();
    for (PlatformRefreshToken token : inFamily) {
      sessionIds.add(token.getPlatformSessionId());
    }
    platformRefreshTokenRepository.revokeByFamilyId(current.getFamilyId());
    LocalDateTime now = LocalDateTime.now();
    if (!sessionIds.isEmpty()) {
      platformSessionRepository.deactivateByIds(sessionIds, now);
      final var cache = cacheManager.getCache(ACTIVE_SESSIONS_CACHE);
      if (cache != null) {
        sessionIds.forEach(cache::evict);
      }
    }
    throw TokenRefreshException.reuse();
  }
}
