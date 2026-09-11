package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import static ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.AuthMessages.HMAC_SHA_256_UNAVAILABLE;

import ar.edu.utn.frvm.typeit.boero_api.auth.config.AuthRateLimitProperties;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.RateLimitExceededException;
import ar.edu.utn.frvm.typeit.boero_api.auth.exceptions.RateLimitUnavailableException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthRateLimitService {

  private static final String KEY_PREFIX = "boero:auth:rate:";
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL =
      new DefaultRedisScript<>(
          "local count = redis.call('INCR', KEYS[1]) "
              + "if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end "
              + "return count",
          Long.class);

  private final StringRedisTemplate redisTemplate;
  private final AuthRateLimitProperties rateLimitProperties;

  public void checkAllowed(
      final String scope, final String key, final int maxAttempts, final Duration window) {
    final Long count = increment(KEY_PREFIX + scope + ":" + key, window, scope);
    if (count > maxAttempts) {
      log.info("[Auth] Rate limit exceeded, scope: {}", scope);
      throw new RateLimitExceededException();
    }
  }

  public String hashKey(final String value) {
    try {
      final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(
          new SecretKeySpec(
              rateLimitProperties.keySecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (final NoSuchAlgorithmException | InvalidKeyException exception) {
      throw new IllegalStateException(HMAC_SHA_256_UNAVAILABLE, exception);
    }
  }

  public static String normalizeDocument(final String documentNumber) {
    return documentNumber == null ? "" : documentNumber.trim();
  }

  private Long increment(final String redisKey, final Duration window, final String scope) {
    final Long count;
    try {
      count =
          redisTemplate.execute(
              INCREMENT_WITH_TTL, List.of(redisKey), String.valueOf(window.toMillis()));
    } catch (final RuntimeException exception) {
      log.warn(
          "[Auth] Rate limiter unavailable, failing closed, scope: {}, cause: {}",
          scope,
          exception.getClass().getSimpleName());
      throw new RateLimitUnavailableException();
    }
    if (count == null) {
      log.warn("[Auth] Rate limiter returned no count, failing closed, scope: {}", scope);
      throw new RateLimitUnavailableException();
    }
    return count;
  }
}
