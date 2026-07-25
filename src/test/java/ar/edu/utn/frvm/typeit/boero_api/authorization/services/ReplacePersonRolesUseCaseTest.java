package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.services.SessionRevocationService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PersonRoleAssignment;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.InstitutionalAuthorityRoleImmutableException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.payloads.ReplacePersonRolesRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReplacePersonRolesUseCaseTest {

  @Mock private InstitutionPersonResolver institutionPersonResolver;
  @Mock private RoleRepository roleRepository;
  @Mock private PersonRoleAssignmentRepository assignmentRepository;
  @Mock private SessionRevocationService sessionRevocationService;
  @Mock private AuthorizationCacheInvalidator authorizationCacheInvalidator;

  @InjectMocks private ReplacePersonRolesUseCase replacePersonRolesUseCase;

  @Test
  @DisplayName("Should reject removing institutional authority from institutional portal")
  void execute_rejectsRemovingInstitutionalAuthority() {
    UUID institutionId = UUID.randomUUID();
    UUID personId = UUID.randomUUID();
    Person person = personWith(institutionId, personId);
    Role authority = roleWith(SystemRoleCode.INSTITUTIONAL_AUTHORITY);
    Role student = roleWith(SystemRoleCode.STUDENT);
    PersonRoleAssignment authorityAssignment = assignmentWith(person, authority);

    givenPersonAndCurrentRoles(institutionId, personId, person, List.of(authorityAssignment));
    givenRole(institutionId, student);

    assertThatThrownBy(
            () ->
                replacePersonRolesUseCase.execute(
                    institutionId,
                    personId,
                    new ReplacePersonRolesRequest(Set.of(student.getId())),
                    false,
                    Set.of(
                        PermissionCode.INSTITUTION_ROLE_ASSIGN,
                        PermissionCode.INSTITUTION_ROLE_REVOKE)))
        .isInstanceOf(InstitutionalAuthorityRoleImmutableException.class);

    verify(assignmentRepository, never()).delete(any());
    verify(assignmentRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should preserve institutional authority when adding another role")
  void execute_preservesInstitutionalAuthorityWhenAddingRole() {
    UUID institutionId = UUID.randomUUID();
    UUID personId = UUID.randomUUID();
    Person person = personWith(institutionId, personId);
    Role authority = roleWith(SystemRoleCode.INSTITUTIONAL_AUTHORITY);
    Role student = roleWith(SystemRoleCode.STUDENT);
    PersonRoleAssignment authorityAssignment = assignmentWith(person, authority);

    givenPersonAndCurrentRoles(institutionId, personId, person, List.of(authorityAssignment));
    givenRole(institutionId, authority);
    givenRole(institutionId, student);

    replacePersonRolesUseCase.execute(
        institutionId,
        personId,
        new ReplacePersonRolesRequest(Set.of(authority.getId(), student.getId())),
        false,
        Set.of(PermissionCode.INSTITUTION_ROLE_ASSIGN));

    verify(assignmentRepository).save(any(PersonRoleAssignment.class));
    verify(assignmentRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should reject combining institutional authority with applicant")
  void execute_rejectsAuthorityAndApplicantCombination() {
    UUID institutionId = UUID.randomUUID();
    UUID personId = UUID.randomUUID();
    Person person = personWith(institutionId, personId);
    Role authority = roleWith(SystemRoleCode.INSTITUTIONAL_AUTHORITY);
    Role applicant = roleWith(SystemRoleCode.APPLICANT);
    PersonRoleAssignment authorityAssignment = assignmentWith(person, authority);

    givenPersonAndCurrentRoles(institutionId, personId, person, List.of(authorityAssignment));
    givenRole(institutionId, authority);
    givenRole(institutionId, applicant);

    assertThatThrownBy(
            () ->
                replacePersonRolesUseCase.execute(
                    institutionId,
                    personId,
                    new ReplacePersonRolesRequest(Set.of(authority.getId(), applicant.getId())),
                    false,
                    Set.of(PermissionCode.INSTITUTION_ROLE_ASSIGN)))
        .isInstanceOf(InstitutionalAuthorityRoleImmutableException.class);

    verify(assignmentRepository, never()).delete(any());
    verify(assignmentRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should allow platform portal to replace authority with applicant")
  void execute_allowsPlatformReplacement() {
    UUID institutionId = UUID.randomUUID();
    UUID personId = UUID.randomUUID();
    Person person = personWith(institutionId, personId);
    Role authority = roleWith(SystemRoleCode.INSTITUTIONAL_AUTHORITY);
    Role applicant = roleWith(SystemRoleCode.APPLICANT);
    PersonRoleAssignment authorityAssignment = assignmentWith(person, authority);

    givenPersonAndCurrentRoles(institutionId, personId, person, List.of(authorityAssignment));
    givenRole(institutionId, applicant);

    replacePersonRolesUseCase.execute(
        institutionId,
        personId,
        new ReplacePersonRolesRequest(Set.of(applicant.getId())),
        true,
        Set.of());

    verify(assignmentRepository).delete(authorityAssignment);
    verify(assignmentRepository).save(any(PersonRoleAssignment.class));
  }

  private void givenPersonAndCurrentRoles(
      UUID institutionId,
      UUID personId,
      Person person,
      List<PersonRoleAssignment> currentAssignments) {
    when(institutionPersonResolver.requirePersonInInstitutionForUpdate(institutionId, personId))
        .thenReturn(person);
    when(assignmentRepository.findByPerson_IdAndInstitution_Id(personId, institutionId))
        .thenReturn(currentAssignments);
  }

  private void givenRole(UUID institutionId, Role role) {
    when(roleRepository.findByIdAndScopeAndInstitution_Id(
            role.getId(), RoleScope.INSTITUTION, institutionId))
        .thenReturn(Optional.of(role));
  }

  private static Person personWith(UUID institutionId, UUID personId) {
    return Person.builder()
        .id(personId)
        .institution(Institution.builder().id(institutionId).build())
        .build();
  }

  private static Role roleWith(SystemRoleCode roleCode) {
    return Role.builder()
        .id(UUID.randomUUID())
        .code(roleCode.name())
        .name(roleCode.getDisplayName())
        .scope(RoleScope.INSTITUTION)
        .system(true)
        .build();
  }

  private static PersonRoleAssignment assignmentWith(Person person, Role role) {
    return PersonRoleAssignment.builder()
        .person(person)
        .role(role)
        .institution(person.getInstitution())
        .build();
  }
}
