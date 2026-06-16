package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.payloads.AssignRoleRequest;
import ar.edu.utn.frvm.typeit.boero_api.authorization.payloads.PersonRoleResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignPersonRoleUseCase {

  private final InstitutionPersonResolver institutionPersonResolver;
  private final InstitutionalSystemRoleResolver institutionalSystemRoleResolver;
  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;
  private final AssignPersonSystemRoleUseCase assignPersonSystemRoleUseCase;

  @Transactional
  public PersonRoleResponse execute(UUID institutionId, UUID personId, AssignRoleRequest request) {
    Person person = institutionPersonResolver.requirePersonInInstitution(institutionId, personId);
    Role role = institutionalSystemRoleResolver.requireInstitutionalSystemRole(request.role());

    assignPersonSystemRoleUseCase.execute(person, request.role());

    return personRoleAssignmentRepository
        .findByPerson_IdAndRole_IdAndInstitution_Id(person.getId(), role.getId(), institutionId)
        .map(PersonRoleResponse::from)
        .orElseThrow();
  }
}
