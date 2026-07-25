package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PersonRoleAssignment;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.LastPersonRoleRevocationException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.RoleNotAssignableException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RevokePersonRoleUseCase {

  private final InstitutionPersonResolver institutionPersonResolver;
  private final RoleRepository roleRepository;
  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;
  private final AuthorizationCacheInvalidator authorizationCacheInvalidator;

  @Transactional
  public void execute(UUID institutionId, UUID personId, UUID roleId, boolean allowAuthority) {
    Person person =
        institutionPersonResolver.requirePersonInInstitutionForUpdate(institutionId, personId);
    Role role =
        roleRepository
            .findByIdAndScopeAndInstitution_Id(roleId, RoleScope.INSTITUTION, institutionId)
            .orElseThrow(RoleNotAssignableException::new);
    boolean authority = role.isInstitutionalAuthority();
    if (authority && !allowAuthority) {
      throw new RoleNotAssignableException();
    }
    List<PersonRoleAssignment> currentAssignments =
        personRoleAssignmentRepository.findByPerson_IdAndInstitution_Id(personId, institutionId);

    preventRevokingOnlyRole(currentAssignments, role.getId());
    personRoleAssignmentRepository
        .findByPerson_IdAndRole_IdAndInstitution_Id(personId, roleId, institutionId)
        .ifPresent(personRoleAssignmentRepository::delete);
    authorizationCacheInvalidator.evictPerson(personId, institutionId);
  }

  private void preventRevokingOnlyRole(
      final List<PersonRoleAssignment> currentAssignments, final UUID roleId) {
    boolean roleIsAssigned =
        currentAssignments.stream()
            .anyMatch(assignment -> assignment.getRole().getId().equals(roleId));
    if (roleIsAssigned && currentAssignments.size() == 1) {
      throw new LastPersonRoleRevocationException();
    }
  }
}
