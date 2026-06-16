package ar.edu.utn.frvm.typeit.boero_api.authorization.payloads;

import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionScope;
import lombok.Builder;

@Builder
public record PermissionCatalogItemResponse(
    String code, String description, PermissionScope scope) {

  public static PermissionCatalogItemResponse from(PermissionCode permission) {
    return PermissionCatalogItemResponse.builder()
        .code(permission.getCode())
        .description(permission.getDescription())
        .scope(permission.getScope())
        .build();
  }
}
