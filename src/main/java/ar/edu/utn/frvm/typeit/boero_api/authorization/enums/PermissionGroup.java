package ar.edu.utn.frvm.typeit.boero_api.authorization.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermissionGroup {
  PEOPLE("Personas", "Acceso a datos y operaciones sobre usuarios institucionales."),
  ROLES("Roles y permisos", "Administración y asignación de responsabilidades."),
  GRADES("Calificaciones", "Carga y gestión de calificaciones académicas.");

  private final String displayName;
  private final String description;
}
