package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

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
public class RevokePersonSystemRoleUseCase {

  private final RoleRepository roleRepository;
  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;

  @org.springframework.cache.annotation.CacheEvict(
      value = "personPermissions",
      key = "#person.id + '-' + #person.institution.id")
  @Transactional
  public void execute(Person person, SystemRoleCode roleCode) {
    Institution institution = person.getInstitution();
    Role role =
        roleRepository
            .findByScopeAndCodeAndInstitution_Id(
                RoleScope.INSTITUTION, roleCode.name(), institution.getId())
            .orElseThrow(
                () -> new IllegalStateException("System role not seeded: " + roleCode.name()));

    personRoleAssignmentRepository
        .findByPerson_IdAndRole_IdAndInstitution_Id(
            person.getId(), role.getId(), institution.getId())
        .ifPresent(assignment -> personRoleAssignmentRepository.delete(assignment));
  }
}
