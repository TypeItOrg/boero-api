package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.common.web.PaginatedResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.ProvinceNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.CityRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.ProvinceRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.CityListItemResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListCitiesUseCase {

  private final CityRepository cityRepository;
  private final ProvinceRepository provinceRepository;

  public PaginatedResponse<CityListItemResponse> execute(String search, Pageable pageable) {
    if (search != null && !search.isBlank()) {
      return PaginatedResponse.from(
          cityRepository.searchByNameOrProvince(search, pageable).map(CityListItemResponse::from));
    }

    return PaginatedResponse.from(cityRepository.findAll(pageable).map(CityListItemResponse::from));
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<CityListItemResponse> executeByProvince(
      UUID provinceId, String search, Pageable pageable) {
    if (!provinceRepository.existsById(provinceId)) throw new ProvinceNotFoundException();

    if (search != null && !search.isBlank()) {
      return PaginatedResponse.from(
          cityRepository
              .searchByProvinceAndName(provinceId, search, pageable)
              .map(CityListItemResponse::from));
    }

    return PaginatedResponse.from(
        cityRepository.findByProvinceId(provinceId, pageable).map(CityListItemResponse::from));
  }
}
