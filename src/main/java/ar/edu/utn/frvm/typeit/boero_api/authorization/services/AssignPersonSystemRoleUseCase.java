package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PersonRoleAssignment;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignPersonSystemRoleUseCase {

  private final RoleRepository roleRepository;
  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;
  private final SessionRevocationService sessionRevocationService;
  private final InstitutionRepository institutionRepository;

  @org.springframework.cache.annotation.CacheEvict(
      value = "personPermissions",
      key = "#person.id + '-' + #person.institution.id")
  @Transactional
  public void execute(Person person, SystemRoleCode roleCode) {
    execute(person, roleCode, false);
  }

  @org.springframework.cache.annotation.CacheEvict(
      value = "personPermissions",
      key = "#person.id + '-' + #person.institution.id")
  @Transactional
  public void execute(Person person, SystemRoleCode roleCode, boolean revokeSessions) {
    Institution institution = person.getInstitution();
    institutionRepository
        .findByIdForUpdate(institution.getId())
        .orElseThrow(InstitutionNotFoundException::new);
    Role role =
        roleRepository
            .findByScopeAndCodeAndInstitution_Id(
                RoleScope.INSTITUTION, roleCode.name(), institution.getId())
            .orElseThrow(
                () -> new IllegalStateException("System role not seeded: " + roleCode.name()));

    assign(person, role, roleCode, revokeSessions);
  }

  @org.springframework.cache.annotation.CacheEvict(
      value = "personPermissions",
      key = "#person.id + '-' + #person.institution.id")
  @Transactional
  public void execute(Person person, Role role, boolean revokeSessions) {
    SystemRoleCode technicalCode = role.isSystem() ? SystemRoleCode.valueOf(role.getCode()) : null;
    assign(person, role, technicalCode, revokeSessions);
  }

  private void assign(
      Person person, Role role, SystemRoleCode technicalCode, boolean revokeSessions) {
    Institution institution = person.getInstitution();
    List<PersonRoleAssignment> currentAssignments =
        personRoleAssignmentRepository.findByPerson_IdAndInstitution_Id(
            person.getId(), institution.getId());
    boolean alreadyAssigned =
        currentAssignments.stream()
            .anyMatch(assignment -> assignment.getRole().getId().equals(role.getId()));

    applyApplicantRolePolicy(currentAssignments, technicalCode);
    if (alreadyAssigned) {
      return;
    }

    personRoleAssignmentRepository.save(
        PersonRoleAssignment.builder().person(person).role(role).institution(institution).build());

    if (revokeSessions) {
      sessionRevocationService.revokeInstitutionalSessionsForPerson(
          person.getId(), institution.getId());
    }
  }

  private void applyApplicantRolePolicy(
      final List<PersonRoleAssignment> currentAssignments, final SystemRoleCode roleCode) {
    for (final PersonRoleAssignment assignment : currentAssignments) {
      final boolean assignmentIsApplicant =
          assignment.getRole().getCode().equals(SystemRoleCode.APPLICANT.name());
      final boolean shouldRemove =
          roleCode == SystemRoleCode.APPLICANT ? !assignmentIsApplicant : assignmentIsApplicant;
      if (shouldRemove) {
        personRoleAssignmentRepository.delete(assignment);
      }
    }
  }
}
