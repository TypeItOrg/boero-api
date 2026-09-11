package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.WebAuthnProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.RecentAuthRequiredException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RecentAuthServiceTest {

  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;

  private RecentAuthService service;

  @BeforeEach
  void setUp() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    service =
        new RecentAuthService(
            redisTemplate,
            new WebAuthnProperties(
                "localhost",
                "Boero",
                List.of("http://localhost:3000"),
                Duration.ofMinutes(5),
                Duration.ofMinutes(5),
                10,
                Duration.ofMinutes(5)));
  }

  @Test
  @DisplayName("Should accept a marker bound to the same session and user")
  void isRecent_acceptsMatchingMarker() {
    final UUID sessionId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    when(valueOperations.get(anyString()))
        .thenReturn(userId + "|PASSWORD|" + System.currentTimeMillis());

    assertThat(service.isRecent(sessionId, userId)).isTrue();
  }

  @Test
  @DisplayName("Should reject markers from other users or missing markers")
  void isRecent_rejectsMismatchOrMissing() {
    final UUID sessionId = UUID.randomUUID();
    when(valueOperations.get(anyString())).thenReturn(UUID.randomUUID() + "|PASSWORD|1");

    assertThat(service.isRecent(sessionId, UUID.randomUUID())).isFalse();

    when(valueOperations.get(anyString())).thenReturn(null);

    assertThatThrownBy(() -> service.requireRecent(sessionId, UUID.randomUUID()))
        .isInstanceOf(RecentAuthRequiredException.class);
  }

  @Test
  @DisplayName("Should store the marker with TTL")
  void mark_setsWithTtl() {
    service.mark(UUID.randomUUID(), UUID.randomUUID(), "PASSWORD");

    org.mockito.Mockito.verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
  }
}
