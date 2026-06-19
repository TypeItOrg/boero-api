package ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonRepository extends JpaRepository<Person, UUID> {

  Optional<Person> findByIdAndInstitution_Id(UUID personId, UUID institutionId);

  Optional<Person> findByDocumentNumberAndInstitution_Id(String documentNumber, UUID institutionId);

  boolean existsByDocumentNumberAndInstitution_Id(String documentNumber, UUID institutionId);

  Page<Person> findByInstitution_IdAndDeletedFalse(UUID institutionId, Pageable pageable);

  @Query(
      """
      SELECT p FROM Person p
      WHERE p.institution.id = :institutionId
        AND p.deleted = false
        AND (
              UNACCENT_LOWER(CONCAT(p.firstName, ' ', p.lastName))
          LIKE UNACCENT_LOWER(CONCAT('%', :search, '%'))
          OR p.documentNumber LIKE CONCAT('%', :search, '%')
        )
      """)
  Page<Person> search(
      @Param("institutionId") UUID institutionId,
      @Param("search") String search,
      Pageable pageable);

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
