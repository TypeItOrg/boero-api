package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PersonRoleAssignment;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.LastPersonRoleRevocationException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.PersonNotFoundInInstitutionException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RevokePersonRoleUseCaseTest {

  @Mock private InstitutionPersonResolver institutionPersonResolver;
  @Mock private RoleRepository roleRepository;
  @Mock private PersonRoleAssignmentRepository personRoleAssignmentRepository;
  @Mock private InstitutionRepository institutionRepository;
  @Mock private AuthorizationCacheInvalidator authorizationCacheInvalidator;

  @InjectMocks private RevokePersonRoleUseCase revokePersonRoleUseCase;

  @BeforeEach
  void setUp() {
    when(institutionRepository.findByIdForUpdate(any(UUID.class)))
        .thenAnswer(
            invocation ->
                Optional.of(
                    Institution.builder().id(invocation.getArgument(0)).name("Boero").build()));
  }

  @Test
  @DisplayName("Should revoke role from person")
  void execute_revokesRole() {
    UUID institutionId = UUID.randomUUID();
    UUID personId = UUID.randomUUID();
    Person person = personWith(institutionId, personId);
    Role role = roleWith(SystemRoleCode.TEACHER);

    when(institutionPersonResolver.requirePersonInInstitution(institutionId, personId))
        .thenReturn(person);
    when(roleRepository.findByIdAndScopeAndInstitution_Id(
            role.getId(), RoleScope.INSTITUTION, institutionId))
        .thenReturn(Optional.of(role));

    revokePersonRoleUseCase.execute(institutionId, personId, role.getId(), false);
  }

  @Test
  @DisplayName("Should throw when revoking the person's only role")
  void execute_throwsWhenRevokingOnlyRole() {
    UUID institutionId = UUID.randomUUID();
    UUID personId = UUID.randomUUID();
    Person person = personWith(institutionId, personId);
    Role role = roleWith(SystemRoleCode.TEACHER);
    PersonRoleAssignment assignment =
        PersonRoleAssignment.builder()
            .person(person)
            .role(role)
            .institution(person.getInstitution())
            .build();

    when(institutionPersonResolver.requirePersonInInstitution(institutionId, personId))
        .thenReturn(person);
    when(roleRepository.findByIdAndScopeAndInstitution_Id(
            role.getId(), RoleScope.INSTITUTION, institutionId))
        .thenReturn(Optional.of(role));
    when(personRoleAssignmentRepository.findByPerson_IdAndInstitution_Id(personId, institutionId))
        .thenReturn(List.of(assignment));

    assertThatThrownBy(
            () -> revokePersonRoleUseCase.execute(institutionId, personId, role.getId(), false))
        .isInstanceOf(LastPersonRoleRevocationException.class);
  }

  @Test
  @DisplayName("Should allow revoking the last institutional authority")
  void execute_revokesLastInstitutionalAuthority() {
    UUID institutionId = UUID.randomUUID();
    UUID personId = UUID.randomUUID();
    Person person = personWith(institutionId, personId);
    Role role = roleWith(SystemRoleCode.INSTITUTIONAL_AUTHORITY);

    when(institutionPersonResolver.requirePersonInInstitution(institutionId, personId))
        .thenReturn(person);
    when(roleRepository.findByIdAndScopeAndInstitution_Id(
            role.getId(), RoleScope.INSTITUTION, institutionId))
        .thenReturn(Optional.of(role));
    Role otherRole = roleWith(SystemRoleCode.TEACHER);
    PersonRoleAssignment authorityAssignment =
        PersonRoleAssignment.builder()
            .person(person)
            .role(role)
            .institution(person.getInstitution())
            .build();
    PersonRoleAssignment otherAssignment =
        PersonRoleAssignment.builder()
            .person(person)
            .role(otherRole)
            .institution(person.getInstitution())
            .build();
    when(personRoleAssignmentRepository.findByPerson_IdAndInstitution_Id(personId, institutionId))
        .thenReturn(List.of(authorityAssignment, otherAssignment));
    when(personRoleAssignmentRepository.findByPerson_IdAndRole_IdAndInstitution_Id(
            personId, role.getId(), institutionId))
        .thenReturn(Optional.of(authorityAssignment));

    revokePersonRoleUseCase.execute(institutionId, personId, role.getId(), true);

    verify(personRoleAssignmentRepository).delete(authorityAssignment);
  }

  @Test
  @DisplayName("Should throw when person does not belong to institution")
  void execute_throwsWhenPersonNotFound() {
    UUID institutionId = UUID.randomUUID();
    UUID personId = UUID.randomUUID();

    when(institutionPersonResolver.requirePersonInInstitution(institutionId, personId))
        .thenThrow(PersonNotFoundInInstitutionException.class);

    assertThatThrownBy(
            () ->
                revokePersonRoleUseCase.execute(institutionId, personId, UUID.randomUUID(), false))
        .isInstanceOf(PersonNotFoundInInstitutionException.class);
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
}
