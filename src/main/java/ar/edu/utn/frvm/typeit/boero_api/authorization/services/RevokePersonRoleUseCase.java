package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.LastInstitutionalAuthorityRevocationException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InstitutionNotFoundException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.interfaces.InstitutionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RevokePersonRoleUseCase {

  private final InstitutionPersonResolver institutionPersonResolver;
  private final InstitutionalSystemRoleResolver institutionalSystemRoleResolver;
  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;
  private final RevokePersonSystemRoleUseCase revokePersonSystemRoleUseCase;
  private final InstitutionRepository institutionRepository;

  @Transactional
  public void execute(UUID institutionId, UUID personId, SystemRoleCode roleCode) {
    institutionRepository
        .findByIdForUpdate(institutionId)
        .orElseThrow(InstitutionNotFoundException::new);
    Person person = institutionPersonResolver.requirePersonInInstitution(institutionId, personId);
    Role role = institutionalSystemRoleResolver.requireInstitutionalSystemRole(roleCode);

    if (roleCode == SystemRoleCode.INSTITUTIONAL_AUTHORITY) {
      preventRevokingLastInstitutionalAuthority(institutionId, personId, role.getId());
    }

    revokePersonSystemRoleUseCase.execute(person, roleCode);
  }

  private void preventRevokingLastInstitutionalAuthority(
      UUID institutionId, UUID personId, UUID roleId) {
    boolean personHasAuthority =
        personRoleAssignmentRepository.existsByPerson_IdAndRole_IdAndInstitution_Id(
            personId, roleId, institutionId);
    if (!personHasAuthority) {
      return;
    }

    long authorityCount =
        personRoleAssignmentRepository.countActivePeopleByInstitutionIdAndRoleCode(
            institutionId, SystemRoleCode.INSTITUTIONAL_AUTHORITY.name());
    if (authorityCount <= 1) {
      throw new LastInstitutionalAuthorityRevocationException();
    }
  }
}
