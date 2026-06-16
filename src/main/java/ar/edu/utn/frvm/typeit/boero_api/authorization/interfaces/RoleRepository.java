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
}
