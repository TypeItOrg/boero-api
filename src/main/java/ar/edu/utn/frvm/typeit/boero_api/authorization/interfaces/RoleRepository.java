package ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

  Optional<Role> findByScopeAndCodeAndInstitutionIsNull(RoleScope scope, String code);

  List<Role> findByScopeAndSystemTrueAndInstitutionIsNullOrderByNameAsc(RoleScope scope);

  Optional<Role> findByIdAndScopeAndInstitution_Id(UUID id, RoleScope scope, UUID institutionId);

  Optional<Role> findByScopeAndCodeAndInstitution_Id(
      RoleScope scope, String code, UUID institutionId);

  List<Role> findByScopeAndInstitution_IdOrderByNameAsc(RoleScope scope, UUID institutionId);

  boolean existsByScopeAndInstitution_IdAndNameIgnoreCase(
      RoleScope scope, UUID institutionId, String name);

  boolean existsByScopeAndInstitution_IdAndNameIgnoreCaseAndIdNot(
      RoleScope scope, UUID institutionId, String name, UUID id);
}
