package ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, UUID> {
  Optional<Person> findByDocumentNumberAndInstitution_Id(String documentNumber, UUID institutionId);

  boolean existsByDocumentNumberAndInstitution_Id(String documentNumber, UUID institutionId);
}
