package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
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
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ListPeopleUseCaseTest {

  @Mock private PersonRepository personRepository;
  @Mock private PersonRoleAssignmentRepository personRoleAssignmentRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks private ListPeopleUseCase listPeopleUseCase;

  @Test
  @DisplayName("Should list people in institution when no search provided")
  void execute_listsPeopleWithoutSearch() {
    UUID institutionId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 20);
    Person person = personWith(institutionId, "11111111", "Ana", "García");
    when(personRepository.findByInstitution_IdAndDeletedFalse(institutionId, pageable))
        .thenReturn(new PageImpl<>(List.of(person), pageable, 1));
    when(personRoleAssignmentRepository.findByPerson_IdInAndInstitution_Id(
            List.of(person.getId()), institutionId))
        .thenReturn(List.of());
    when(userRepository.findByPerson_IdInAndInstitution_Id(List.of(person.getId()), institutionId))
        .thenReturn(List.of());

    var response = listPeopleUseCase.execute(institutionId, null, null, pageable);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().firstName()).isEqualTo("Ana");
    assertThat(response.items().getFirst().lastName()).isEqualTo("García");
    assertThat(response.items().getFirst().documentNumber()).isEqualTo("11111111");
  }

  @Test
  @DisplayName("Should delegate to search when search term is provided")
  void execute_delegatesToSearchWhenSearchProvided() {
    UUID institutionId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 20);
    Person person = personWith(institutionId, "11111111", "Ana", "García");
    when(personRepository.search(institutionId, "ana", null, pageable))
        .thenReturn(new PageImpl<>(List.of(person), pageable, 1));
    when(personRoleAssignmentRepository.findByPerson_IdInAndInstitution_Id(
            List.of(person.getId()), institutionId))
        .thenReturn(List.of());
    when(userRepository.findByPerson_IdInAndInstitution_Id(List.of(person.getId()), institutionId))
        .thenReturn(List.of());

    var response = listPeopleUseCase.execute(institutionId, "ana", null, pageable);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().firstName()).isEqualTo("Ana");
  }

  @Test
  @DisplayName("Should delegate to search when role ID is provided")
  void execute_delegatesToSearchWhenRoleIdProvided() {
    UUID institutionId = UUID.randomUUID();
    UUID roleId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 20);
    Person person = personWith(institutionId, "11111111", "Ana", "García");
    when(personRepository.search(institutionId, null, roleId, pageable))
        .thenReturn(new PageImpl<>(List.of(person), pageable, 1));
    when(personRoleAssignmentRepository.findByPerson_IdInAndInstitution_Id(
            List.of(person.getId()), institutionId))
        .thenReturn(List.of());
    when(userRepository.findByPerson_IdInAndInstitution_Id(List.of(person.getId()), institutionId))
        .thenReturn(List.of());

    var response = listPeopleUseCase.execute(institutionId, null, roleId, pageable);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().firstName()).isEqualTo("Ana");
  }

  @Test
  @DisplayName("Should trim the search before delegating to repository")
  void execute_trimsSearchBeforeDelegating() {
    UUID institutionId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 20);
    when(personRepository.search(institutionId, "matias", null, pageable))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    listPeopleUseCase.execute(institutionId, "  matias  ", null, pageable);

    verify(personRepository).search(institutionId, "matias", null, pageable);
  }

  @Test
  @DisplayName("Should treat whitespace-only search as no search")
  void execute_treatsWhitespaceOnlyAsNoSearch() {
    UUID institutionId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 20);
    when(personRepository.findByInstitution_IdAndDeletedFalse(institutionId, pageable))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    listPeopleUseCase.execute(institutionId, "   ", null, pageable);

    verify(personRepository, never()).search(institutionId, "   ", null, pageable);
  }

  @Test
  @DisplayName("Should return empty page when no matches")
  void execute_returnsEmptyWhenNoMatch() {
    UUID institutionId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 20);
    when(personRepository.search(institutionId, "nope", null, pageable))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    var response = listPeopleUseCase.execute(institutionId, "nope", null, pageable);

    assertThat(response.items()).isEmpty();
    assertThat(response.totalItems()).isZero();
  }

  private Person personWith(UUID institutionId, String documentNumber, String name, String last) {
    Institution institution = Institution.builder().id(institutionId).name("Conservatorio").build();
    return Person.builder()
        .id(UUID.randomUUID())
        .institution(institution)
        .firstName(name)
        .lastName(last)
        .documentNumber(documentNumber)
        .build();
  }
}
