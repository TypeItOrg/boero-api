package ar.edu.utn.frvm.typeit.boero_api.authorization.controllers;

import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresAnyPermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresPermission;
import ar.edu.utn.frvm.typeit.boero_api.authorization.RequiresPlatformRole;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.SystemRoleCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.payloads.AssignRoleRequest;
import ar.edu.utn.frvm.typeit.boero_api.authorization.payloads.PersonRoleResponse;
import ar.edu.utn.frvm.typeit.boero_api.authorization.payloads.SystemRoleListResponse;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.AssignPersonRoleUseCase;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.BootstrapInstitutionalAuthorityUseCase;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.InstitutionalCallerGuard;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.ListPersonRolesUseCase;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.ListSystemRolesUseCase;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.RevokePersonRoleUseCase;
import ar.edu.utn.frvm.typeit.boero_api.common.web.Version;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoleController {

  private final InstitutionalCallerGuard institutionalCallerGuard;
  private final ListPersonRolesUseCase listPersonRolesUseCase;
  private final AssignPersonRoleUseCase assignPersonRoleUseCase;
  private final RevokePersonRoleUseCase revokePersonRoleUseCase;
  private final ListSystemRolesUseCase listSystemRolesUseCase;
  private final BootstrapInstitutionalAuthorityUseCase bootstrapInstitutionalAuthorityUseCase;

  @GetMapping(value = "/institutions/{institutionId}/people/{personId}/roles", version = Version.V1)
  @RequiresAnyPermission({
    PermissionCode.INSTITUTION_ROLE_ASSIGN,
    PermissionCode.INSTITUTION_ROLE_REVOKE
  })
  public List<PersonRoleResponse> listPersonRoles(
      @PathVariable UUID institutionId,
      @PathVariable UUID personId,
      Authentication authentication) {
    institutionalCallerGuard.ensureCallerBelongsToInstitution(authentication, institutionId);
    return listPersonRolesUseCase.execute(institutionId, personId);
  }

  @PostMapping(
      value = "/institutions/{institutionId}/people/{personId}/roles",
      version = Version.V1)
  @ResponseStatus(HttpStatus.CREATED)
  @RequiresPermission(PermissionCode.INSTITUTION_ROLE_ASSIGN)
  public PersonRoleResponse assignPersonRole(
      @PathVariable UUID institutionId,
      @PathVariable UUID personId,
      @Valid @RequestBody AssignRoleRequest request,
      Authentication authentication) {
    institutionalCallerGuard.ensureCallerBelongsToInstitution(authentication, institutionId);
    return assignPersonRoleUseCase.execute(institutionId, personId, request);
  }

  @DeleteMapping(
      value = "/institutions/{institutionId}/people/{personId}/roles/{roleCode}",
      version = Version.V1)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RequiresPermission(PermissionCode.INSTITUTION_ROLE_REVOKE)
  public void revokePersonRole(
      @PathVariable UUID institutionId,
      @PathVariable UUID personId,
      @PathVariable SystemRoleCode roleCode,
      Authentication authentication) {
    institutionalCallerGuard.ensureCallerBelongsToInstitution(authentication, institutionId);
    revokePersonRoleUseCase.execute(institutionId, personId, roleCode);
  }

  @GetMapping(value = "/roles/system", version = Version.V1)
  @RequiresPermission(PermissionCode.INSTITUTION_ROLE_ASSIGN)
  public SystemRoleListResponse listSystemRoles(Authentication authentication) {
    institutionalCallerGuard.ensureInstitutionalPrincipal(authentication);
    return SystemRoleListResponse.from(listSystemRolesUseCase.execute());
  }

  @PostMapping(
      value = "/platform/institutions/{institutionId}/authority/{personId}",
      version = Version.V1)
  @ResponseStatus(HttpStatus.CREATED)
  @RequiresPlatformRole(PlatformRoleCode.PLATFORM_ADMIN)
  public PersonRoleResponse bootstrapInstitutionalAuthority(
      @PathVariable UUID institutionId, @PathVariable UUID personId) {
    return bootstrapInstitutionalAuthorityUseCase.execute(institutionId, personId);
  }
}
