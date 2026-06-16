package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.CityNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.SlugAlreadyExistsException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.CityRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.InstitutionDetailResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.requests.UpdateInstitutionRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateInstitutionUseCase {

  private final CityRepository cityRepository;
  private final InstitutionRepository institutionRepository;

  @Transactional
  public InstitutionDetailResponse execute(UUID id, UpdateInstitutionRequest request) {
    var institution =
        institutionRepository
            .findWithCityAndProvinceById(id)
            .orElseThrow(InstitutionNotFoundException::new);

    if (institutionRepository.existsBySlugAndIdNot(request.slug(), id)) {
      throw new SlugAlreadyExistsException();
    }

    var city = cityRepository.findById(request.cityId()).orElseThrow(CityNotFoundException::new);

    institution.setName(request.name());
    institution.setSlug(request.slug());
    institution.setCity(city);
    institution.setStreet(request.street());
    institution.setNumber(request.number());
    institution.setNeighborhood(request.neighborhood());
    institution.setAdditionalInfo(request.additionalInfo());
    institution.setPhoneNumber(request.phoneNumber());
    institution.setEmail(request.email());
    institution.setActive(request.active());

    institutionRepository.save(institution);

    return InstitutionDetailResponse.from(institution);
  }
}
