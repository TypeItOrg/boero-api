package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

  private static final String KEY_PREFIX = "jwt:blacklist:";

  private final StringRedisTemplate redisTemplate;

  public void blacklist(String jti, Duration ttl) {
    if (ttl.isNegative() || ttl.isZero()) {
      return;
    }
    redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", ttl);
  }

  public boolean isBlacklisted(String jti) {
    Boolean has = redisTemplate.hasKey(KEY_PREFIX + jti);
    return Boolean.TRUE.equals(has);
  }
}
