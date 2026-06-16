package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PersonRoleAssignment;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.PersonNotFoundInInstitutionException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.RoleNotAssignableException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.payloads.AssignRoleRequest;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignPersonRoleUseCaseTest {

  @Mock private InstitutionPersonResolver institutionPersonResolver;
  @Mock private InstitutionalSystemRoleResolver institutionalSystemRoleResolver;
  @Mock private PersonRoleAssignmentRepository personRoleAssignmentRepository;
  @Mock private AssignPersonSystemRoleUseCase assignPersonSystemRoleUseCase;

  @InjectMocks private AssignPersonRoleUseCase assignPersonRoleUseCase;

  @Test
  @DisplayName("Should assign role to person in institution")
  void execute_assignsRole() {
    UUID institutionId = UUID.randomUUID();
    UUID personId = UUID.randomUUID();
    Person person = personWith(institutionId, personId);
    Role role = roleWith(SystemRoleCode.TEACHER);
    PersonRoleAssignment assignment = assignmentWith(person, role, institutionId);

    when(institutionPersonResolver.requirePersonInInstitution(institutionId, personId))
        .thenReturn(person);
    when(institutionalSystemRoleResolver.requireInstitutionalSystemRole(SystemRoleCode.TEACHER))
        .thenReturn(role);
    when(personRoleAssignmentRepository.findByPerson_IdAndRole_IdAndInstitution_Id(
            personId, role.getId(), institutionId))
        .thenReturn(Optional.of(assignment));

    var response =
        assignPersonRoleUseCase.execute(
            institutionId, personId, new AssignRoleRequest(SystemRoleCode.TEACHER));

    verify(assignPersonSystemRoleUseCase).execute(person, SystemRoleCode.TEACHER);
    assertThat(response.roleCode()).isEqualTo(SystemRoleCode.TEACHER);
    assertThat(response.displayName()).isEqualTo("Docente");
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
                assignPersonRoleUseCase.execute(
                    institutionId, personId, new AssignRoleRequest(SystemRoleCode.TEACHER)))
        .isInstanceOf(PersonNotFoundInInstitutionException.class);
  }

  @Test
  @DisplayName("Should throw when role is not assignable")
  void execute_throwsWhenRoleNotAssignable() {
    UUID institutionId = UUID.randomUUID();
    UUID personId = UUID.randomUUID();
    Person person = personWith(institutionId, personId);

    when(institutionPersonResolver.requirePersonInInstitution(institutionId, personId))
        .thenReturn(person);
    when(institutionalSystemRoleResolver.requireInstitutionalSystemRole(SystemRoleCode.TEACHER))
        .thenThrow(RoleNotAssignableException.class);

    assertThatThrownBy(
            () ->
                assignPersonRoleUseCase.execute(
                    institutionId, personId, new AssignRoleRequest(SystemRoleCode.TEACHER)))
        .isInstanceOf(RoleNotAssignableException.class);
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

  private static PersonRoleAssignment assignmentWith(Person person, Role role, UUID institutionId) {
    return PersonRoleAssignment.builder()
        .person(person)
        .role(role)
        .institution(person.getInstitution())
        .build();
  }
}
