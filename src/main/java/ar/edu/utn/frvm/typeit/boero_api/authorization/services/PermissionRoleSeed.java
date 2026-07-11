package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

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
      List.of("personPermissions", "platformAccountPermissions", "platformAccountRoles");

  private static final Map<SystemRoleCode, Set<PermissionCode>> INSTITUTIONAL_ROLE_PERMISSIONS =
      Map.of(
          SystemRoleCode.APPLICANT,
          EnumSet.of(
              PermissionCode.INSTITUTION_PERSON_READ_OWN,
              PermissionCode.INSTITUTION_PERSON_UPDATE_OWN),
          SystemRoleCode.INSTITUTIONAL_AUTHORITY,
          EnumSet.of(
              PermissionCode.INSTITUTION_ROLE_ASSIGN,
              PermissionCode.INSTITUTION_ROLE_REVOKE,
              PermissionCode.INSTITUTION_PERSON_READ_ANY,
              PermissionCode.INSTITUTION_PERSON_CREATE,
              PermissionCode.INSTITUTION_PERSON_UPDATE_ANY,
              PermissionCode.INSTITUTION_PERSON_DELETE));

  private final PermissionRepository permissionRepository;
  private final RoleRepository roleRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;
  private final AssignPersonSystemRoleUseCase assignPersonSystemRoleUseCase;
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
    if (shouldBackfillApplicants()) {
      backfillApplicantRoleForPersonsWithoutRoles();
    }
    clearAuthorityCaches();
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
            .collect(Collectors.toMap(Permission::getCode, permission -> permission));

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
    return roleRepository
        .findByScopeAndCodeAndInstitutionIsNull(scope, code)
        .orElseGet(
            () ->
                roleRepository.save(
                    Role.builder().scope(scope).code(code).name(displayName).system(true).build()));
  }

  private void syncRolePermissions(
      Role role, Set<PermissionCode> permissionCodes, Map<PermissionCode, Permission> permissions) {
    Set<UUID> desiredPermissionIds =
        permissionCodes.stream()
            .map(code -> permissions.get(code).getId())
            .collect(Collectors.toSet());

    rolePermissionRepository.findByRole_Id(role.getId()).stream()
        .filter(
            rolePermission ->
                !desiredPermissionIds.contains(rolePermission.getPermission().getId()))
        .forEach(rolePermissionRepository::delete);

    for (PermissionCode permissionCode : permissionCodes) {
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
