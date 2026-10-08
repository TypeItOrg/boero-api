# Documentación de inscripciones

## Catálogo y asignaciones

Cada institución administra su propio catálogo. El nombre, las instrucciones generales y los formatos son compartidos. Cada trayecto define la exigencia, el orden, las instrucciones específicas y el estado de su asignación. Las instrucciones específicas complementan las generales.

Configurar un requisito no permite editar su definición compartida. La administración del catálogo requiere permiso institucional; cada asignación requiere actualización del trayecto correspondiente. La selección de opciones se autoriza por el trayecto, la creación de trayectos o la inscripción que solicita documentación, sin otorgar administración del catálogo.

Las asignaciones se guardan mediante cambios explícitos: omitir un trayecto no lo modifica. Quitar desactiva la asignación; reactivar recupera su identificador. Cambiar de documento requiere quitar la asignación anterior y seleccionar otro documento. Las ediciones compartidas y sus asignaciones pueden guardarse atómicamente. Revisiones independientes detectan cambios concurrentes; ante un conflicto es necesario recargar.

El formulario compartido muestra el número de trayectos asociados y borradores vinculados antes de permitir guardar. Los nombres y detalles de trayectos respetan el alcance de lectura. Crear desde el formulario de un trayecto guarda una definición independiente: cancelar posteriormente el trayecto no la elimina.

## Borradores y condiciones enviadas

Los borradores se sincronizan cuando cambia el catálogo o sus asignaciones y nuevamente antes de enviar. La operación utiliza el bloqueo institucional y lotes de 100, dentro de la misma transacción. Un fallo revierte el guardado completo.

La sincronización agrega, modifica, retira y reactiva requisitos, conservando sus identificadores, entregas y versiones. Los retirados no bloquean envío ni aprobaciones y no aparecen como pendientes. Su documentación e historial siguen disponibles para lectura, pero no se pueden modificar mientras estén retirados. Si no queda ningún requisito vigente, el paso documental desaparece y el detalle del borrador muestra el historial retirado por separado.

Desactivar una definición retira sus requisitos de los borradores. Reactivarla recupera los requisitos de las asignaciones que siguen activas. No afecta inscripciones enviadas.

Si cambian los formatos, el archivo existente no se elimina. Se indica que debe reemplazarse; en un borrador puede retirarse si es opcional. El envío identifica la documentación faltante o incompatible. La acción **Actualizar documentación** permite recuperar las condiciones vigentes mientras el formulario permanece abierto.

Al enviar se congelan las condiciones originales. Editar el catálogo no las modifica. Cada inscripción conserva archivos propios: compartir una definición nunca comparte entregas entre inscripciones.

## Documentación adicional

Administrativo y Autoridad institucional pueden solicitar documentación, según su alcance, en inscripciones enviadas o aprobadas provisionalmente. El pedido requiere motivo y documentos activos de la misma institución. No es necesario que estén asignados al trayecto.

Fecha, responsable y nombre del responsable se obtienen en el servidor. El pedido y sus condiciones son inmutables. No hay vencimientos, correos, edición ni cancelación de pedidos. Un documento ya requerido, incluso retirado, no puede agregarse otra vez; debe consultarse su requisito existente. Un duplicado invalida el pedido completo.

El pedido no cambia el estado, no reabre otros datos y no modifica las matrículas. Se permite cargar, observar, reemplazar y aceptar entregas progresivamente, incluso después del cierre del período. Las entregas aceptadas siguen siendo inmutables.

| Exigencia adicional | Condición |
|---|---|
| Obligatorio antes de aprobar (`AT_SUBMISSION`) | Debe estar aceptado antes de la próxima aprobación. |
| Obligatorio para confirmar (`BEFORE_CONFIRMATION`) | Debe estar aceptado antes de la aprobación definitiva. |
| Opcional | No bloquea aprobación. |

Un pedido obligatorio en una inscripción provisional bloquea la confirmación, sin revocar su admisión. Rechazadas, canceladas y aprobadas definitivamente permanecen cerradas. Si no se puede confirmar el resultado de un guardado, se debe recargar el detalle antes de reintentar.

## Compatibilidad y migración

La migración conserva identificadores, referencias de archivos y registros históricos. Unifica únicamente coincidencias exactas de institución, nombre, instrucciones y conjunto de formatos cuando no hay repetición de trayecto dentro del grupo. Los grupos ambiguos permanecen separados.

Los snapshots enviados no se reescriben ni reciben revisiones históricas ficticias. Una referencia histórica sin asignación existente recibe una definición inactiva propia. Las restricciones impiden duplicados y referencias entre instituciones o inscripciones distintas.

Los payloads distinguen definición, asignación y snapshot, así como requisito vigente/retirado y revisión de su archivo. Los requisitos identifican su origen original/adicional, pedido asociado y revisiones aplicadas; las revisiones históricas desconocidas son nulas.

## Verificación

La entrega usa compilación Java, Spotless, análisis estático, TypeScript completo, ESLint, Prettier y revisión del diff. Las comprobaciones de API, PostgreSQL e interfaz se realizan manualmente sobre base y almacenamiento aislados. No incluye nuevas suites ni ejecución de tests automatizados PostgreSQL. No implica publicación o despliegue.

La evidencia local y sus límites se detallan en [Verificación del catálogo documental](DOCUMENTATION-CATALOG-VERIFICATION.md).
