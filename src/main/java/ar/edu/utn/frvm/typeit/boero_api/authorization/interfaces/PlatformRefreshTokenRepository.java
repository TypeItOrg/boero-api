package ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PlatformRefreshToken;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatformRefreshTokenRepository extends JpaRepository<PlatformRefreshToken, UUID> {

  Optional<PlatformRefreshToken> findByTokenHash(String tokenHash);

  List<PlatformRefreshToken> findByFamilyId(String familyId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE PlatformRefreshToken rt SET rt.revoked = true WHERE rt.familyId = :familyId")
  void revokeByFamilyId(String familyId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "UPDATE PlatformRefreshToken rt SET rt.revoked = true WHERE rt.platformSessionId = :sessionId")
  void revokeByPlatformSessionId(UUID sessionId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      UPDATE PlatformRefreshToken rt
      SET rt.revoked = true
      WHERE rt.platformSessionId IN :sessionIds
      """)
  void revokeByPlatformSessionIds(@Param("sessionIds") Collection<UUID> sessionIds);
}
