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

  long countByDeletedFalse();

  Page<Person> findByInstitution_IdAndDeletedFalse(UUID institutionId, Pageable pageable);

  @Query(
      """
      SELECT p FROM Person p
      WHERE p.institution.id = :institutionId
        AND p.deleted = false
        AND (
          :roleId IS NULL
          OR EXISTS (
            SELECT assignment.id FROM PersonRoleAssignment assignment
            WHERE assignment.person = p
              AND assignment.institution.id = :institutionId
              AND assignment.role.id = :roleId
          )
        )
        AND (
          :search IS NULL
          OR (
            UNACCENT_LOWER(CONCAT(p.firstName, ' ', p.lastName))
                LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%'))
            OR p.documentNumber LIKE CONCAT('%', CAST(:search AS string), '%')
          )
        )
      """)
  Page<Person> search(
      @Param("institutionId") UUID institutionId,
      @Param("search") String search,
      @Param("roleId") UUID roleId,
      Pageable pageable);

  @EntityGraph(attributePaths = "institution")
  @Query(
      """
      SELECT person FROM Person person
      WHERE person.deleted = false
        AND (:institutionId IS NULL OR person.institution.id = :institutionId)
        AND (
          :roleCode IS NULL
          OR EXISTS (
            SELECT assignment.id FROM PersonRoleAssignment assignment
            WHERE assignment.person = person
              AND assignment.role.code = :roleCode
          )
        )
        AND (
          :search IS NULL
          OR UNACCENT_LOWER(CONCAT(person.firstName, ' ', person.lastName))
              LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR person.documentNumber LIKE CONCAT('%', CAST(:search AS string), '%')
          OR UNACCENT_LOWER(COALESCE(person.email, ''))
              LIKE UNACCENT_LOWER(CONCAT('%', CAST(:search AS string), '%'))
        )
      """)
  Page<Person> findPlatformPeople(
      @Param("search") String search,
      @Param("institutionId") UUID institutionId,
      @Param("roleCode") String roleCode,
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
