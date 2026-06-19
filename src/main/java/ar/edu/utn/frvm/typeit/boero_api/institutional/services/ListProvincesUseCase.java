package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.CountryNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.CountryRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.ProvinceRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.ProvinceListItemResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListProvincesUseCase {

  private final ProvinceRepository provinceRepository;
  private final CountryRepository countryRepository;

  public PaginatedResponse<ProvinceListItemResponse> execute(String search, Pageable pageable) {
    if (search != null && !search.isBlank()) {
      return PaginatedResponse.from(
          provinceRepository.searchByName(search, pageable).map(ProvinceListItemResponse::from));
    }
    return PaginatedResponse.from(
        provinceRepository.findAll(pageable).map(ProvinceListItemResponse::from));
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<ProvinceListItemResponse> executeByCountry(
      UUID countryId, String search, Pageable pageable) {
    if (!countryRepository.existsById(countryId)) {
      throw new CountryNotFoundException();
    }

    if (search != null && !search.isBlank()) {
      return PaginatedResponse.from(
          provinceRepository
              .searchByCountryIdAndName(countryId, search, pageable)
              .map(ProvinceListItemResponse::from));
    }

    return PaginatedResponse.from(
        provinceRepository
            .findByCountryId(countryId, pageable)
            .map(ProvinceListItemResponse::from));
  }
}
