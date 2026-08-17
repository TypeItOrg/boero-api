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
      "Cargar calificaciones finales"),
  INSTITUTION_READ(
      "institution:read",
      PermissionScope.INSTITUTION,
      PermissionGroup.INSTITUTION,
      "Ver detalles de la institución"),
  INSTITUTION_UPDATE(
      "institution:update",
      PermissionScope.INSTITUTION,
      PermissionGroup.INSTITUTION,
      "Editar detalles de la institución"),
  ACADEMIC_YEAR_READ(
      "institution:academic-year:read",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Ver ciclos lectivos"),
  ACADEMIC_YEAR_CREATE(
      "institution:academic-year:create",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Crear ciclos lectivos"),
  ACADEMIC_YEAR_UPDATE(
      "institution:academic-year:update",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Editar ciclos lectivos"),
  ACADEMIC_YEAR_STATUS_UPDATE(
      "institution:academic-year:update-status",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Cambiar estado de ciclos lectivos"),
  ACADEMIC_YEAR_DELETE(
      "institution:academic-year:delete",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Eliminar ciclos lectivos"),
  ACADEMIC_YEAR_RESTORE(
      "institution:academic-year:restore",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Restaurar ciclos lectivos"),
  TRAINING_PATH_READ(
      "institution:training-path:read",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Ver trayectos formativos"),
  TRAINING_PATH_CREATE(
      "institution:training-path:create",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Crear trayectos formativos"),
  TRAINING_PATH_UPDATE(
      "institution:training-path:update",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Editar trayectos formativos"),
  TRAINING_PATH_STATUS_UPDATE(
      "institution:training-path:update-status",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Activar o desactivar trayectos formativos"),
  TRAINING_PATH_DELETE(
      "institution:training-path:delete",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Eliminar trayectos formativos"),
  TRAINING_PATH_RESTORE(
      "institution:training-path:restore",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Restaurar trayectos formativos"),
  STUDY_PLAN_READ(
      "institution:study-plan:read",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Ver planes de estudio"),
  STUDY_PLAN_CREATE(
      "institution:study-plan:create",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Crear planes de estudio"),
  STUDY_PLAN_UPDATE(
      "institution:study-plan:update",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Editar planes de estudio"),
  STUDY_PLAN_STATUS_UPDATE(
      "institution:study-plan:update-status",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Activar o desactivar planes de estudio"),
  STUDY_PLAN_CURRICULUM_UPDATE(
      "institution:study-plan:curriculum-update",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Editar estructura curricular"),
  STUDY_PLAN_DELETE(
      "institution:study-plan:delete",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Eliminar planes de estudio"),
  STUDY_PLAN_RESTORE(
      "institution:study-plan:restore",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Restaurar planes de estudio"),
  ACADEMIC_SPACE_READ(
      "institution:academic-space:read",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Ver espacios académicos"),
  ACADEMIC_SPACE_CREATE(
      "institution:academic-space:create",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Crear espacios académicos"),
  ACADEMIC_SPACE_UPDATE(
      "institution:academic-space:update",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Editar espacios académicos"),
  ACADEMIC_SPACE_STATUS_UPDATE(
      "institution:academic-space:update-status",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Activar o desactivar espacios académicos"),
  ACADEMIC_SPACE_DELETE(
      "institution:academic-space:delete",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Eliminar espacios académicos"),
  ACADEMIC_SPACE_RESTORE(
      "institution:academic-space:restore",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Restaurar espacios académicos"),
  INSTRUMENT_READ(
      "institution:instrument:read",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Ver instrumentos"),
  INSTRUMENT_CREATE(
      "institution:instrument:create",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Crear instrumentos"),
  INSTRUMENT_UPDATE(
      "institution:instrument:update",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Editar instrumentos"),
  INSTRUMENT_STATUS_UPDATE(
      "institution:instrument:update-status",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Activar o desactivar instrumentos"),
  INSTRUMENT_DELETE(
      "institution:instrument:delete",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Eliminar instrumentos"),
  INSTRUMENT_RESTORE(
      "institution:instrument:restore",
      PermissionScope.INSTITUTION,
      PermissionGroup.ACADEMIC,
      "Restaurar instrumentos");

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
      case INSTITUTION_UPDATE -> Set.of(INSTITUTION_READ);
      case ACADEMIC_YEAR_CREATE,
          ACADEMIC_YEAR_UPDATE,
          ACADEMIC_YEAR_STATUS_UPDATE,
          ACADEMIC_YEAR_DELETE,
          ACADEMIC_YEAR_RESTORE ->
          Set.of(ACADEMIC_YEAR_READ);
      case TRAINING_PATH_CREATE,
          TRAINING_PATH_UPDATE,
          TRAINING_PATH_STATUS_UPDATE,
          TRAINING_PATH_DELETE,
          TRAINING_PATH_RESTORE ->
          Set.of(TRAINING_PATH_READ);
      case STUDY_PLAN_CREATE,
          STUDY_PLAN_UPDATE,
          STUDY_PLAN_STATUS_UPDATE,
          STUDY_PLAN_CURRICULUM_UPDATE,
          STUDY_PLAN_DELETE,
          STUDY_PLAN_RESTORE ->
          Set.of(STUDY_PLAN_READ);
      case ACADEMIC_SPACE_CREATE,
          ACADEMIC_SPACE_UPDATE,
          ACADEMIC_SPACE_STATUS_UPDATE,
          ACADEMIC_SPACE_DELETE,
          ACADEMIC_SPACE_RESTORE ->
          Set.of(ACADEMIC_SPACE_READ);
      case INSTRUMENT_CREATE,
          INSTRUMENT_UPDATE,
          INSTRUMENT_STATUS_UPDATE,
          INSTRUMENT_DELETE,
          INSTRUMENT_RESTORE ->
          Set.of(INSTRUMENT_READ);
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
