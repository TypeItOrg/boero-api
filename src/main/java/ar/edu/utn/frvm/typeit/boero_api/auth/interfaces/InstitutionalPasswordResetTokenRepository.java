package ar.edu.utn.frvm.typeit.boero_api.auth.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.InstitutionalPasswordResetToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstitutionalPasswordResetTokenRepository
    extends JpaRepository<InstitutionalPasswordResetToken, UUID> {

  void deleteByUser_Id(UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select token from InstitutionalPasswordResetToken token
      join fetch token.user user
      where token.tokenHash = :tokenHash
      """)
  Optional<InstitutionalPasswordResetToken> findByTokenHashForUpdate(
      @Param("tokenHash") String tokenHash);
}
