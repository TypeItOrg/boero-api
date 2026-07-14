package ar.edu.utn.frvm.typeit.boero_api.auth.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByPersonDocumentNumberAndInstitution_Id(
      String documentNumber, UUID institutionId);

  @EntityGraph(attributePaths = {"person", "institution"})
  Optional<User> findWithPersonAndInstitutionByPersonDocumentNumberAndInstitution_Id(
      String documentNumber, UUID institutionId);

  boolean existsByPersonDocumentNumberAndInstitution_Id(String documentNumber, UUID institutionId);

  Optional<User> findByPerson_IdAndInstitution_Id(UUID personId, UUID institutionId);

  @EntityGraph(attributePaths = {"person", "institution"})
  Optional<User> findWithPersonAndInstitutionById(UUID id);

  @Query(
      """
      select u.institution.id as institutionId, count(u) as userCount
      from User u
      where u.enabled = true and u.institution.id in :ids
      group by u.institution.id
      """)
  List<InstitutionUserCount> countEnabledUsersByInstitutionIdIn(@Param("ids") Collection<UUID> ids);

  @Query(
      """
      select count(u)
      from User u
      where u.enabled = true
        and u.person.deleted = false
        and u.institution.active = true
      """)
  long countUsersWithAccess();
}
