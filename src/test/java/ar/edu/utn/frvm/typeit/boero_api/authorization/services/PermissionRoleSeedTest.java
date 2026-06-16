package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PermissionRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.support.JpaAuditingTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
  JpaAuditingTestConfig.class,
  PermissionRoleSeed.class,
  AssignPersonSystemRoleUseCase.class,
  SessionRevocationService.class
})
class PermissionRoleSeedTest {

  @Autowired private PermissionRoleSeed permissionRoleSeed;
  @Autowired private PermissionRepository permissionRepository;
  @Autowired private RoleRepository roleRepository;

  @Test
  @DisplayName("Should seed permissions and system roles idempotently")
  void run_createsCatalog() {
    permissionRoleSeed.run(null);
    permissionRoleSeed.run(null);

    assertThat(permissionRepository.count()).isEqualTo(PermissionCode.values().length);
    assertThat(
            roleRepository.findByScopeAndCodeAndInstitutionIsNull(
                RoleScope.INSTITUTION, SystemRoleCode.APPLICANT.name()))
        .isPresent();
    assertThat(
            roleRepository.findByScopeAndCodeAndInstitutionIsNull(
                RoleScope.PLATFORM, PlatformRoleCode.PLATFORM_ADMIN.name()))
        .isPresent();
  }
}
