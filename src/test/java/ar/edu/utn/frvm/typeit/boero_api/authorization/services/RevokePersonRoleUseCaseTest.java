package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PersonRoleAssignment;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.LastInstitutionalAuthorityRevocationException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.LastPersonRoleRevocationException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.PersonNotFoundInInstitutionException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
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
  @Mock private InstitutionalSystemRoleResolver institutionalSystemRoleResolver;
  @Mock private PersonRoleAssignmentRepository personRoleAssignmentRepository;
  @Mock private RevokePersonSystemRoleUseCase revokePersonSystemRoleUseCase;
  @Mock private InstitutionRepository institutionRepository;

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
    when(institutionalSystemRoleResolver.requireInstitutionalSystemRole(SystemRoleCode.TEACHER))
        .thenReturn(role);

    revokePersonRoleUseCase.execute(institutionId, personId, SystemRoleCode.TEACHER);

    verify(revokePersonSystemRoleUseCase).execute(person, SystemRoleCode.TEACHER);
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
    when(institutionalSystemRoleResolver.requireInstitutionalSystemRole(SystemRoleCode.TEACHER))
        .thenReturn(role);
    when(personRoleAssignmentRepository.findByPerson_IdAndInstitution_Id(personId, institutionId))
        .thenReturn(List.of(assignment));

    assertThatThrownBy(
            () -> revokePersonRoleUseCase.execute(institutionId, personId, SystemRoleCode.TEACHER))
        .isInstanceOf(LastPersonRoleRevocationException.class);

    verify(revokePersonSystemRoleUseCase, never()).execute(person, SystemRoleCode.TEACHER);
  }

  @Test
  @DisplayName("Should throw when revoking last institutional authority")
  void execute_throwsWhenRevokingLastInstitutionalAuthority() {
    UUID institutionId = UUID.randomUUID();
    UUID personId = UUID.randomUUID();
    Person person = personWith(institutionId, personId);
    Role role = roleWith(SystemRoleCode.INSTITUTIONAL_AUTHORITY);

    when(institutionPersonResolver.requirePersonInInstitution(institutionId, personId))
        .thenReturn(person);
    when(institutionalSystemRoleResolver.requireInstitutionalSystemRole(
            SystemRoleCode.INSTITUTIONAL_AUTHORITY))
        .thenReturn(role);
    when(personRoleAssignmentRepository.existsByPerson_IdAndRole_IdAndInstitution_Id(
            personId, role.getId(), institutionId))
        .thenReturn(true);
    when(personRoleAssignmentRepository.countActivePeopleByInstitutionIdAndRoleCode(
            institutionId, SystemRoleCode.INSTITUTIONAL_AUTHORITY.name()))
        .thenReturn(1L);

    assertThatThrownBy(
            () ->
                revokePersonRoleUseCase.execute(
                    institutionId, personId, SystemRoleCode.INSTITUTIONAL_AUTHORITY))
        .isInstanceOf(LastInstitutionalAuthorityRevocationException.class);

    verify(revokePersonSystemRoleUseCase, never())
        .execute(person, SystemRoleCode.INSTITUTIONAL_AUTHORITY);
  }

  @Test
  @DisplayName("Should throw when revoking and other authorities in database are soft-deleted")
  void execute_throwsWhenOtherAuthoritiesAreSoftDeleted() {
    UUID institutionId = UUID.randomUUID();
    UUID personId = UUID.randomUUID();
    Person person = personWith(institutionId, personId);
    Role role = roleWith(SystemRoleCode.INSTITUTIONAL_AUTHORITY);

    when(institutionPersonResolver.requirePersonInInstitution(institutionId, personId))
        .thenReturn(person);
    when(institutionalSystemRoleResolver.requireInstitutionalSystemRole(
            SystemRoleCode.INSTITUTIONAL_AUTHORITY))
        .thenReturn(role);
    when(personRoleAssignmentRepository.existsByPerson_IdAndRole_IdAndInstitution_Id(
            personId, role.getId(), institutionId))
        .thenReturn(true);
    // There are 2 authority assignments, but only 1 belongs to an active (non-deleted) person
    when(personRoleAssignmentRepository.countActivePeopleByInstitutionIdAndRoleCode(
            institutionId, SystemRoleCode.INSTITUTIONAL_AUTHORITY.name()))
        .thenReturn(1L);

    assertThatThrownBy(
            () ->
                revokePersonRoleUseCase.execute(
                    institutionId, personId, SystemRoleCode.INSTITUTIONAL_AUTHORITY))
        .isInstanceOf(LastInstitutionalAuthorityRevocationException.class);

    verify(revokePersonSystemRoleUseCase, never())
        .execute(person, SystemRoleCode.INSTITUTIONAL_AUTHORITY);
  }

  @Test
  @DisplayName("Should throw when person does not belong to institution")
  void execute_throwsWhenPersonNotFound() {
    UUID institutionId = UUID.randomUUID();
    UUID personId = UUID.randomUUID();

    when(institutionPersonResolver.requirePersonInInstitution(institutionId, personId))
        .thenThrow(PersonNotFoundInInstitutionException.class);

    assertThatThrownBy(
            () -> revokePersonRoleUseCase.execute(institutionId, personId, SystemRoleCode.TEACHER))
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
