package ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PersonRoleAssignment;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonRoleAssignmentRepository extends JpaRepository<PersonRoleAssignment, UUID> {

  boolean existsByPerson_IdAndRole_IdAndInstitution_Id(
      UUID personId, UUID roleId, UUID institutionId);

  Optional<PersonRoleAssignment> findByPerson_IdAndRole_IdAndInstitution_Id(
      UUID personId, UUID roleId, UUID institutionId);

  @EntityGraph(attributePaths = "role")
  List<PersonRoleAssignment> findByPerson_IdAndInstitution_Id(UUID personId, UUID institutionId);

  long countByInstitution_IdAndRole_Code(UUID institutionId, String roleCode);

  @Query(
      """
      SELECT pra.role.id
      FROM PersonRoleAssignment pra
      WHERE pra.person.id = :personId
      AND pra.institution.id = :institutionId
      """)
  List<UUID> findRoleIdsByPersonIdAndInstitutionId(
      @Param("personId") UUID personId, @Param("institutionId") UUID institutionId);

  @Query(
      """
      SELECT p FROM Person p
      LEFT JOIN FETCH p.institution
      WHERE NOT EXISTS (
        SELECT 1 FROM PersonRoleAssignment pra WHERE pra.person = p
      )
      """)
  List<Person> findPersonsWithoutRoleAssignments();
}
