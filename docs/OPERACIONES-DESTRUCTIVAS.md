# Ciclo de vida y operaciones destructivas académicas

## Política general

- `academic_years`, `training_paths`, `study_plans`, `academic_spaces` e `instruments`
  usan eliminación lógica mediante `deleted_at`.
- El estado operativo no representa eliminación. Restaurar conserva el estado previo.
- Las lecturas, búsquedas y catálogos excluyen eliminados por defecto. Los listados de
  administración pueden solicitar exclusivamente eliminados con `deleted=true`.
- Una eliminación o restauración queda registrada en `academic_lifecycle_events` con
  institución, recurso, actor, motivo opcional, request ID y fecha.
- No existe un endpoint de purga física para estos recursos raíz.

## Reglas por recurso

| Recurso | Estado requerido para eliminar | Condición adicional |
| --- | --- | --- |
| Ciclo lectivo | `PLANNED` | Ninguna |
| Trayecto formativo | Inactivo | No puede tener planes vigentes |
| Plan de estudio | `DRAFT` | La estructura curricular se conserva |
| Espacio académico | Inactivo | Las referencias históricas se conservan |
| Instrumento | Inactivo | Las referencias históricas se conservan |

Al restaurar un plan, su trayecto debe seguir vigente y activo. Los conflictos de
unicidad de una restauración se informan como conflicto funcional, sin sobrescribir el
recurso vigente.

## Autorización e integridad

- Eliminación y restauración tienen permisos separados por tipo de recurso.
- Los controladores de plataforma e institución mantienen fronteras independientes.
- El caso de uso resuelve el recurso por institución y lo bloquea de forma pesimista.
- Las invariantes existen tanto en el dominio como en restricciones de base de datos.
- La unicidad funcional aplica únicamente a filas con `deleted_at IS NULL`.

## Excepciones de eliminación física

Los nodos internos de una estructura curricular en borrador —prerrequisitos, espacios
del plan y niveles académicos— pueden eliminarse físicamente. Son datos de composición,
no raíces independientes, y sólo se modifican mientras el plan está en `DRAFT`.

Toda nueva excepción necesita una decisión explícita de producto, análisis de
referencias, permiso específico cuando corresponda y pruebas PostgreSQL que demuestren
que no quedan referencias huérfanas. Una cascada JPA o un botón de UI no constituyen esa
justificación.

## Pruebas necesarias

- Dominio: estados admitidos, rechazo de estados no eliminables e idempotencia.
- Aplicación: aislamiento por institución, dependencias, restauración y auditoría.
- Web: permisos de eliminación/restauración y contrato de errores funcionales.
- PostgreSQL: migración, restricciones, unicidad parcial, locks y referencias.
- Frontend: capacidades visibles, filtros vigentes/eliminados y acciones destructivas
  ubicadas al final.

Las garantías PostgreSQL se ejecutan mediante `./gradlew integrationTest`; las pruebas
unitarias y slices rápidos, mediante `./gradlew fastTest`.
