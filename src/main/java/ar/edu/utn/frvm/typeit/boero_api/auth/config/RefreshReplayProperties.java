package ar.edu.utn.frvm.typeit.boero_api.auth.config;

import java.time.Duration;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.refresh-replay")
public record RefreshReplayProperties(String encryptionKey, Duration ttl) {

  private static final int KEY_SIZE_BYTES = 32;

  public RefreshReplayProperties {
    if (encryptionKey == null || encryptionKey.isBlank()) {
      throw new IllegalArgumentException("app.auth.refresh-replay.encryption-key is required");
    }
    if (ttl == null || ttl.isZero() || ttl.isNegative()) {
      throw new IllegalArgumentException("app.auth.refresh-replay.ttl must be positive");
    }
    decodeKey(encryptionKey);
  }

  public SecretKey secretKey() {
    return new SecretKeySpec(decodeKey(encryptionKey), "AES");
  }

  private static byte[] decodeKey(final String encodedKey) {
    final byte[] decodedKey;
    try {
      decodedKey = Base64.getDecoder().decode(encodedKey);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(
          "app.auth.refresh-replay.encryption-key must be valid Base64", exception);
    }

    if (decodedKey.length != KEY_SIZE_BYTES) {
      throw new IllegalArgumentException(
          "app.auth.refresh-replay.encryption-key must decode to 32 bytes");
    }

    return decodedKey;
  }
}
