package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frvm.typeit.boero_api.auth.services.SessionRevocationService;
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
  InstitutionRoleProvisioner.class,
  AssignPersonSystemRoleUseCase.class,
  AuthorizationCacheInvalidator.class,
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
  @DisplayName("Should assign people and granular academic permissions to institutional authority")
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
            PermissionCode.INSTITUTION_PERSON_DELETE.getCode(),
            PermissionCode.ACADEMIC_YEAR_CREATE.getCode(),
            PermissionCode.ACADEMIC_YEAR_UPDATE.getCode(),
            PermissionCode.ACADEMIC_YEAR_STATUS_UPDATE.getCode(),
            PermissionCode.ACADEMIC_YEAR_READ.getCode(),
            PermissionCode.TRAINING_PATH_CREATE.getCode(),
            PermissionCode.TRAINING_PATH_UPDATE.getCode(),
            PermissionCode.TRAINING_PATH_STATUS_UPDATE.getCode(),
            PermissionCode.TRAINING_PATH_READ.getCode(),
            PermissionCode.STUDY_PLAN_CREATE.getCode(),
            PermissionCode.STUDY_PLAN_UPDATE.getCode(),
            PermissionCode.STUDY_PLAN_STATUS_UPDATE.getCode(),
            PermissionCode.STUDY_PLAN_CURRICULUM_UPDATE.getCode(),
            PermissionCode.STUDY_PLAN_READ.getCode(),
            PermissionCode.ACADEMIC_SPACE_CREATE.getCode(),
            PermissionCode.ACADEMIC_SPACE_UPDATE.getCode(),
            PermissionCode.ACADEMIC_SPACE_STATUS_UPDATE.getCode(),
            PermissionCode.ACADEMIC_SPACE_READ.getCode(),
            PermissionCode.INSTRUMENT_CREATE.getCode(),
            PermissionCode.INSTRUMENT_UPDATE.getCode(),
            PermissionCode.INSTRUMENT_STATUS_UPDATE.getCode(),
            PermissionCode.INSTRUMENT_READ.getCode(),
            PermissionCode.SHIFT_CREATE.getCode(),
            PermissionCode.SHIFT_UPDATE.getCode(),
            PermissionCode.SHIFT_STATUS_UPDATE.getCode(),
            PermissionCode.SHIFT_READ.getCode());
  }
}
