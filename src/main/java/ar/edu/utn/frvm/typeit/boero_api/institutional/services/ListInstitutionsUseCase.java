package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.InstitutionListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListInstitutionsUseCase {

  private final InstitutionRepository institutionRepository;

  public PaginatedResponse<InstitutionListItemResponse> execute(
      final String search, final Boolean active, final Pageable pageable) {
    final String normalizedSearch = normalizeSearch(search);
    return PaginatedResponse.from(
        institutionRepository
            .findWithLocationByFilters(normalizedSearch, active, pageable)
            .map(InstitutionListItemResponse::from));
  }

  private static String normalizeSearch(final String search) {
    if (search == null || search.isBlank()) {
      return null;
    }
    return search.trim();
  }
}
