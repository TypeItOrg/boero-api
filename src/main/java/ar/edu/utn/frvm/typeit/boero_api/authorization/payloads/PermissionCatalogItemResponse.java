package ar.edu.utn.frvm.typeit.boero_api.authorization.payloads;

import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionGroup;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionScope;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;

@Builder
public record PermissionCatalogItemResponse(
    String code,
    String description,
    PermissionScope scope,
    PermissionGroup group,
    String groupDisplayName,
    String groupDescription,
    boolean configurable,
    Set<String> requiredPermissions) {

  public static PermissionCatalogItemResponse from(PermissionCode permission) {
    return PermissionCatalogItemResponse.builder()
        .code(permission.getCode())
        .description(permission.getDescription())
        .scope(permission.getScope())
        .group(permission.getGroup())
        .groupDisplayName(permission.getGroup().getDisplayName())
        .groupDescription(permission.getGroup().getDescription())
        .configurable(permission.isConfigurable())
        .requiredPermissions(
            permission.requiredPermissions().stream()
                .map(PermissionCode::getCode)
                .collect(Collectors.toUnmodifiableSet()))
        .build();
  }
}
