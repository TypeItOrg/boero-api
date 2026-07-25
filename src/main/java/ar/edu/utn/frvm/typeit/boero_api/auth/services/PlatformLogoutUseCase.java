package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.filters.JwtAuthenticatedPlatformAccount;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformRefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformSessionRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformLogoutUseCase {

  private final AccessTokenRevocationService accessTokenRevocationService;
  private final PlatformRefreshTokenRepository platformRefreshTokenRepository;
  private final PlatformSessionRepository platformSessionRepository;

  @org.springframework.cache.annotation.CacheEvict(
      value = "activePlatformSessions",
      key = "#principal.sessionId")
  @Transactional
  public void execute(JwtAuthenticatedPlatformAccount principal, String accessToken) {
    accessTokenRevocationService.revoke(accessToken);

    platformRefreshTokenRepository.revokeByPlatformSessionId(principal.sessionId());
    platformSessionRepository
        .findById(principal.sessionId())
        .ifPresent(
            session -> {
              session.end(LocalDateTime.now());
              platformSessionRepository.save(session);
            });
  }
}
