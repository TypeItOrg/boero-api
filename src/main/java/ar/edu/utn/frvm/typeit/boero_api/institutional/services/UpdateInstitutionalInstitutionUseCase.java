package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.CityNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.CityRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.InstitutionDetailResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.requests.UpdateInstitutionalInstitutionRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateInstitutionalInstitutionUseCase {

  private final CityRepository cityRepository;
  private final InstitutionRepository institutionRepository;

  @Transactional
  public InstitutionDetailResponse execute(
      final UUID id, final UpdateInstitutionalInstitutionRequest request) {
    final var institution =
        institutionRepository
            .findWithLocationById(id)
            .orElseThrow(InstitutionNotFoundException::new);

    final var city =
        cityRepository.findById(request.cityId()).orElseThrow(CityNotFoundException::new);

    institution.setName(request.name());
    institution.setCity(city);
    institution.setStreet(request.street());
    institution.setNumber(request.number());
    institution.setNeighborhood(request.neighborhood());
    institution.setAdditionalInfo(request.additionalInfo());
    institution.setPhoneNumber(request.phoneNumber());
    institution.setEmail(request.email());

    institutionRepository.save(institution);

    return InstitutionDetailResponse.from(institution);
  }
}
