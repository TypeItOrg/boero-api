package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.auth.services.SessionRevocationService;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.InstitutionPersonResolver;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.PersonRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeletePersonUseCase {

  private final InstitutionPersonResolver institutionPersonResolver;
  private final PersonRepository personRepository;
  private final UserRepository userRepository;
  private final SessionRevocationService sessionRevocationService;
  private final InstitutionRepository institutionRepository;

  @Transactional
  public void execute(final UUID institutionId, final UUID personId) {
    institutionRepository
        .findByIdForUpdate(institutionId)
        .orElseThrow(InstitutionNotFoundException::new);
    Person person = institutionPersonResolver.requirePersonInInstitution(institutionId, personId);
    if (!person.delete()) {
      return;
    }

    personRepository.save(person);
    userRepository
        .findByPerson_IdAndInstitution_Id(personId, institutionId)
        .ifPresent(
            user -> {
              user.updateAccess(false);
              userRepository.save(user);
            });

    sessionRevocationService.revokeInstitutionalSessionsForPerson(personId, institutionId);
  }
}
