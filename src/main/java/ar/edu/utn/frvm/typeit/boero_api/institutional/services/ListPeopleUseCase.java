package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.person.PersonSummaryResponse;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ListPeopleUseCase {

  private final PersonRepository personRepository;

  @Transactional(readOnly = true)
  public PaginatedResponse<PersonSummaryResponse> execute(
      final UUID institutionId, final String search, final Pageable pageable) {
    var page = personRepository.findAll(specification(institutionId, search), pageable);
    return PaginatedResponse.from(page.map(PersonSummaryResponse::from));
  }

  private Specification<Person> specification(final UUID institutionId, final String search) {
    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(builder.equal(root.get("institution").get("id"), institutionId));
      predicates.add(builder.isFalse(root.get("deleted")));
      if (StringUtils.hasText(search)) {
        String value = "%" + search.toLowerCase() + "%";
        predicates.add(
            builder.or(
                builder.like(builder.lower(root.get("firstName")), value),
                builder.like(builder.lower(root.get("lastName")), value),
                builder.like(root.get("documentNumber"), value)));
      }
      return builder.and(predicates.toArray(Predicate[]::new));
    };
  }
}
