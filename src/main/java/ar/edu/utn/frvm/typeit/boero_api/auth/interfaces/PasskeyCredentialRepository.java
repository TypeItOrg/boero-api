package ar.edu.utn.frvm.typeit.boero_api.auth.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.PasskeyCredential;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, UUID> {

  @Query(
      """
      SELECT credential FROM PasskeyCredential credential
      WHERE credential.user.id = :userId
        AND credential.revokedAt IS NULL
      ORDER BY credential.createdAt ASC
      """)
  List<PasskeyCredential> findActiveByUserId(@Param("userId") UUID userId);

  @Query(
      """
      SELECT COUNT(credential) FROM PasskeyCredential credential
      WHERE credential.user.id = :userId
        AND credential.revokedAt IS NULL
      """)
  long countActiveByUserId(@Param("userId") UUID userId);

  @Query(
      """
      SELECT CASE WHEN COUNT(credential) > 0 THEN true ELSE false END
      FROM PasskeyCredential credential
      WHERE credential.user.id = :userId
        AND credential.revokedAt IS NULL
      """)
  boolean existsActiveByUserId(@Param("userId") UUID userId);

  Optional<PasskeyCredential> findByIdAndUserId(UUID id, UUID userId);

  Optional<PasskeyCredential> findByCredentialId(String credentialId);
}
