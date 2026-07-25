package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.services.SessionRevocationService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PersonRoleAssignment;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.InstitutionalAuthorityRoleImmutableException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.LastPersonRoleRevocationException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.RoleAssignmentNotAllowedException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.RoleNotAssignableException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.RoleRevocationNotAllowedException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.payloads.PersonRoleResponse;
import ar.edu.utn.frvm.typeit.boero_api.authorization.payloads.ReplacePersonRolesRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReplacePersonRolesUseCase {

  private final InstitutionPersonResolver institutionPersonResolver;
  private final RoleRepository roleRepository;
  private final PersonRoleAssignmentRepository assignmentRepository;
  private final SessionRevocationService sessionRevocationService;
  private final AuthorizationCacheInvalidator authorizationCacheInvalidator;

  @Transactional
  public List<PersonRoleResponse> execute(
      UUID institutionId,
      UUID personId,
      ReplacePersonRolesRequest request,
      boolean allowAuthority,
      Set<PermissionCode> actorPermissions) {
    Person person =
        institutionPersonResolver.requirePersonInInstitutionForUpdate(institutionId, personId);
    List<PersonRoleAssignment> current =
        assignmentRepository.findByPerson_IdAndInstitution_Id(personId, institutionId);

    Map<UUID, Role> desiredRoles = loadRoles(institutionId, request.roleIds());
    ensureInstitutionalAuthorityIsUnchanged(current, desiredRoles, allowAuthority);
    removeApplicantWhenAnotherRoleIsSelected(desiredRoles);
    if (desiredRoles.isEmpty()) {
      throw new LastPersonRoleRevocationException();
    }

    Set<UUID> currentIds =
        current.stream()
            .map(assignment -> assignment.getRole().getId())
            .collect(Collectors.toSet());
    Set<UUID> desiredIds = desiredRoles.keySet();
    Set<UUID> additions = new HashSet<>(desiredIds);
    additions.removeAll(currentIds);
    Set<UUID> removals = new HashSet<>(currentIds);
    removals.removeAll(desiredIds);
    requirePermissionForChanges(additions, removals, allowAuthority, actorPermissions);

    current.stream()
        .filter(assignment -> !desiredIds.contains(assignment.getRole().getId()))
        .forEach(assignmentRepository::delete);
    for (Role role : desiredRoles.values()) {
      if (!currentIds.contains(role.getId())) {
        assignmentRepository.save(
            PersonRoleAssignment.assign(person, role, person.getInstitution()));
      }
    }

    if (!additions.isEmpty() || !removals.isEmpty()) {
      sessionRevocationService.revokeInstitutionalSessionsForPerson(personId, institutionId);
      authorizationCacheInvalidator.evictPerson(personId, institutionId);
    }

    return assignmentRepository.findByPerson_IdAndInstitution_Id(personId, institutionId).stream()
        .map(PersonRoleResponse::from)
        .toList();
  }

  private Map<UUID, Role> loadRoles(UUID institutionId, Set<UUID> roleIds) {
    Map<UUID, Role> roles = new HashMap<>();
    for (UUID roleId : roleIds) {
      Role role =
          roleRepository
              .findByIdAndScopeAndInstitution_Id(roleId, RoleScope.INSTITUTION, institutionId)
              .orElseThrow(RoleNotAssignableException::new);
      roles.put(role.getId(), role);
    }
    return roles;
  }

  private void ensureInstitutionalAuthorityIsUnchanged(
      List<PersonRoleAssignment> current, Map<UUID, Role> desiredRoles, boolean allowAuthority) {
    if (allowAuthority) return;

    Set<UUID> currentAuthorityIds =
        current.stream()
            .map(PersonRoleAssignment::getRole)
            .filter(Role::isInstitutionalAuthority)
            .map(Role::getId)
            .collect(Collectors.toSet());
    Set<UUID> desiredAuthorityIds =
        desiredRoles.values().stream()
            .filter(Role::isInstitutionalAuthority)
            .map(Role::getId)
            .collect(Collectors.toSet());

    if (!currentAuthorityIds.equals(desiredAuthorityIds)
        || (!currentAuthorityIds.isEmpty()
            && desiredRoles.values().stream().anyMatch(Role::isApplicant))) {
      throw new InstitutionalAuthorityRoleImmutableException();
    }
  }

  private void removeApplicantWhenAnotherRoleIsSelected(Map<UUID, Role> roles) {
    boolean hasNonApplicant = roles.values().stream().anyMatch(role -> !role.isApplicant());
    if (hasNonApplicant) {
      roles.values().removeIf(Role::isApplicant);
    }
  }

  private void requirePermissionForChanges(
      Set<UUID> additions,
      Set<UUID> removals,
      boolean allowAuthority,
      Set<PermissionCode> actorPermissions) {
    if (allowAuthority) return;
    if (!additions.isEmpty()
        && !actorPermissions.contains(PermissionCode.INSTITUTION_ROLE_ASSIGN)) {
      throw new RoleAssignmentNotAllowedException();
    }
    if (!removals.isEmpty() && !actorPermissions.contains(PermissionCode.INSTITUTION_ROLE_REVOKE)) {
      throw new RoleRevocationNotAllowedException();
    }
  }
}
