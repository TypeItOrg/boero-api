package ar.edu.utn.frvm.typeit.boero_api.auth.entities;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthenticationDomainTest {

  @Test
  @DisplayName("Should distinguish own access state from effective access")
  void userDistinguishesOwnAccessFromEffectiveAccess() {
    final Institution institution =
        Institution.builder().id(UUID.randomUUID()).active(false).build();
    final Person person = Person.builder().institution(institution).deleted(false).build();
    final User user =
        User.builder()
            .institution(institution)
            .person(person)
            .password("encoded")
            .enabled(true)
            .build();

    assertThat(user.isAccessEnabled()).isTrue();
    assertThat(user.isEnabled()).isFalse();

    assertThat(user.updateAccess(false)).isTrue();
    assertThat(user.updateAccess(false)).isFalse();
    assertThat(user.isAccessEnabled()).isFalse();
  }

  @Test
  @DisplayName("Should end an active session only once")
  void sessionEndsOnlyOnce() {
    final UserSession session = UserSession.builder().userId(UUID.randomUUID()).build();
    final LocalDateTime endedAt = LocalDateTime.of(2026, 7, 24, 2, 0);

    assertThat(session.end(endedAt)).isTrue();
    assertThat(session.end(endedAt.plusMinutes(1))).isFalse();
    assertThat(session.isActive()).isFalse();
    assertThat(session.getEndedAt()).isEqualTo(endedAt);
  }

  @Test
  @DisplayName("Should revoke a refresh token irreversibly and evaluate its expiry")
  void refreshTokenControlsItsLifecycle() {
    final LocalDateTime expiresAt = LocalDateTime.of(2026, 7, 24, 3, 0);
    final RefreshToken token = RefreshToken.builder().expiresAt(expiresAt).build();

    assertThat(token.isExpiredAt(expiresAt.minusSeconds(1))).isFalse();
    assertThat(token.isExpiredAt(expiresAt.plusSeconds(1))).isTrue();
    assertThat(token.revoke()).isTrue();
    assertThat(token.revoke()).isFalse();
    assertThat(token.isRevoked()).isTrue();
  }
}
