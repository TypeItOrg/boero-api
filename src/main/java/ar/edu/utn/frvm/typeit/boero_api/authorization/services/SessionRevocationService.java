package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.UserSession;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.RefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserSessionRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PlatformSession;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PlatformRefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PlatformSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionRevocationService {

  private final UserRepository userRepository;
  private final UserSessionRepository userSessionRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PlatformSessionRepository platformSessionRepository;
  private final PlatformRefreshTokenRepository platformRefreshTokenRepository;

  @Transactional
  public void revokeInstitutionalSessionsForPerson(UUID personId, UUID institutionId) {
    userRepository
        .findByPerson_IdAndInstitution_Id(personId, institutionId)
        .ifPresent(user -> revokeInstitutionalSessionsForUser(user.getId()));
  }

  @Transactional
  public void revokeInstitutionalSessionsForUser(UUID userId) {
    deactivateInstitutionalSessions(userSessionRepository.findByUserIdAndActive(userId, true));
  }

  @Transactional
  public void revokePlatformAccountSessions(UUID platformAccountId) {
    deactivatePlatformSessions(
        platformSessionRepository.findByPlatformAccountIdAndActive(platformAccountId, true));
  }

  private void deactivateInstitutionalSessions(List<UserSession> sessions) {
    if (sessions.isEmpty()) return;

    LocalDateTime now = LocalDateTime.now();
    List<UUID> sessionIds = sessions.stream().map(UserSession::getId).toList();
    refreshTokenRepository.revokeBySessionIds(sessionIds);
    userSessionRepository.deactivateByIds(sessionIds, now);
  }

  private void deactivatePlatformSessions(List<PlatformSession> sessions) {
    if (sessions.isEmpty()) return;

    LocalDateTime now = LocalDateTime.now();
    List<UUID> sessionIds = sessions.stream().map(PlatformSession::getId).toList();
    platformRefreshTokenRepository.revokeByPlatformSessionIds(sessionIds);
    platformSessionRepository.deactivateByIds(sessionIds, now);
  }
}
