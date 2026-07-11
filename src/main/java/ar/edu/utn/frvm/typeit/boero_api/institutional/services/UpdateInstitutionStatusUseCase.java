package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.services.SessionRevocationService;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateInstitutionStatusUseCase {

  private final InstitutionRepository institutionRepository;
  private final SessionRevocationService sessionRevocationService;

  @Transactional
  public void execute(final UUID id, final boolean active) {
    final var institution =
        institutionRepository.findById(id).orElseThrow(InstitutionNotFoundException::new);

    institution.setActive(active);
    institutionRepository.save(institution);

    if (!active) {
      sessionRevocationService.revokeInstitutionalSessionsForInstitution(id);
    }
  }
}
