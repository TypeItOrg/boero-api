package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.PersonSummaryResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListPeopleUseCase {

  private final PersonRepository personRepository;

  @Transactional(readOnly = true)
  public PaginatedResponse<PersonSummaryResponse> execute(
      final UUID institutionId, final String search, final Pageable pageable) {
    if (search != null && !search.isBlank()) {
      return PaginatedResponse.from(
          personRepository
              .search(institutionId, search.trim(), pageable)
              .map(PersonSummaryResponse::from));
    }
    return PaginatedResponse.from(
        personRepository
            .findByInstitution_IdAndDeletedFalse(institutionId, pageable)
            .map(PersonSummaryResponse::from));
  }
}
