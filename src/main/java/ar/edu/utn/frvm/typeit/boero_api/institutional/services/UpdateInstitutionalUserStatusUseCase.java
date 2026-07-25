package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.SessionRevocationService;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.CannotModifyOwnAccessException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.PersonNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateInstitutionalUserStatusUseCase {

  private final InstitutionRepository institutionRepository;
  private final UserRepository userRepository;
  private final SessionRevocationService sessionRevocationService;

  @Transactional
  public void execute(
      final UUID institutionId,
      final UUID actorPersonId,
      final UUID personId,
      final boolean enabled) {
    if (actorPersonId.equals(personId)) {
      throw new CannotModifyOwnAccessException();
    }

    institutionRepository
        .findByIdForUpdate(institutionId)
        .orElseThrow(InstitutionNotFoundException::new);
    final var user =
        userRepository
            .findByPerson_IdAndInstitution_Id(personId, institutionId)
            .orElseThrow(PersonNotFoundException::new);
    if (!user.updateAccess(enabled)) {
      return;
    }

    userRepository.save(user);
    if (!enabled) {
      sessionRevocationService.revokeInstitutionalSessionsForUser(user.getId());
    }
  }
}
