package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PersonRoleAssignment;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignPersonSystemRoleUseCase {

  private final RoleRepository roleRepository;
  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;
  private final SessionRevocationService sessionRevocationService;

  @Transactional
  public void execute(Person person, SystemRoleCode roleCode) {
    execute(person, roleCode, false);
  }

  @Transactional
  public void execute(Person person, SystemRoleCode roleCode, boolean revokeSessions) {
    Institution institution = person.getInstitution();
    Role role =
        roleRepository
            .findByScopeAndCodeAndInstitutionIsNull(RoleScope.INSTITUTION, roleCode.name())
            .orElseThrow(
                () -> new IllegalStateException("System role not seeded: " + roleCode.name()));

    if (personRoleAssignmentRepository.existsByPerson_IdAndRole_IdAndInstitution_Id(
        person.getId(), role.getId(), institution.getId())) {
      return;
    }

    personRoleAssignmentRepository.save(
        PersonRoleAssignment.builder().person(person).role(role).institution(institution).build());

    if (revokeSessions) {
      sessionRevocationService.revokeInstitutionalSessionsForPerson(
          person.getId(), institution.getId());
    }
  }
}
