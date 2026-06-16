package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.UserSession;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.RefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserSessionRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PlatformRefreshTokenRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PlatformSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionRevocationServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private UserSessionRepository userSessionRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private PlatformSessionRepository platformSessionRepository;
  @Mock private PlatformRefreshTokenRepository platformRefreshTokenRepository;

  @Captor private ArgumentCaptor<List<UUID>> sessionIdsCaptor;
  @Captor private ArgumentCaptor<LocalDateTime> endedAtCaptor;

  @InjectMocks private SessionRevocationService sessionRevocationService;

  @Test
  @DisplayName("Should deactivate institutional sessions when revoking by person")
  void revokeInstitutionalSessionsForPerson_closesActiveSessions() {
    UUID personId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    UserSession session =
        UserSession.builder().userId(userId).ipAddress("127.0.0.1").userAgent("JUnit").build();
    session.setId(sessionId);

    whenUserFound(personId, institutionId, userId);
    when(userSessionRepository.findByUserIdAndActive(userId, true)).thenReturn(List.of(session));

    sessionRevocationService.revokeInstitutionalSessionsForPerson(personId, institutionId);

    verify(refreshTokenRepository).revokeBySessionIds(sessionIdsCaptor.capture());
    assertThat(sessionIdsCaptor.getValue()).containsExactly(sessionId);
    verify(userSessionRepository)
        .deactivateByIds(sessionIdsCaptor.capture(), endedAtCaptor.capture());
    assertThat(sessionIdsCaptor.getValue()).containsExactly(sessionId);
    assertThat(endedAtCaptor.getValue()).isNotNull();
  }

  private void whenUserFound(UUID personId, UUID institutionId, UUID userId) {
    when(userRepository.findByPerson_IdAndInstitution_Id(personId, institutionId))
        .thenReturn(
            Optional.of(
                ar.edu.utn.frvm.typeit.boero_api.auth.entities.User.builder().id(userId).build()));
  }
}
