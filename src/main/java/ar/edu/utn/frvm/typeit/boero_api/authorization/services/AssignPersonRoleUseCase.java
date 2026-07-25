package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.RoleNotAssignableException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.payloads.AssignRoleRequest;
import ar.edu.utn.frvm.typeit.boero_api.authorization.payloads.PersonRoleResponse;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignPersonRoleUseCase {

  private final InstitutionPersonResolver institutionPersonResolver;
  private final RoleRepository roleRepository;
  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;
  private final AssignPersonSystemRoleUseCase assignPersonSystemRoleUseCase;

  @Transactional
  public PersonRoleResponse execute(
      UUID institutionId, UUID personId, AssignRoleRequest request, boolean allowAuthority) {
    Person person =
        institutionPersonResolver.requirePersonInInstitutionForUpdate(institutionId, personId);
    Role role =
        roleRepository
            .findByIdAndScopeAndInstitution_Id(
                request.roleId(), RoleScope.INSTITUTION, institutionId)
            .orElseThrow(RoleNotAssignableException::new);
    if (!allowAuthority && role.isInstitutionalAuthority()) {
      throw new RoleNotAssignableException();
    }

    assignPersonSystemRoleUseCase.execute(person, role, true);

    PersonRoleResponse response =
        personRoleAssignmentRepository
            .findByPerson_IdAndRole_IdAndInstitution_Id(person.getId(), role.getId(), institutionId)
            .map(PersonRoleResponse::from)
            .orElseThrow();

    log.info(
        "[Role] Assigned successfully, personId: {}, roleId: {}, institutionId: {}",
        personId,
        role.getId(),
        institutionId);

    return response;
  }
}
