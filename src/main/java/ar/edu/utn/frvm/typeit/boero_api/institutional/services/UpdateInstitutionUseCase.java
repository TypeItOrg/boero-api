package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.services.SessionRevocationService;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.CityNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.SlugAlreadyExistsException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.CityRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.InstitutionDetailResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.requests.UpdateInstitutionRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateInstitutionUseCase {

  private final CityRepository cityRepository;
  private final InstitutionRepository institutionRepository;
  private final SessionRevocationService sessionRevocationService;

  @Transactional
  public InstitutionDetailResponse execute(UUID id, UpdateInstitutionRequest request) {
    var institution =
        institutionRepository
            .findWithLocationById(id)
            .orElseThrow(InstitutionNotFoundException::new);

    if (institutionRepository.existsBySlugAndIdNot(request.slug(), id)) {
      throw new SlugAlreadyExistsException();
    }

    var city = cityRepository.findById(request.cityId()).orElseThrow(CityNotFoundException::new);

    boolean deactivating = institution.isActive() && !request.active();

    institution.rename(request.name());
    institution.changeSlug(request.slug());
    institution.updateLocation(
        city, request.street(), request.number(), request.neighborhood(), request.additionalInfo());
    institution.updateContact(request.phoneNumber(), request.email());
    institution.updateStatus(request.active());

    try {
      institutionRepository.save(institution);
      institutionRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      throw new SlugAlreadyExistsException();
    }

    if (deactivating) {
      sessionRevocationService.revokeInstitutionalSessionsForInstitution(id);
    }

    return InstitutionDetailResponse.from(institution);
  }
}
