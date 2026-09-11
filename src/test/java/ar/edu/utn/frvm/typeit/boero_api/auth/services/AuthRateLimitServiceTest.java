package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.AuthRateLimitProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.RateLimitExceededException;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.RateLimitUnavailableException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class AuthRateLimitServiceTest {

  private static final String KEY_SECRET = "test-key-secret";

  @Mock private StringRedisTemplate redisTemplate;
  @Mock private AuthRateLimitProperties rateLimitProperties;

  private AuthRateLimitService service;

  @BeforeEach
  void setUp() {
    lenient().when(rateLimitProperties.keySecret()).thenReturn(KEY_SECRET);
    service = new AuthRateLimitService(redisTemplate, rateLimitProperties);
  }

  @Test
  @DisplayName("Should allow requests under the limit")
  void checkAllowed_allowsUnderLimit() {
    doReturn(1L).when(redisTemplate).execute(any(), anyList(), any());

    service.checkAllowed("scope", "key", 3, Duration.ofMinutes(1));
  }

  @Test
  @DisplayName("Should reject requests over the limit with 429")
  void checkAllowed_rejectsOverLimit() {
    doReturn(4L).when(redisTemplate).execute(any(), anyList(), any());

    assertThatThrownBy(() -> service.checkAllowed("scope", "key", 3, Duration.ofMinutes(1)))
        .isInstanceOf(RateLimitExceededException.class);
  }

  @Test
  @DisplayName("Should fail closed when Redis is unavailable")
  void checkAllowed_failsClosedWhenRedisIsUnavailable() {
    doThrow(new RedisConnectionFailureException("Redis down"))
        .when(redisTemplate)
        .execute(any(), anyList(), any());

    assertThatThrownBy(() -> service.checkAllowed("scope", "key", 3, Duration.ofMinutes(1)))
        .isInstanceOf(RateLimitUnavailableException.class);
  }

  @Test
  @DisplayName("Should fail closed when Redis returns no count")
  void checkAllowed_failsClosedWhenRedisReturnsNoCount() {
    doReturn(null).when(redisTemplate).execute(any(), anyList(), any());

    assertThatThrownBy(() -> service.checkAllowed("scope", "key", 3, Duration.ofMinutes(1)))
        .isInstanceOf(RateLimitUnavailableException.class);
  }

  @Test
  @DisplayName("Should hash raw identifiers with HMAC instead of exposing them")
  void hashKey_doesNotExposeRawValue() {
    final String hashed = service.hashKey("institution|12345678");

    assertThat(hashed).doesNotContain("12345678");
    assertThat(hashed).isNotBlank();
    assertThat(hashed).isEqualTo(service.hashKey("institution|12345678"));
  }

  @Test
  @DisplayName("Should derive different keys for different secrets")
  void hashKey_dependsOnSecret() {
    final AuthRateLimitProperties otherProperties = Mockito.mock(AuthRateLimitProperties.class);
    lenient().when(otherProperties.keySecret()).thenReturn("another-secret");
    final AuthRateLimitService otherService =
        new AuthRateLimitService(redisTemplate, otherProperties);

    assertThat(service.hashKey("institution|12345678"))
        .isNotEqualTo(otherService.hashKey("institution|12345678"));
  }
}
