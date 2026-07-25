package ar.edu.utn.frvm.typeit.boero_api.authorization.payloads;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record InstitutionRoleRequest(
    @NotBlank(message = "El nombre es requerido.")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
        String name,
    Set<String> permissions) {

  public InstitutionRoleRequest {
    permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
  }

  @AssertTrue(message = "El catálogo contiene un permiso desconocido.")
  public boolean hasKnownPermissions() {
    return permissions.stream().allMatch(InstitutionRoleRequest::isKnownPermission);
  }

  private static boolean isKnownPermission(String code) {
    try {
      ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PermissionCode.fromCode(code);
      return true;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }
}
