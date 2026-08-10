package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.entities.User;
import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.PersonRoleAssignment;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.common.search.SearchNormalization;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.PersonSummaryResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListPeopleUseCase {

  private final PersonRepository personRepository;
  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public PaginatedResponse<PersonSummaryResponse> execute(
      final UUID institutionId, final String search, final UUID roleId, final Pageable pageable) {

    final String normalizedSearch = SearchNormalization.normalizeSearch(search);
    final boolean hasFilter = normalizedSearch != null || roleId != null;

    final Page<Person> peoplePage =
        hasFilter
            ? personRepository.search(institutionId, normalizedSearch, roleId, pageable)
            : personRepository.findByInstitution_IdAndDeletedFalse(institutionId, pageable);

    if (peoplePage.isEmpty()) {
      return PaginatedResponse.from(peoplePage.map(PersonSummaryResponse::from));
    }

    final List<UUID> personIds = peoplePage.getContent().stream().map(Person::getId).toList();
    final List<PersonRoleAssignment> roleAssignments =
        personRoleAssignmentRepository.findByPerson_IdInAndInstitution_Id(personIds, institutionId);
    final Map<UUID, Boolean> accessByPerson =
        userRepository.findByPerson_IdInAndInstitution_Id(personIds, institutionId).stream()
            .collect(Collectors.toMap(user -> user.getPerson().getId(), User::isEnabled));

    final Map<UUID, List<PersonSummaryResponse.PersonRoleSummaryResponse>> rolesByPerson =
        roleAssignments.stream()
            .collect(
                Collectors.groupingBy(
                    assignment -> assignment.getPerson().getId(),
                    Collectors.mapping(
                        assignment ->
                            new PersonSummaryResponse.PersonRoleSummaryResponse(
                                assignment.getRole().getCode(), assignment.getRole().getName()),
                        Collectors.toList())));

    return PaginatedResponse.from(
        peoplePage.map(
            person ->
                PersonSummaryResponse.from(
                    person,
                    rolesByPerson.getOrDefault(person.getId(), List.of()),
                    accessByPerson.getOrDefault(person.getId(), false))));
  }
}
