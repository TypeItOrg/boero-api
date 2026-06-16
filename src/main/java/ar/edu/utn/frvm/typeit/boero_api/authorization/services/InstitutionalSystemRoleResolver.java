package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.RoleNotAssignableException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InstitutionalSystemRoleResolver {

  private final RoleRepository roleRepository;

  public Role requireInstitutionalSystemRole(SystemRoleCode roleCode) {
    Role role =
        roleRepository
            .findByScopeAndCodeAndInstitutionIsNull(RoleScope.INSTITUTION, roleCode.name())
            .orElseThrow(RoleNotAssignableException::new);

    if (role.getScope() != RoleScope.INSTITUTION || !role.isSystem()) {
      throw new RoleNotAssignableException();
    }

    return role;
  }
}
