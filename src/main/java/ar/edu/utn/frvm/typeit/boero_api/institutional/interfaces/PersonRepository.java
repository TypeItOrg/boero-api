package ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PersonRepository
    extends JpaRepository<Person, UUID>, JpaSpecificationExecutor<Person> {

  Optional<Person> findByIdAndInstitution_Id(UUID personId, UUID institutionId);

  Optional<Person> findByDocumentNumberAndInstitution_Id(String documentNumber, UUID institutionId);

  boolean existsByDocumentNumberAndInstitution_Id(String documentNumber, UUID institutionId);

  @EntityGraph(
      attributePaths = {
        "address",
        "address.city",
        "address.city.province",
        "birthCity",
        "birthCity.province",
        "nationalityCountry",
        "institution"
      })
  Optional<Person> findWithDetailsByIdAndInstitution_Id(UUID personId, UUID institutionId);
}
