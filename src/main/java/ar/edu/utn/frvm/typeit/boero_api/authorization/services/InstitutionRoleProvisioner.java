package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Permission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.RolePermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PermissionRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RolePermissionRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InstitutionRoleProvisioner {

  private static final Map<SystemRoleCode, Set<PermissionCode>> DEFAULT_PERMISSIONS =
      Map.of(
          SystemRoleCode.APPLICANT,
          Set.of(),
          SystemRoleCode.STUDENT,
          Set.of(),
          SystemRoleCode.INSTITUTIONAL_AUTHORITY,
          EnumSet.allOf(PermissionCode.class));

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final RolePermissionRepository rolePermissionRepository;

  @Transactional
  public void provision(Institution institution) {
    for (SystemRoleCode code :
        Set.of(
            SystemRoleCode.APPLICANT,
            SystemRoleCode.STUDENT,
            SystemRoleCode.INSTITUTIONAL_AUTHORITY)) {
      var existingRole =
          roleRepository.findByScopeAndCodeAndInstitution_Id(
              RoleScope.INSTITUTION, code.name(), institution.getId());
      Role role =
          existingRole.orElseGet(
              () ->
                  roleRepository.save(
                      Role.builder()
                          .scope(RoleScope.INSTITUTION)
                          .code(code.name())
                          .name(code.getDisplayName())
                          .system(true)
                          .institution(institution)
                          .build()));
      if (!role.getName().equals(code.getDisplayName())) {
        role.rename(code.getDisplayName());
        roleRepository.save(role);
      }
      if (existingRole.isEmpty() || code == SystemRoleCode.INSTITUTIONAL_AUTHORITY) {
        assignDefaults(role, DEFAULT_PERMISSIONS.get(code));
      }
    }
  }

  private void assignDefaults(Role role, Set<PermissionCode> codes) {
    codes = PermissionCode.withRequiredPermissions(codes);
    for (PermissionCode code : codes) {
      Permission permission =
          permissionRepository
              .findByCode(code.getCode())
              .orElseThrow(() -> new IllegalStateException("Permission not seeded: " + code));
      if (!rolePermissionRepository.existsByRoleIdAndPermissionId(
          role.getId(), permission.getId())) {
        rolePermissionRepository.save(RolePermission.of(role, permission));
      }
    }
  }
}
