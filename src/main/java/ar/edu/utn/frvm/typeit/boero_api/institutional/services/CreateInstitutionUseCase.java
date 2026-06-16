package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.CityNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.SlugAlreadyExistsException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.CityRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.InstitutionDetailResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.requests.CreateInstitutionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateInstitutionUseCase {

  private final CityRepository cityRepository;
  private final InstitutionRepository institutionRepository;

  @Transactional
  public InstitutionDetailResponse execute(CreateInstitutionRequest request) {
    if (institutionRepository.existsBySlug(request.slug())) {
      throw new SlugAlreadyExistsException();
    }

    var city = cityRepository.findById(request.cityId()).orElseThrow(CityNotFoundException::new);

    Institution institution =
        Institution.builder()
            .name(request.name())
            .slug(request.slug())
            .city(city)
            .street(request.street())
            .number(request.number())
            .neighborhood(request.neighborhood())
            .additionalInfo(request.additionalInfo())
            .phoneNumber(request.phoneNumber())
            .email(request.email())
            .active(true)
            .build();

    Institution saved = institutionRepository.save(institution);

    return institutionRepository
        .findWithCityAndProvinceById(saved.getId())
        .map(InstitutionDetailResponse::from)
        .orElseThrow();
  }
}
