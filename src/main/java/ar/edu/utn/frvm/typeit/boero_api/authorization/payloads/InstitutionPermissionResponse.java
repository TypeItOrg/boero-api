package ar.edu.utn.frvm.typeit.boero_api.authorization.payloads;

import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import java.util.Set;
import java.util.stream.Collectors;

public record InstitutionPermissionResponse(
    String code, String description, boolean grantable, Set<String> requiredPermissions) {

  public static InstitutionPermissionResponse from(
      PermissionCode permission, Set<PermissionCode> actorPermissions) {
    return new InstitutionPermissionResponse(
        permission.getCode(),
        permission.getDescription(),
        actorPermissions.contains(permission),
        permission.requiredPermissions().stream()
            .map(PermissionCode::getCode)
            .collect(Collectors.toUnmodifiableSet()));
  }
}
