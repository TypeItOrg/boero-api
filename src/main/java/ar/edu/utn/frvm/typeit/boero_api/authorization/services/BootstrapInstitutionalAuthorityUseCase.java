package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.payloads.PersonRoleResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BootstrapInstitutionalAuthorityUseCase {

  private final InstitutionPersonResolver institutionPersonResolver;
  private final InstitutionalSystemRoleResolver institutionalSystemRoleResolver;
  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;
  private final AssignPersonSystemRoleUseCase assignPersonSystemRoleUseCase;

  @Transactional
  public PersonRoleResponse execute(UUID institutionId, UUID personId) {
    Person person = institutionPersonResolver.requirePersonInInstitution(institutionId, personId);
    Role role =
        institutionalSystemRoleResolver.requireInstitutionalSystemRole(
            institutionId, SystemRoleCode.INSTITUTIONAL_AUTHORITY);

    assignPersonSystemRoleUseCase.execute(person, SystemRoleCode.INSTITUTIONAL_AUTHORITY);

    return personRoleAssignmentRepository
        .findByPerson_IdAndRole_IdAndInstitution_Id(person.getId(), role.getId(), institutionId)
        .map(PersonRoleResponse::from)
        .orElseThrow();
  }
}
