package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.auth.interfaces.UserRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.LastInstitutionalAuthorityDeactivationException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.InstitutionPersonResolver;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.SessionRevocationService;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeactivatePersonAccessUseCase {

  private final InstitutionRepository institutionRepository;
  private final InstitutionPersonResolver institutionPersonResolver;
  private final UserRepository userRepository;
  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;
  private final SessionRevocationService sessionRevocationService;

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

    preventDeactivatingLastInstitutionalAuthority(institutionId, personId);

    userRepository
        .findByPerson_IdAndInstitution_Id(personId, institutionId)
        .ifPresent(
            user -> {
              if (!user.isEnabled()) {
                return;
              }
              user.setEnabled(false);
              userRepository.save(user);
            });

    sessionRevocationService.revokeInstitutionalSessionsForPerson(personId, institutionId);
  }

  private void preventDeactivatingLastInstitutionalAuthority(
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
      throw new LastInstitutionalAuthorityDeactivationException();
    }
  }
}
