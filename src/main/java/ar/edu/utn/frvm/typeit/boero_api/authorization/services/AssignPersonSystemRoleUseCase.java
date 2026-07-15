package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PersonRoleAssignment;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.LastInstitutionalAuthorityRevocationException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.List;
import java.util.UUID;
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
            .findByScopeAndCodeAndInstitutionIsNull(RoleScope.INSTITUTION, roleCode.name())
            .orElseThrow(
                () -> new IllegalStateException("System role not seeded: " + roleCode.name()));

    List<PersonRoleAssignment> currentAssignments =
        personRoleAssignmentRepository.findByPerson_IdAndInstitution_Id(
            person.getId(), institution.getId());
    boolean alreadyAssigned =
        currentAssignments.stream()
            .anyMatch(assignment -> assignment.getRole().getId().equals(role.getId()));

    applyApplicantRolePolicy(currentAssignments, roleCode, institution.getId());
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
      final List<PersonRoleAssignment> currentAssignments,
      final SystemRoleCode roleCode,
      final UUID institutionId) {
    if (roleCode == SystemRoleCode.APPLICANT) {
      preventReplacingLastInstitutionalAuthority(currentAssignments, institutionId);
    }

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

  private void preventReplacingLastInstitutionalAuthority(
      final List<PersonRoleAssignment> currentAssignments, final UUID institutionId) {
    final boolean hasInstitutionalAuthority =
        currentAssignments.stream()
            .anyMatch(
                assignment ->
                    assignment
                        .getRole()
                        .getCode()
                        .equals(SystemRoleCode.INSTITUTIONAL_AUTHORITY.name()));
    if (!hasInstitutionalAuthority) {
      return;
    }

    final long authorityCount =
        personRoleAssignmentRepository.countActivePeopleByInstitutionIdAndRoleCode(
            institutionId, SystemRoleCode.INSTITUTIONAL_AUTHORITY.name());
    if (authorityCount <= 1) {
      throw new LastInstitutionalAuthorityRevocationException();
    }
  }
}
