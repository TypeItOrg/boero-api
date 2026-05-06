package ar.edu.utn.frvm.typeit.boero_api.auth.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.UserSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
  List<UserSession> findByUserIdAndActive(UUID userId, boolean active);

  Page<UserSession> findByUserIdAndActive(UUID userId, boolean active, Pageable pageable);

  long countByUserIdAndActive(UUID userId, boolean active);

  Optional<UserSession> findByIdAndUserId(UUID id, UUID userId);
}
