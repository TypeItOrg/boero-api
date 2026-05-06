package ar.edu.utn.frvm.typeit.boero_api.auth.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.RefreshToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  List<RefreshToken> findByFamilyId(String familyId);

  List<RefreshToken> findBySessionId(UUID sessionId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.familyId = :familyId")
  void revokeByFamilyId(String familyId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.sessionId = :sessionId")
  void revokeBySessionId(UUID sessionId);
}
