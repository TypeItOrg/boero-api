# Roles con alcance por trayecto

Cada asignación conserva un rol dentro de una institución y declara `accessScope`:

- `INSTITUTION`: `trainingPathIds: []`; incluye trayectos actuales y futuros.
- `TRAINING_PATHS`: IDs únicos, no vacíos y de la misma institución. Los permisos exclusivamente institucionales quedan inactivos.

Los permisos efectivos se unen **por permiso**. Una concesión de escritura en un trayecto no se combina con una concesión de lectura en otro para ampliar la escritura. `/auth/me` expone `permissionScopes`; el resumen `permissions` sirve para navegación y controles de entrada.

## Contratos

La asignación individual requiere `roleId`, `accessScope` y `trainingPathIds`. El reemplazo recibe `assignments`, con esos mismos campos por rol. Los clientes anteriores que escribían `roleIds` deben actualizarse junto con la API. Las respuestas incluyen nombres de los trayectos seleccionados y el catálogo indica qué permisos admiten alcance.

Las listas se filtran antes de contar y paginar. Un filtro fuera de alcance produce una colección vacía; un ID directo fuera de alcance produce 404. Las anotaciones siguen exigiendo el permiso de operación. Los accesos propios de estudiantes y docentes siguen sus relaciones y no conceden acceso a gestión.

Las opciones académicas tienen endpoints de proyecciones mínimas. Los formularios envían la operación correspondiente para que, por ejemplo, un permiso de lectura más amplio no amplíe las opciones de creación.

Los períodos devuelven solamente ofertas visibles, `limitedView` y capacidades de modificación. Crear, actualizar, cambiar estado o eliminar exige cobertura de todos los trayectos afectados; actualizar comprueba los anteriores y los propuestos. Los períodos vacíos requieren alcance institucional. Las respuestas de creación y actualización usan el permiso de esa operación sin conceder acceso adicional de lectura.

## Delegación y concurrencia

Asignar y ampliar requiere asignación; revocar y reducir requiere revocación. Se validan las concesiones efectivas contra la autoridad del actor. Editar los permisos de un rol reutilizado también verifica sus asignaciones. El bloqueo común de la institución serializa estas escrituras y la autoridad se vuelve a consultar dentro de la transacción.

Los cambios de asignación invalidan autorización después del commit y revocan sesiones, aunque sólo cambien los trayectos. Los snapshots usan una nueva versión de caché.

## Migraciones

La migración `20260920231705` ya fue registrada vacía y permanece vacía. El esquema se incorpora en `20260920232226`, con restricciones adicionales en `20260920233741` y `20260921001322`. No modificar ninguna de estas migraciones si está aplicada ni reparar sus checksums.

Para futuras migraciones durante desarrollo, detener el consumidor de Flyway antes de ejecutar `make migration <nombre>` y mantenerlo detenido hasta completar el archivo generado. Sincronizar el contenido completo y recién entonces iniciar la API. Así se evita que el watcher aplique el archivo vacío entre su generación y edición.

## Verificación focalizada

- `fastTest`: autorización, delegación, contratos HTTP de roles, revocación y capacidades de períodos.
- `RoleScopePostgresIntegrationTest`: paginación y búsquedas, separación por permiso, restricciones PostgreSQL, opciones por operación, cambios de alcance con caché y sesiones, delegación concurrente y acceso ajeno a inscripciones, adjuntos y cursadas.
- UI: validación de asignaciones, edición de roles, permisos por trayecto, opciones, colecciones y listas de espera; compilación de producción y lint de archivos modificados.

La comprobación ampliada en navegador del 21/09/2026 cubrió responsive, guardado y reversión de una asignación temporal autorizada, sesión limitada, acceso directo fuera de alcance, filtros, permisos institucionales inactivos y revocación de sesión. Ver [resultados y límites de la prueba](training-path-browser-qa-2026-09-21.md).
