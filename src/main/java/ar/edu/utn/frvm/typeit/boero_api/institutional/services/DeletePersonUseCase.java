package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.LastInstitutionalAuthorityDeletionException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.InstitutionPersonResolver;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.SessionRevocationService;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
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
  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;
  private final SessionRevocationService sessionRevocationService;

  @Transactional
  public void execute(final UUID institutionId, final UUID personId) {
    Person person = institutionPersonResolver.requirePersonInInstitution(institutionId, personId);
    preventDeletingLastInstitutionalAuthority(institutionId, personId);
    if (person.isDeleted()) {
      return;
    }

    person.setDeleted(true);
    personRepository.save(person);
    userRepository
        .findByPerson_IdAndInstitution_Id(personId, institutionId)
        .ifPresent(
            user -> {
              user.setEnabled(false);
              userRepository.save(user);
            });
    sessionRevocationService.revokeInstitutionalSessionsForPerson(personId, institutionId);
  }

  private void preventDeletingLastInstitutionalAuthority(
      final UUID institutionId, final UUID personId) {
    boolean personHasAuthority =
        personRoleAssignmentRepository.existsByPerson_IdAndRole_CodeAndInstitution_Id(
            personId, SystemRoleCode.INSTITUTIONAL_AUTHORITY.name(), institutionId);
    if (!personHasAuthority) {
      return;
    }
    long authorityCount =
        personRoleAssignmentRepository.countActivePeopleByInstitutionIdAndRoleCode(
            institutionId, SystemRoleCode.INSTITUTIONAL_AUTHORITY.name());
    if (authorityCount <= 1) {
      throw new LastInstitutionalAuthorityDeletionException();
    }
  }
}
