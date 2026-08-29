package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.cache.AuthorizationCacheNames;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Permission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.RolePermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PermissionRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RolePermissionRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import jakarta.persistence.EntityManager;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.CacheManager;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class PermissionRoleSeed implements ApplicationRunner {

  private static final long SEED_LOCK_ID = 7_439_201_884L;
  private static final List<String> AUTHORITY_CACHE_NAMES =
      List.of(
          AuthorizationCacheNames.PERSON_AUTHORITIES, AuthorizationCacheNames.PLATFORM_AUTHORITIES);

  private static final Map<SystemRoleCode, Set<PermissionCode>> INSTITUTIONAL_ROLE_PERMISSIONS =
      Map.of(
          SystemRoleCode.INSTITUTIONAL_AUTHORITY,
          EnumSet.of(
              PermissionCode.INSTITUTION_ROLE_ASSIGN,
              PermissionCode.INSTITUTION_ROLE_REVOKE,
              PermissionCode.INSTITUTION_PERSON_READ_ANY,
              PermissionCode.INSTITUTION_PERSON_CREATE,
              PermissionCode.INSTITUTION_PERSON_UPDATE_ANY,
              PermissionCode.INSTITUTION_PERSON_DELETE,
              PermissionCode.INSTITUTION_READ,
              PermissionCode.INSTITUTION_UPDATE,
              PermissionCode.ACADEMIC_YEAR_CREATE,
              PermissionCode.ACADEMIC_YEAR_UPDATE,
              PermissionCode.ACADEMIC_YEAR_STATUS_UPDATE,
              PermissionCode.ACADEMIC_YEAR_DELETE,
              PermissionCode.ACADEMIC_YEAR_RESTORE,
              PermissionCode.TRAINING_PATH_CREATE,
              PermissionCode.TRAINING_PATH_UPDATE,
              PermissionCode.TRAINING_PATH_STATUS_UPDATE,
              PermissionCode.TRAINING_PATH_DELETE,
              PermissionCode.TRAINING_PATH_RESTORE,
              PermissionCode.STUDY_PLAN_CREATE,
              PermissionCode.STUDY_PLAN_UPDATE,
              PermissionCode.STUDY_PLAN_STATUS_UPDATE,
              PermissionCode.STUDY_PLAN_CURRICULUM_UPDATE,
              PermissionCode.STUDY_PLAN_DELETE,
              PermissionCode.STUDY_PLAN_RESTORE,
              PermissionCode.ACADEMIC_SPACE_CREATE,
              PermissionCode.ACADEMIC_SPACE_UPDATE,
              PermissionCode.ACADEMIC_SPACE_STATUS_UPDATE,
              PermissionCode.ACADEMIC_SPACE_DELETE,
              PermissionCode.ACADEMIC_SPACE_RESTORE,
              PermissionCode.INSTRUMENT_CREATE,
              PermissionCode.INSTRUMENT_UPDATE,
              PermissionCode.INSTRUMENT_STATUS_UPDATE,
              PermissionCode.INSTRUMENT_DELETE,
              PermissionCode.INSTRUMENT_RESTORE,
              PermissionCode.COURSE_READ,
              PermissionCode.COURSE_CREATE,
              PermissionCode.COURSE_UPDATE,
              PermissionCode.COURSE_STATUS_UPDATE,
              PermissionCode.COURSE_DELETE,
              PermissionCode.COURSE_RESTORE));

  private final PermissionRepository permissionRepository;
  private final RoleRepository roleRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;
  private final AssignPersonSystemRoleUseCase assignPersonSystemRoleUseCase;
  private final InstitutionRepository institutionRepository;
  private final InstitutionRoleProvisioner institutionRoleProvisioner;
  private final Environment environment;
  private final CacheManager cacheManager;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public void run(final ApplicationArguments args) {
    acquireSeedLock();
    final Map<PermissionCode, Permission> permissions = syncPermissions();
    syncInstitutionalRoles(permissions);
    syncPlatformRoles(permissions);
    provisionInstitutionRoles();
    if (shouldBackfillApplicants()) {
      backfillApplicantRoleForPersonsWithoutRoles();
    }
    clearAuthorityCaches();
  }

  private void provisionInstitutionRoles() {
    for (final var institution : institutionRepository.findAll()) {
      institutionRoleProvisioner.provision(institution);
    }
  }

  private void acquireSeedLock() {
    if (environment.acceptsProfiles(Profiles.of("test"))) {
      return;
    }
    entityManager
        .createNativeQuery("select pg_advisory_xact_lock(:lockId)")
        .setParameter("lockId", SEED_LOCK_ID)
        .getSingleResult();
  }

  private Map<PermissionCode, Permission> syncPermissions() {
    Map<String, Permission> byCode =
        permissionRepository.findAll().stream()
            .collect(
                Collectors.toMap(permission -> permission.getCode(), permission -> permission));

    Map<PermissionCode, Permission> synced = new HashMap<>();
    for (PermissionCode code : PermissionCode.values()) {
      Permission permission =
          byCode.computeIfAbsent(
              code.getCode(),
              ignored ->
                  permissionRepository.save(
                      Permission.builder()
                          .code(code.getCode())
                          .description(code.getDescription())
                          .scope(code.getScope())
                          .build()));
      synced.put(code, permission);
    }
    return synced;
  }

  private void syncInstitutionalRoles(Map<PermissionCode, Permission> permissions) {
    for (SystemRoleCode roleCode : SystemRoleCode.values()) {
      syncScopedRole(
          RoleScope.INSTITUTION,
          roleCode.name(),
          roleCode.getDisplayName(),
          INSTITUTIONAL_ROLE_PERMISSIONS.getOrDefault(roleCode, Set.of()),
          permissions);
    }
  }

  private void syncPlatformRoles(Map<PermissionCode, Permission> permissions) {
    for (PlatformRoleCode roleCode : PlatformRoleCode.values()) {
      syncScopedRole(
          RoleScope.PLATFORM, roleCode.name(), roleCode.getDisplayName(), Set.of(), permissions);
    }
  }

  private void syncScopedRole(
      RoleScope scope,
      String code,
      String displayName,
      Set<PermissionCode> permissionCodes,
      Map<PermissionCode, Permission> permissions) {
    Role role = upsertSystemRole(scope, code, displayName);
    syncRolePermissions(role, permissionCodes, permissions);
  }

  private Role upsertSystemRole(RoleScope scope, String code, String displayName) {
    Role role =
        roleRepository
            .findByScopeAndCodeAndInstitutionIsNull(scope, code)
            .orElseGet(
                () ->
                    roleRepository.save(
                        Role.builder()
                            .scope(scope)
                            .code(code)
                            .name(displayName)
                            .system(true)
                            .build()));
    if (!role.getName().equals(displayName)) {
      role.rename(displayName);
      roleRepository.save(role);
    }
    return role;
  }

  private void syncRolePermissions(
      Role role, Set<PermissionCode> permissionCodes, Map<PermissionCode, Permission> permissions) {
    final var requiredPermissionCodes = PermissionCode.withRequiredPermissions(permissionCodes);
    Set<UUID> desiredPermissionIds =
        requiredPermissionCodes.stream()
            .map(code -> permissions.get(code).getId())
            .collect(Collectors.toSet());

    for (final var rolePermission : rolePermissionRepository.findByRole_Id(role.getId())) {
      if (!desiredPermissionIds.contains(rolePermission.getPermission().getId())) {
        rolePermissionRepository.delete(rolePermission);
      }
    }

    for (PermissionCode permissionCode : requiredPermissionCodes) {
      Permission permission = permissions.get(permissionCode);
      if (!rolePermissionRepository.existsByRoleIdAndPermissionId(
          role.getId(), permission.getId())) {
        rolePermissionRepository.save(RolePermission.of(role, permission));
      }
    }
  }

  private boolean shouldBackfillApplicants() {
    return environment.acceptsProfiles(Profiles.of("dev", "test"));
  }

  private void backfillApplicantRoleForPersonsWithoutRoles() {
    List<Person> persons = personRoleAssignmentRepository.findPersonsWithoutRoleAssignments();
    for (Person person : persons) {
      assignPersonSystemRoleUseCase.execute(person, SystemRoleCode.APPLICANT);
    }
  }

  private void clearAuthorityCaches() {
    for (final String cacheName : AUTHORITY_CACHE_NAMES) {
      final var cache = cacheManager.getCache(cacheName);
      if (cache != null) {
        cache.clear();
      }
    }
  }
}
