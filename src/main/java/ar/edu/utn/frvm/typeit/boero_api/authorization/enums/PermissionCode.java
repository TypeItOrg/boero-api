package ar.edu.utn.frvm.typeit.boero_api.authorization.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermissionCode {
  INSTITUTION_PERSON_READ_OWN(
      "institution:person:read-own",
      PermissionScope.INSTITUTION,
      PermissionGroup.PEOPLE,
      "Ver mi perfil"),
  INSTITUTION_PERSON_UPDATE_OWN(
      "institution:person:update-own",
      PermissionScope.INSTITUTION,
      PermissionGroup.PEOPLE,
      "Editar mi perfil"),
  INSTITUTION_PERSON_READ_ANY(
      "institution:person:read-any",
      PermissionScope.INSTITUTION,
      PermissionGroup.PEOPLE,
      "Ver usuarios"),
  INSTITUTION_PERSON_CREATE(
      "institution:person:create",
      PermissionScope.INSTITUTION,
      PermissionGroup.PEOPLE,
      "Crear usuarios"),
  INSTITUTION_PERSON_UPDATE_ANY(
      "institution:person:update-any",
      PermissionScope.INSTITUTION,
      PermissionGroup.PEOPLE,
      "Editar usuarios"),
  INSTITUTION_PERSON_DELETE(
      "institution:person:delete",
      PermissionScope.INSTITUTION,
      PermissionGroup.PEOPLE,
      "Eliminar usuarios"),
  INSTITUTION_USER_STATUS_UPDATE(
      "institution:users:update-status",
      PermissionScope.INSTITUTION,
      PermissionGroup.PEOPLE,
      "Activar o desactivar usuarios"),
  INSTITUTION_ROLE_ASSIGN(
      "institution:roles:assign",
      PermissionScope.INSTITUTION,
      PermissionGroup.ROLES,
      "Asignar roles institucionales"),
  INSTITUTION_ROLE_REVOKE(
      "institution:roles:revoke",
      PermissionScope.INSTITUTION,
      PermissionGroup.ROLES,
      "Revocar roles institucionales"),
  INSTITUTION_ROLE_READ(
      "institution:roles:read",
      PermissionScope.INSTITUTION,
      PermissionGroup.ROLES,
      "Consultar roles institucionales"),
  INSTITUTION_ROLE_CREATE(
      "institution:roles:create",
      PermissionScope.INSTITUTION,
      PermissionGroup.ROLES,
      "Crear roles institucionales"),
  INSTITUTION_ROLE_UPDATE(
      "institution:roles:update",
      PermissionScope.INSTITUTION,
      PermissionGroup.ROLES,
      "Actualizar roles institucionales"),
  INSTITUTION_ROLE_DELETE(
      "institution:roles:delete",
      PermissionScope.INSTITUTION,
      PermissionGroup.ROLES,
      "Eliminar roles institucionales"),
  INSTITUTION_GRADES_ENTER(
      "institution:grades:enter",
      PermissionScope.INSTITUTION,
      PermissionGroup.GRADES,
      "Cargar calificaciones"),
  INSTITUTION_GRADES_ENTER_FINAL(
      "institution:grades:enter-final",
      PermissionScope.INSTITUTION,
      PermissionGroup.GRADES,
      "Cargar calificaciones finales");

  private final String code;
  private final PermissionScope scope;
  private final PermissionGroup group;
  private final String description;

  public boolean isConfigurable() {
    return group != PermissionGroup.GRADES;
  }

  public static PermissionCode fromCode(String code) {
    for (PermissionCode permission : values()) {
      if (permission.code.equals(code)) {
        return permission;
      }
    }
    throw new IllegalArgumentException("Código de permiso desconocido: " + code);
  }
}
