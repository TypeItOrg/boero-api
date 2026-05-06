package ar.edu.utn.frvm.typeit.boero_api.auth.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.RefreshToken;
import ar.edu.utn.frvm.typeit.boero_api.support.JpaAuditingTestConfig;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingTestConfig.class)
class RefreshTokenRepositoryTest {

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Test
  @DisplayName("Should find refresh token by hash, family and session")
  void shouldFindRefreshTokenByHashFamilyAndSession() {
    UUID sessionId = UUID.randomUUID();
    RefreshToken token = refreshToken("hash-1", "family-1", sessionId);
    refreshTokenRepository.saveAndFlush(token);

    assertThat(refreshTokenRepository.findByTokenHash("hash-1")).contains(token);
    assertThat(refreshTokenRepository.findByFamilyId("family-1")).containsExactly(token);
    assertThat(refreshTokenRepository.findBySessionId(sessionId)).containsExactly(token);
  }

  @Test
  @DisplayName("Should revoke only tokens from selected family")
  void shouldRevokeOnlyTokensFromSelectedFamily() {
    RefreshToken first = refreshToken("hash-1", "family-1", UUID.randomUUID());
    RefreshToken second = refreshToken("hash-2", "family-1", UUID.randomUUID());
    RefreshToken other = refreshToken("hash-3", "family-2", UUID.randomUUID());
    refreshTokenRepository.save(first);
    refreshTokenRepository.save(second);
    refreshTokenRepository.saveAndFlush(other);

    refreshTokenRepository.revokeByFamilyId("family-1");

    assertThat(refreshTokenRepository.findByFamilyId("family-1"))
        .extracting(RefreshToken::isRevoked)
        .containsOnly(true);
    assertThat(refreshTokenRepository.findByTokenHash("hash-3")).get().returns(false, RefreshToken::isRevoked);
  }

  @Test
  @DisplayName("Should revoke only tokens from selected session")
  void shouldRevokeOnlyTokensFromSelectedSession() {
    UUID sessionId = UUID.randomUUID();
    RefreshToken first = refreshToken("hash-1", "family-1", sessionId);
    RefreshToken second = refreshToken("hash-2", "family-2", sessionId);
    RefreshToken other = refreshToken("hash-3", "family-3", UUID.randomUUID());
    refreshTokenRepository.save(first);
    refreshTokenRepository.save(second);
    refreshTokenRepository.saveAndFlush(other);

    refreshTokenRepository.revokeBySessionId(sessionId);

    assertThat(refreshTokenRepository.findBySessionId(sessionId))
        .extracting(RefreshToken::isRevoked)
        .containsOnly(true);
    assertThat(refreshTokenRepository.findByTokenHash("hash-3")).get().returns(false, RefreshToken::isRevoked);
  }

  private static RefreshToken refreshToken(String hash, String familyId, UUID sessionId) {
    return RefreshToken.builder()
        .sessionId(sessionId)
        .tokenHash(hash)
        .familyId(familyId)
        .expiresAt(LocalDateTime.now().plusDays(7))
        .build();
  }
}
