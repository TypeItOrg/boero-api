package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ListPeopleUseCaseTest {

  @Mock private PersonRepository personRepository;

  @InjectMocks private ListPeopleUseCase listPeopleUseCase;

  @Test
  @DisplayName("Should list people in institution")
  @SuppressWarnings("unchecked")
  void execute_listsPeople() {
    UUID institutionId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 20);
    Person person = personWith(institutionId, "11111111", "Ana", "García");
    when(personRepository.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(person), pageable, 1));

    var response = listPeopleUseCase.execute(institutionId, null, pageable);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().documentNumber()).isEqualTo("11111111");
  }

  @Test
  @DisplayName("Should pass search term to specification")
  @SuppressWarnings("unchecked")
  void execute_passesSearchTerm() {
    UUID institutionId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 20);
    when(personRepository.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    var response = listPeopleUseCase.execute(institutionId, "ana", pageable);

    assertThat(response.items()).isEmpty();
    assertThat(response.totalItems()).isZero();
  }

  @Test
  @DisplayName("Should return empty page when no matches")
  @SuppressWarnings("unchecked")
  void execute_returnsEmptyWhenNoMatch() {
    UUID institutionId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 20);
    when(personRepository.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    var response = listPeopleUseCase.execute(institutionId, "nope", pageable);

    assertThat(response.items()).isEmpty();
    assertThat(response.totalItems()).isZero();
  }

  private Person personWith(UUID institutionId, String documentNumber, String name, String last) {
    Institution institution = Institution.builder().id(institutionId).name("Conservatorio").build();
    return Person.builder()
        .institution(institution)
        .firstName(name)
        .lastName(last)
        .documentNumber(documentNumber)
        .build();
  }
}
