package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Permission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.RolePermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PermissionRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RolePermissionRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.payloads.InstitutionRoleRequest;
import ar.edu.utn.frvm.typeit.boero_api.authorization.payloads.InstitutionRoleResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InstitutionRoleManagementService {

  private static final Set<String> ROLE_MANAGEMENT_PERMISSIONS =
      Set.of(
          PermissionCode.INSTITUTION_ROLE_READ.getCode(),
          PermissionCode.INSTITUTION_ROLE_UPDATE.getCode());

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final PersonRoleAssignmentRepository assignmentRepository;
  private final InstitutionRepository institutionRepository;
  private final CacheManager cacheManager;

  @Transactional(readOnly = true)
  public List<InstitutionRoleResponse> list(UUID institutionId, boolean includeAuthority) {
    return roleRepository
        .findByScopeAndInstitution_IdOrderByNameAsc(RoleScope.INSTITUTION, institutionId)
        .stream()
        .filter(role -> includeAuthority || !isAuthority(role))
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public InstitutionRoleResponse get(
      UUID institutionId, UUID roleId, boolean includeAuthority, UUID actorPersonId) {
    Role role = requireRole(institutionId, roleId);
    if (!includeAuthority && isAuthority(role)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado.");
    }
    Set<String> permissions = permissionsFor(role);
    Set<String> protectedPermissions =
        new HashSet<>(requiredRolePermissionsForActor(role, institutionId, actorPersonId));
    protectedPermissions.retainAll(permissions);
    return toResponse(role, permissions, Set.copyOf(protectedPermissions));
  }

  @Transactional
  public InstitutionRoleResponse create(
      UUID institutionId, InstitutionRoleRequest request, Set<PermissionCode> actorPermissions) {
    String name = normalizedName(request.name());
    ensureUniqueName(institutionId, name, null);
    var institution =
        institutionRepository
            .findById(institutionId)
            .orElseThrow(InstitutionNotFoundException::new);
    Role role =
        roleRepository.save(
            Role.builder()
                .scope(RoleScope.INSTITUTION)
                .code("CUSTOM_" + UUID.randomUUID().toString().replace("-", ""))
                .name(name)
                .system(false)
                .institution(institution)
                .build());
    replacePermissions(role, request.permissions(), actorPermissions, false);
    return toResponse(role);
  }

  @Transactional
  public InstitutionRoleResponse update(
      UUID institutionId,
      UUID roleId,
      InstitutionRoleRequest request,
      UUID actorPersonId,
      Set<PermissionCode> actorPermissions) {
    Role role = requireRole(institutionId, roleId);
    if (isAuthority(role)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El rol de autoridad institucional no se puede modificar.");
    }
    String name = normalizedName(request.name());
    ensureUniqueName(institutionId, name, roleId);
    role.setName(name);
    ensureActorRetainsRoleManagementPermissions(
        role, institutionId, actorPersonId, request.permissions());
    replacePermissions(role, request.permissions(), actorPermissions, true);
    evictAffectedPermissionCaches(role, institutionId);
    return toResponse(role);
  }

  @Transactional
  public void delete(UUID institutionId, UUID roleId) {
    Role role = requireRole(institutionId, roleId);
    if (role.isSystem()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Los roles del sistema no se pueden eliminar.");
    }
    if (assignmentRepository.countByRole_Id(roleId) > 0) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El rol no se puede eliminar mientras tenga usuarios asignados.");
    }
    rolePermissionRepository.deleteAll(rolePermissionRepository.findByRole_Id(roleId));
    roleRepository.delete(role);
  }

  private void replacePermissions(
      Role role,
      Set<String> requestedCodes,
      Set<PermissionCode> actorPermissions,
      boolean preserveUnmanageable) {
    Set<String> grantableCodes =
        actorPermissions.stream()
            .filter(permission -> permission.getScope() == PermissionScope.INSTITUTION)
            .filter(PermissionCode::isConfigurable)
            .map(PermissionCode::getCode)
            .collect(Collectors.toSet());
    if (!grantableCodes.containsAll(requestedCodes)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "No podés delegar permisos que no poseés.");
    }

    List<RolePermission> existing = rolePermissionRepository.findByRole_Id(role.getId());
    Set<String> existingCodes =
        existing.stream()
            .map(rolePermission -> rolePermission.getPermission().getCode())
            .collect(Collectors.toSet());
    Set<String> desiredCodes =
        preserveUnmanageable
            ? existingCodes.stream()
                .filter(code -> !grantableCodes.contains(code))
                .collect(Collectors.toSet())
            : new HashSet<>();
    desiredCodes.addAll(requestedCodes);

    existing.stream()
        .filter(rolePermission -> !desiredCodes.contains(rolePermission.getPermission().getCode()))
        .forEach(rolePermissionRepository::delete);
    for (Permission permission : permissionRepository.findByCodeIn(List.copyOf(desiredCodes))) {
      if (!rolePermissionRepository.existsByRoleIdAndPermissionId(
          role.getId(), permission.getId())) {
        rolePermissionRepository.save(RolePermission.of(role, permission));
      }
    }
  }

  private void ensureActorRetainsRoleManagementPermissions(
      Role role, UUID institutionId, UUID actorPersonId, Set<String> requestedCodes) {
    Set<String> protectedPermissions =
        requiredRolePermissionsForActor(role, institutionId, actorPersonId);
    if (requestedCodes.containsAll(protectedPermissions)) return;

    throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "No podés quitarte los permisos necesarios para administrar roles institucionales.");
  }

  private Set<String> requiredRolePermissionsForActor(
      Role role, UUID institutionId, UUID actorPersonId) {
    if (!assignmentRepository.existsByPerson_IdAndRole_IdAndInstitution_Id(
        actorPersonId, role.getId(), institutionId)) {
      return Set.of();
    }

    List<UUID> otherRoleIds =
        assignmentRepository
            .findRoleIdsByPersonIdAndInstitutionId(actorPersonId, institutionId)
            .stream()
            .filter(roleId -> !roleId.equals(role.getId()))
            .toList();
    Set<String> otherRolePermissions =
        otherRoleIds.isEmpty()
            ? Set.of()
            : Set.copyOf(rolePermissionRepository.findPermissionCodesByRoleIds(otherRoleIds));
    Set<String> protectedPermissions = new HashSet<>(ROLE_MANAGEMENT_PERMISSIONS);
    protectedPermissions.removeAll(otherRolePermissions);
    return protectedPermissions;
  }

  private InstitutionRoleResponse toResponse(Role role) {
    return toResponse(role, permissionsFor(role), Set.of());
  }

  private InstitutionRoleResponse toResponse(
      Role role, Set<String> permissions, Set<String> protectedPermissions) {
    return InstitutionRoleResponse.from(
        role, assignmentRepository.countByRole_Id(role.getId()), permissions, protectedPermissions);
  }

  private Set<String> permissionsFor(Role role) {
    return rolePermissionRepository.findByRole_Id(role.getId()).stream()
        .map(rolePermission -> rolePermission.getPermission().getCode())
        .collect(Collectors.toUnmodifiableSet());
  }

  private Role requireRole(UUID institutionId, UUID roleId) {
    return roleRepository
        .findByIdAndScopeAndInstitution_Id(roleId, RoleScope.INSTITUTION, institutionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado."));
  }

  private void ensureUniqueName(UUID institutionId, String name, UUID excludedId) {
    boolean exists =
        excludedId == null
            ? roleRepository.existsByScopeAndInstitution_IdAndNameIgnoreCase(
                RoleScope.INSTITUTION, institutionId, name)
            : roleRepository.existsByScopeAndInstitution_IdAndNameIgnoreCaseAndIdNot(
                RoleScope.INSTITUTION, institutionId, name, excludedId);
    if (exists) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Ya existe un rol con ese nombre en la institución.");
    }
  }

  private String normalizedName(String name) {
    return name.trim().replaceAll("\\s+", " ");
  }

  private boolean isAuthority(Role role) {
    return role.isSystem() && role.getCode().equals(SystemRoleCode.INSTITUTIONAL_AUTHORITY.name());
  }

  private void evictAffectedPermissionCaches(Role role, UUID institutionId) {
    var cache = cacheManager.getCache("personPermissions");
    if (cache == null) return;
    assignmentRepository
        .findByRole_Id(role.getId())
        .forEach(assignment -> cache.evict(assignment.getPerson().getId() + "-" + institutionId));
  }
}
