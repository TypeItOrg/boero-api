package ar.edu.utn.frvm.typeit.boero_api.auth.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.UserSession;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class UserSessionRepositoryTest {

  @Autowired private UserSessionRepository userSessionRepository;

  @Test
  @DisplayName("Should find active sessions for user")
  void shouldFindActiveSessionsForUser() {
    UUID userId = UUID.randomUUID();
    UserSession active = userSession(userId, true);
    UserSession inactive = userSession(userId, false);
    UserSession otherUser = userSession(UUID.randomUUID(), true);
    userSessionRepository.save(active);
    userSessionRepository.save(inactive);
    userSessionRepository.saveAndFlush(otherUser);

    assertThat(userSessionRepository.findByUserIdAndActive(userId, true)).containsExactly(active);
  }

  @Test
  @DisplayName("Should page active sessions and count them")
  void shouldPageActiveSessionsAndCountThem() {
    UUID userId = UUID.randomUUID();
    UserSession first = userSession(userId, true);
    UserSession second = userSession(userId, true);
    userSessionRepository.save(first);
    userSessionRepository.save(second);
    userSessionRepository.saveAndFlush(userSession(userId, false));

    var page = userSessionRepository.findByUserIdAndActive(userId, true, PageRequest.of(0, 10));

    assertThat(page.getContent()).containsExactlyInAnyOrder(first, second);
    assertThat(userSessionRepository.countByUserIdAndActive(userId, true)).isEqualTo(2);
  }

  @Test
  @DisplayName("Should find session only for owning user")
  void shouldFindSessionOnlyForOwningUser() {
    UUID userId = UUID.randomUUID();
    UserSession session = userSessionRepository.saveAndFlush(userSession(userId, true));

    assertThat(userSessionRepository.findByIdAndUserId(session.getId(), userId)).contains(session);
    assertThat(userSessionRepository.findByIdAndUserId(session.getId(), UUID.randomUUID())).isEmpty();
  }

  private static UserSession userSession(UUID userId, boolean active) {
    return UserSession.builder()
        .userId(userId)
        .ipAddress("192.0.2.10")
        .userAgent("Mozilla/5.0")
        .active(active)
        .build();
  }
}
