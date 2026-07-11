package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PermissionRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RolePermissionRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.support.JpaAuditingTestConfig;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@Import({
  JpaAuditingTestConfig.class,
  PermissionRoleSeed.class,
  AssignPersonSystemRoleUseCase.class,
  SessionRevocationService.class
})
class PermissionRoleSeedTest {

  @MockitoBean private org.springframework.cache.CacheManager cacheManager;

  @Autowired private PermissionRoleSeed permissionRoleSeed;
  @Autowired private PermissionRepository permissionRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private RolePermissionRepository rolePermissionRepository;

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

  @Test
  @DisplayName("Should assign new people management permissions to institutional authority")
  void run_assignsNewPeoplePermissionsToAuthority() {
    permissionRoleSeed.run(null);

    var authorityRole =
        roleRepository
            .findByScopeAndCodeAndInstitutionIsNull(
                RoleScope.INSTITUTION, SystemRoleCode.INSTITUTIONAL_AUTHORITY.name())
            .orElseThrow();
    Set<String> permissionCodes =
        rolePermissionRepository.findByRole_Id(authorityRole.getId()).stream()
            .map(rp -> rp.getPermission().getCode())
            .collect(Collectors.toSet());

    assertThat(permissionCodes)
        .contains(
            PermissionCode.INSTITUTION_PERSON_READ_ANY.getCode(),
            PermissionCode.INSTITUTION_PERSON_CREATE.getCode(),
            PermissionCode.INSTITUTION_PERSON_UPDATE_ANY.getCode(),
            PermissionCode.INSTITUTION_PERSON_DELETE.getCode());
  }
}
