package ar.edu.utn.frvm.typeit.boero_api.authorization.enums;

import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermissionCode {
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
      "Asignar roles"),
  INSTITUTION_ROLE_REVOKE(
      "institution:roles:revoke",
      PermissionScope.INSTITUTION,
      PermissionGroup.ROLES,
      "Revocar roles"),
  INSTITUTION_ROLE_READ(
      "institution:roles:read", PermissionScope.INSTITUTION, PermissionGroup.ROLES, "Ver roles"),
  INSTITUTION_ROLE_CREATE(
      "institution:roles:create",
      PermissionScope.INSTITUTION,
      PermissionGroup.ROLES,
      "Crear roles"),
  INSTITUTION_ROLE_UPDATE(
      "institution:roles:update",
      PermissionScope.INSTITUTION,
      PermissionGroup.ROLES,
      "Actualizar roles"),
  INSTITUTION_ROLE_DELETE(
      "institution:roles:delete",
      PermissionScope.INSTITUTION,
      PermissionGroup.ROLES,
      "Eliminar roles"),
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

  public Set<PermissionCode> requiredPermissions() {
    return switch (this) {
      case INSTITUTION_PERSON_CREATE,
          INSTITUTION_PERSON_UPDATE_ANY,
          INSTITUTION_PERSON_DELETE,
          INSTITUTION_USER_STATUS_UPDATE ->
          Set.of(INSTITUTION_PERSON_READ_ANY);
      case INSTITUTION_ROLE_ASSIGN, INSTITUTION_ROLE_REVOKE ->
          Set.of(INSTITUTION_PERSON_READ_ANY, INSTITUTION_ROLE_READ);
      case INSTITUTION_ROLE_CREATE, INSTITUTION_ROLE_UPDATE, INSTITUTION_ROLE_DELETE ->
          Set.of(INSTITUTION_ROLE_READ);
      default -> Set.of();
    };
  }

  public static Set<PermissionCode> withRequiredPermissions(Set<PermissionCode> permissions) {
    EnumSet<PermissionCode> expanded =
        permissions.isEmpty() ? EnumSet.noneOf(PermissionCode.class) : EnumSet.copyOf(permissions);
    boolean changed;
    do {
      changed =
          expanded.addAll(
              expanded.stream()
                  .flatMap(permission -> permission.requiredPermissions().stream())
                  .toList());
    } while (changed);
    return Set.copyOf(expanded);
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
