package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.InstitutionPersonResolver;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReactivatePersonAccessUseCase {

  private final InstitutionRepository institutionRepository;
  private final InstitutionPersonResolver institutionPersonResolver;
  private final UserRepository userRepository;

  @Transactional
  public void execute(final UUID institutionId, final UUID personId) {
    institutionRepository
        .findByIdForUpdate(institutionId)
        .orElseThrow(InstitutionNotFoundException::new);
    final Person person =
        institutionPersonResolver.requirePersonInInstitution(institutionId, personId);

    if (person.isDeleted()) {
      return;
    }

    userRepository
        .findByPerson_IdAndInstitution_Id(personId, institutionId)
        .ifPresent(
            user -> {
              if (user.isEnabled()) {
                return;
              }
              user.setEnabled(true);
              userRepository.save(user);
            });
  }
}
