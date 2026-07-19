package ar.edu.utn.frvm.typeit.boero_api.authorization.services;

import ar.edu.utn.frvm.typeit.boero_api.authorization.entities.Role;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.RoleScope;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.exceptions.RoleNotAssignableException;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.PersonRoleAssignmentRepository;
import ar.edu.utn.frvm.typeit.boero_api.authorization.interfaces.RoleRepository;
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
  private final RoleRepository roleRepository;
  private final PersonRoleAssignmentRepository personRoleAssignmentRepository;
  private final AssignPersonSystemRoleUseCase assignPersonSystemRoleUseCase;

  @Transactional
  public PersonRoleResponse execute(
      UUID institutionId, UUID personId, AssignRoleRequest request, boolean allowAuthority) {
    Person person = institutionPersonResolver.requirePersonInInstitution(institutionId, personId);
    Role role =
        roleRepository
            .findByIdAndScopeAndInstitution_Id(
                request.roleId(), RoleScope.INSTITUTION, institutionId)
            .orElseThrow(RoleNotAssignableException::new);
    if (!allowAuthority
        && role.isSystem()
        && role.getCode().equals(SystemRoleCode.INSTITUTIONAL_AUTHORITY.name())) {
      throw new RoleNotAssignableException();
    }

    assignPersonSystemRoleUseCase.execute(person, role, true);

    return personRoleAssignmentRepository
        .findByPerson_IdAndRole_IdAndInstitution_Id(person.getId(), role.getId(), institutionId)
        .map(PersonRoleResponse::from)
        .orElseThrow();
  }
}
