package ar.edu.utn.frvm.typeit.boero_api.authorization.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermissionCode {
  INSTITUTION_PERSON_READ_OWN(
      "institution:person:read-own", PermissionScope.INSTITUTION, "Leer datos propios"),
  INSTITUTION_PERSON_UPDATE_OWN(
      "institution:person:update-own", PermissionScope.INSTITUTION, "Actualizar datos propios"),
  INSTITUTION_PERSON_READ_ANY(
      "institution:person:read-any",
      PermissionScope.INSTITUTION,
      "Leer datos de cualquier persona"),
  INSTITUTION_PERSON_CREATE(
      "institution:person:create", PermissionScope.INSTITUTION, "Crear personas institucionales"),
  INSTITUTION_PERSON_UPDATE_ANY(
      "institution:person:update-any",
      PermissionScope.INSTITUTION,
      "Actualizar datos de cualquier persona"),
  INSTITUTION_PERSON_DELETE(
      "institution:person:delete",
      PermissionScope.INSTITUTION,
      "Eliminar personas institucionales"),
  INSTITUTION_ROLE_ASSIGN(
      "institution:roles:assign", PermissionScope.INSTITUTION, "Asignar roles institucionales"),
  INSTITUTION_ROLE_REVOKE(
      "institution:roles:revoke", PermissionScope.INSTITUTION, "Revocar roles institucionales"),
  INSTITUTION_GRADES_ENTER(
      "institution:grades:enter", PermissionScope.INSTITUTION, "Cargar calificaciones"),
  INSTITUTION_GRADES_ENTER_FINAL(
      "institution:grades:enter-final",
      PermissionScope.INSTITUTION,
      "Cargar calificaciones finales");

  private final String code;
  private final PermissionScope scope;
  private final String description;

  public static PermissionCode fromCode(String code) {
    for (PermissionCode permission : values()) {
      if (permission.code.equals(code)) {
        return permission;
      }
    }
    throw new IllegalArgumentException("Código de permiso desconocido: " + code);
  }
}
