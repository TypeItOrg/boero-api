package ar.edu.utn.frvm.typeit.boero_api.auth.services;

import java.time.Duration;
import java.time.Instant;

final class TokenBlacklistTtl {

  private static final Duration MIN_TTL = Duration.ofMinutes(1);

  private TokenBlacklistTtl() {}

  static Duration remaining(Instant expiration) {
    Duration ttl = Duration.between(Instant.now(), expiration);
    return ttl.isNegative() || ttl.isZero() ? MIN_TTL : ttl;
  }
}
