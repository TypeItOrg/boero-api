package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Permission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.RolePermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.DuplicateRoleNameException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.InstitutionInactiveForRoleManagementException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.InstitutionalAuthorityRoleImmutableException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.PermissionDelegationNotAllowedException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.RoleManagementSelfLockoutException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.RoleNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.RoleWithAssignmentsException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.SystemRoleNotDeletableException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PermissionRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RolePermissionRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.payloads.InstitutionRoleRequest;
import ar.edu.utn.frvm.typeit.boero_api.authorization.payloads.InstitutionRoleResponse;
import ar.edu.utn.frvm.typeit.boero_api.authorization.payloads.PlatformRoleResponse;
import ar.edu.utn.frvm.typeit.boero_api.common.search.SearchNormalization;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InstitutionRoleManagementService {

  private static final Set<String> ROLE_MANAGEMENT_PERMISSIONS =
      Set.of(
          PermissionCode.INSTITUTION_ROLE_READ.getCode(),
          PermissionCode.INSTITUTION_ROLE_UPDATE.getCode());

  private static final Set<PermissionCode> PLATFORM_ADMIN_PERMISSIONS =
      Arrays.stream(PermissionCode.values())
          .filter(permission -> permission.getScope() == PermissionScope.INSTITUTION)
          .filter(PermissionCode::isConfigurable)
          .collect(Collectors.toUnmodifiableSet());

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final PersonRoleAssignmentRepository assignmentRepository;
  private final InstitutionRepository institutionRepository;
  private final AuthorizationCacheInvalidator authorizationCacheInvalidator;

  @Transactional(readOnly = true)
  public List<InstitutionRoleResponse> list(UUID institutionId, boolean includeAuthority) {
    List<Role> roles =
        roleRepository
            .findByScopeAndInstitution_IdOrderByNameAsc(RoleScope.INSTITUTION, institutionId)
            .stream()
            .filter(role -> includeAuthority || !isAuthority(role))
            .toList();
    return toResponses(roles);
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<InstitutionRoleResponse> list(
      UUID institutionId, String search, Pageable pageable) {
    final var page =
        roleRepository.findInstitutionRoles(
            RoleScope.INSTITUTION,
            institutionId,
            SearchNormalization.normalizeSearch(search),
            pageable);
    final List<InstitutionRoleResponse> items = toResponses(page.getContent());
    return new PaginatedResponse<>(
        items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }

  @Transactional(readOnly = true)
  public PlatformRoleResponse getAsPlatformAdmin(UUID roleId) {
    Role role =
        roleRepository
            .findByIdAndScope(roleId, RoleScope.INSTITUTION)
            .orElseThrow(RoleNotFoundException::new);
    return PlatformRoleResponse.from(
        role, assignmentRepository.countByRole_Id(roleId), permissionsFor(role), Set.of());
  }

  @Transactional(readOnly = true)
  public InstitutionRoleResponse get(
      UUID institutionId, UUID roleId, boolean includeAuthority, UUID actorPersonId) {
    Role role = requireRole(institutionId, roleId);
    if (!includeAuthority && isAuthority(role)) {
      throw new RoleNotFoundException();
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
            Role.customInstitutional(
                "CUSTOM_" + UUID.randomUUID().toString().replace("-", ""), name, institution));
    replacePermissions(role, request.permissions(), actorPermissions, false);
    return toResponse(role);
  }

  @Transactional
  public InstitutionRoleResponse createAsPlatformAdmin(
      UUID institutionId, InstitutionRoleRequest request) {
    ensureActiveInstitution(institutionId);
    return create(institutionId, request, PLATFORM_ADMIN_PERMISSIONS);
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
      throw new InstitutionalAuthorityRoleImmutableException();
    }
    String name = normalizedName(request.name());
    ensureUniqueName(institutionId, name, roleId);
    role.rename(name);
    ensureActorRetainsRoleManagementPermissions(
        role, institutionId, actorPersonId, request.permissions());
    replacePermissions(role, request.permissions(), actorPermissions, true);
    authorizationCacheInvalidator.evictPeopleForRole(role.getId(), institutionId);
    return toResponse(role);
  }

  @Transactional
  public InstitutionRoleResponse updateAsPlatformAdmin(
      UUID institutionId, UUID roleId, InstitutionRoleRequest request) {
    Role role = requireRole(institutionId, roleId);
    ensureActiveInstitution(role);
    if (isAuthority(role)) {
      throw new InstitutionalAuthorityRoleImmutableException();
    }
    String name = normalizedName(request.name());
    ensureUniqueName(institutionId, name, roleId);
    role.rename(name);
    replacePermissions(role, request.permissions(), PLATFORM_ADMIN_PERMISSIONS, true);
    authorizationCacheInvalidator.evictPeopleForRole(role.getId(), institutionId);
    return toResponse(role);
  }

  @Transactional
  public void delete(UUID institutionId, UUID roleId) {
    Role role = requireRole(institutionId, roleId);
    if (role.isSystem()) {
      throw new SystemRoleNotDeletableException();
    }
    if (assignmentRepository.countByRole_Id(roleId) > 0) {
      throw new RoleWithAssignmentsException();
    }
    rolePermissionRepository.deleteAll(rolePermissionRepository.findByRole_Id(roleId));
    roleRepository.delete(role);
  }

  @Transactional
  public void deleteAsPlatformAdmin(UUID institutionId, UUID roleId) {
    Role role = requireRole(institutionId, roleId);
    ensureActiveInstitution(role);
    delete(institutionId, roleId);
  }

  private void replacePermissions(
      Role role,
      Set<String> requestedCodes,
      Set<PermissionCode> actorPermissions,
      boolean preserveUnmanageable) {
    Set<String> expandedRequestedCodes = expandPermissionCodes(requestedCodes);
    Set<String> grantableCodes =
        actorPermissions.stream()
            .filter(permission -> permission.getScope() == PermissionScope.INSTITUTION)
            .filter(PermissionCode::isConfigurable)
            .map(PermissionCode::getCode)
            .collect(Collectors.toSet());
    if (!grantableCodes.containsAll(expandedRequestedCodes)) {
      throw new PermissionDelegationNotAllowedException();
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
    desiredCodes.addAll(expandedRequestedCodes);

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

  private Set<String> expandPermissionCodes(Set<String> permissionCodes) {
    Set<PermissionCode> requestedPermissions =
        permissionCodes.stream().map(PermissionCode::fromCode).collect(Collectors.toSet());
    return PermissionCode.withRequiredPermissions(requestedPermissions).stream()
        .map(PermissionCode::getCode)
        .collect(Collectors.toUnmodifiableSet());
  }

  private void ensureActorRetainsRoleManagementPermissions(
      Role role, UUID institutionId, UUID actorPersonId, Set<String> requestedCodes) {
    Set<String> protectedPermissions =
        requiredRolePermissionsForActor(role, institutionId, actorPersonId);
    if (expandPermissionCodes(requestedCodes).containsAll(protectedPermissions)) return;

    throw new RoleManagementSelfLockoutException();
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

  private List<InstitutionRoleResponse> toResponses(final List<Role> roles) {
    if (roles.isEmpty()) return List.of();

    final List<UUID> roleIds = roles.stream().map(Role::getId).toList();
    final Map<UUID, Long> assignmentCounts =
        assignmentRepository.countByRoleIds(roleIds).stream()
            .collect(
                Collectors.toMap(
                    PersonRoleAssignmentRepository.RoleAssignmentCount::getRoleId,
                    PersonRoleAssignmentRepository.RoleAssignmentCount::getAssignmentCount));
    final Map<UUID, Set<String>> permissionsByRole =
        rolePermissionRepository.findByRole_IdIn(roleIds).stream()
            .collect(
                Collectors.groupingBy(
                    rolePermission -> rolePermission.getRole().getId(),
                    Collectors.mapping(
                        rolePermission -> rolePermission.getPermission().getCode(),
                        Collectors.toUnmodifiableSet())));
    return roles.stream()
        .map(
            role ->
                toResponse(
                    role,
                    permissionsByRole.getOrDefault(role.getId(), Set.of()),
                    Set.of(),
                    assignmentCounts.getOrDefault(role.getId(), 0L)))
        .toList();
  }

  private InstitutionRoleResponse toResponse(
      Role role, Set<String> permissions, Set<String> protectedPermissions) {
    return InstitutionRoleResponse.from(
        role, assignmentRepository.countByRole_Id(role.getId()), permissions, protectedPermissions);
  }

  private InstitutionRoleResponse toResponse(
      Role role, Set<String> permissions, Set<String> protectedPermissions, long assignmentCount) {
    return InstitutionRoleResponse.from(role, assignmentCount, permissions, protectedPermissions);
  }

  private Set<String> permissionsFor(Role role) {
    return rolePermissionRepository.findByRole_Id(role.getId()).stream()
        .map(rolePermission -> rolePermission.getPermission().getCode())
        .collect(Collectors.toUnmodifiableSet());
  }

  private Role requireRole(UUID institutionId, UUID roleId) {
    return roleRepository
        .findByIdAndScopeAndInstitution_Id(roleId, RoleScope.INSTITUTION, institutionId)
        .orElseThrow(RoleNotFoundException::new);
  }

  private void ensureActiveInstitution(UUID institutionId) {
    Institution institution =
        institutionRepository
            .findById(institutionId)
            .orElseThrow(InstitutionNotFoundException::new);
    if (!institution.isActive()) {
      throw new InstitutionInactiveForRoleManagementException();
    }
  }

  private void ensureActiveInstitution(Role role) {
    if (!role.getInstitution().isActive()) {
      throw new InstitutionInactiveForRoleManagementException();
    }
  }

  private void ensureUniqueName(UUID institutionId, String name, UUID excludedId) {
    boolean exists =
        excludedId == null
            ? roleRepository.existsByScopeAndInstitution_IdAndNameIgnoreCase(
                RoleScope.INSTITUTION, institutionId, name)
            : roleRepository.existsByScopeAndInstitution_IdAndNameIgnoreCaseAndIdNot(
                RoleScope.INSTITUTION, institutionId, name, excludedId);
    if (exists) {
      throw new DuplicateRoleNameException();
    }
  }

  private String normalizedName(String name) {
    return name.trim().replaceAll("\\s+", " ");
  }

  private boolean isAuthority(Role role) {
    return role.isInstitutionalAuthority();
  }
}
