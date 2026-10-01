package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PersonRoleAssignment;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ListInstitutionTeachersUseCaseTest {

  @Mock private PersonRepository personRepository;
  @Mock private PersonRoleAssignmentRepository personRoleAssignmentRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks private ListInstitutionTeachersUseCase listInstitutionTeachersUseCase;

  @Test
  @DisplayName("Should list teachers with roles and access status")
  void execute_listsTeachersWithRolesAndAccessStatus() {
    UUID institutionId = UUID.randomUUID();
    var pageable = PageRequest.of(0, 20);
    Institution institution = Institution.builder().id(institutionId).name("Boero").build();
    Person person =
        Person.builder()
            .id(UUID.randomUUID())
            .institution(institution)
            .firstName("Ana")
            .lastName("García")
            .documentNumber("12345678")
            .build();
    Role role =
        Role.builder()
            .code(SystemRoleCode.TEACHER.name())
            .name(SystemRoleCode.TEACHER.getDisplayName())
            .build();
    PersonRoleAssignment assignment =
        PersonRoleAssignment.builder().person(person).institution(institution).role(role).build();
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .person(person)
            .institution(institution)
            .enabled(false)
            .build();
    when(personRepository.findByInstitutionAndRoleCode(
            institutionId, SystemRoleCode.TEACHER.name(), "ana", pageable))
        .thenReturn(new PageImpl<>(List.of(person), pageable, 1));
    when(personRoleAssignmentRepository.findByPerson_IdInAndInstitution_Id(
            List.of(person.getId()), institutionId))
        .thenReturn(List.of(assignment));
    when(userRepository.findByPerson_IdInAndInstitution_Id(List.of(person.getId()), institutionId))
        .thenReturn(List.of(user));

    var response = listInstitutionTeachersUseCase.execute(institutionId, "  ana  ", pageable);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().documentNumber()).isEqualTo("12345678");
    assertThat(response.items().getFirst().enabled()).isFalse();
    assertThat(response.items().getFirst().roles())
        .extracting(roleSummary -> roleSummary.roleCode())
        .containsExactly(SystemRoleCode.TEACHER.name());
    verify(personRepository)
        .findByInstitutionAndRoleCode(
            institutionId, SystemRoleCode.TEACHER.name(), "ana", pageable);
  }

  @Test
  @DisplayName("Should return empty page when no teacher matches")
  void execute_returnsEmptyWhenNoTeacherMatches() {
    UUID institutionId = UUID.randomUUID();
    var pageable = PageRequest.of(0, 20);
    when(personRepository.findByInstitutionAndRoleCode(
            institutionId, SystemRoleCode.TEACHER.name(), "nope", pageable))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    var response = listInstitutionTeachersUseCase.execute(institutionId, "nope", pageable);

    assertThat(response.items()).isEmpty();
    assertThat(response.totalItems()).isZero();
  }
}
