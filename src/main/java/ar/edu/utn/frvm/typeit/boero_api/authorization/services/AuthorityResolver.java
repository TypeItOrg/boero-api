package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PlatformAccountRoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RolePermissionRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthorityResolver {

  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;
  private final PlatformAccountRoleRepository platformAccountRoleRepository;
  private final RolePermissionRepository rolePermissionRepository;

  @org.springframework.cache.annotation.Cacheable(
      value = "personPermissions",
      key = "#personId + '-' + #institutionId")
  @Transactional(readOnly = true)
  public Set<PermissionCode> resolveForPerson(UUID personId, UUID institutionId) {
    return permissionsForRoleIds(
        personRoleAssignmentRepository.findRoleIdsByPersonIdAndInstitutionId(
            personId, institutionId));
  }

  @org.springframework.cache.annotation.Cacheable(
      value = "platformAccountPermissions",
      key = "#platformAccountId")
  @Transactional(readOnly = true)
  public Set<PermissionCode> resolveForPlatformAccount(UUID platformAccountId) {
    return permissionsForRoleIds(
        platformAccountRoleRepository.findRoleIdsByPlatformAccountId(platformAccountId));
  }

  @org.springframework.cache.annotation.Cacheable(
      value = "platformAccountRoles",
      key = "#platformAccountId")
  @Transactional(readOnly = true)
  public Set<PlatformRoleCode> resolvePlatformRoles(UUID platformAccountId) {
    return platformAccountRoleRepository
        .findSystemRoleCodesByPlatformAccountId(platformAccountId)
        .stream()
        .map(PlatformRoleCode::valueOf)
        .collect(Collectors.toUnmodifiableSet());
  }

  private Set<PermissionCode> permissionsForRoleIds(List<UUID> roleIds) {
    if (roleIds.isEmpty()) return Set.of();

    return rolePermissionRepository.findPermissionCodesByRoleIds(roleIds).stream()
        .map(PermissionCode::fromCode)
        .collect(Collectors.toUnmodifiableSet());
  }
}
