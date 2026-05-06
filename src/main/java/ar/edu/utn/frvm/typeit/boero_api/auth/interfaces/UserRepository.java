package ar.edu.utn.frvm.typeit.boero_api.auth.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByPersonDocumentNumberAndInstitution_Id(
      String documentNumber, UUID institutionId);

  boolean existsByPersonDocumentNumberAndInstitution_Id(String documentNumber, UUID institutionId);
}
