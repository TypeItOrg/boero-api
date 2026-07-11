package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PlatformSession;
import ar.edu.utn.frvm.typeit.boero_api.auth.entities.UserSession;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformRefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.PlatformSessionRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.RefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionRevocationService {

  private static final String ACTIVE_SESSIONS_CACHE = "activeSessions";
  private static final String ACTIVE_PLATFORM_SESSIONS_CACHE = "activePlatformSessions";

  private final UserRepository userRepository;
  private final UserSessionRepository userSessionRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PlatformSessionRepository platformSessionRepository;
  private final PlatformRefreshTokenRepository platformRefreshTokenRepository;
  private final CacheManager cacheManager;

  @Transactional
  public void revokeInstitutionalSessionsForPerson(final UUID personId, final UUID institutionId) {
    userRepository
        .findByPerson_IdAndInstitution_Id(personId, institutionId)
        .ifPresent(user -> revokeInstitutionalSessionsForUser(user.getId()));
  }

  @Transactional
  public void revokeInstitutionalSessionsForUser(final UUID userId) {
    deactivateInstitutionalSessions(userSessionRepository.findByUserIdAndActive(userId, true));
  }

  @Transactional
  public void revokeInstitutionalSessionsForInstitution(final UUID institutionId) {
    deactivateInstitutionalSessions(userSessionRepository.findActiveByInstitutionId(institutionId));
  }

  @Transactional
  public void revokePlatformAccountSessions(final UUID platformAccountId) {
    deactivatePlatformSessions(
        platformSessionRepository.findByPlatformAccountIdAndActive(platformAccountId, true));
  }

  private void deactivateInstitutionalSessions(final List<UserSession> sessions) {
    if (sessions.isEmpty()) {
      return;
    }

    final LocalDateTime now = LocalDateTime.now();
    final List<UUID> sessionIds = sessions.stream().map(UserSession::getId).toList();
    refreshTokenRepository.revokeBySessionIds(sessionIds);
    userSessionRepository.deactivateByIds(sessionIds, now);

    evictSessions(ACTIVE_SESSIONS_CACHE, sessionIds);
  }

  private void deactivatePlatformSessions(final List<PlatformSession> sessions) {
    if (sessions.isEmpty()) {
      return;
    }

    final LocalDateTime now = LocalDateTime.now();
    final List<UUID> sessionIds = sessions.stream().map(PlatformSession::getId).toList();
    platformRefreshTokenRepository.revokeByPlatformSessionIds(sessionIds);
    platformSessionRepository.deactivateByIds(sessionIds, now);

    evictSessions(ACTIVE_PLATFORM_SESSIONS_CACHE, sessionIds);
  }

  private void evictSessions(final String cacheName, final List<UUID> sessionIds) {
    final var cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      sessionIds.forEach(cache::evict);
    }
  }
}
