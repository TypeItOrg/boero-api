package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.common.search.SearchNormalization;
import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.CountryRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.CountrySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListCountriesUseCase {

  private final CountryRepository countryRepository;

  public PaginatedResponse<CountrySummaryResponse> execute(String search, Pageable pageable) {
    final String normalizedSearch = SearchNormalization.normalizeSearch(search);
    if (normalizedSearch != null) {
      return PaginatedResponse.from(
          countryRepository
              .searchByName(normalizedSearch, pageable)
              .map(CountrySummaryResponse::from));
    }

    return PaginatedResponse.from(
        countryRepository.findAll(pageable).map(CountrySummaryResponse::from));
  }
}
