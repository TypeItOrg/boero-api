# Prueba en navegador — 21/09/2026

Entorno local de Boero, navegador integrado. Sesión administradora en `localhost:3000`; sesión independiente de estudiante en `qa.localhost:3000`. No se modificó configuración para habilitar la segunda sesión. El intento inicial con `127.0.0.1` mostró HTML pero no activó los controles; Next admite `*.localhost` en desarrollo.

## Resultados

| Escenario | Resultado observado |
| --- | --- |
| Responsive del formulario de asignaciones | Anchos CSS efectivos de 320, 390 y 768 px; sin desbordamiento horizontal. Los nombres largos se ajustan y los botones finales pueden alcanzarse sin quedar tapados por la navegación. |
| Catálogo de roles en escritorio | Verificado a 1440 × 900 px; sin desbordamiento horizontal. |
| Crear rol vacío | Muestra «Ingresá un nombre». |
| Rol sin permisos aplicables a trayectos | Estudiante tiene deshabilitada la opción de alcance limitado. |
| Selección de trayectos | Agregar dos trayectos, quitar uno y conservar el restante funciona. La selección vacía muestra la advertencia correspondiente. |
| Permisos institucionales inactivos | La asignación limitada informa que «Ver usuarios» queda inactivo. |
| Guardar asignación | Con autorización explícita se asignó temporalmente a Ana un rol de lectura de trayectos y solicitudes, limitado a CAVI. Se recargó el formulario y conservó el alcance y el nombre seleccionado. |
| Sesión limitada | El listado de trayectos muestra solamente CAVI y un total de 1; el administrador ve 2. |
| URL directa de otro trayecto | Muestra «Página no encontrada». |
| Solicitudes y filtro | Cargan las solicitudes de CAVI. El filtro sólo ofrece CAVI. Un parámetro de URL con el otro trayecto devuelve una colección vacía. |
| Permiso global inactivo | La navegación no muestra Usuarios y abrir `/people` directamente devuelve «Sin acceso». |
| Acceso propio | «Mis materias» sigue mostrando las cursadas propias, incluso las del otro trayecto. |
| Períodos | El administrador puede cargar opciones de planes, seleccionar un plan y consultar y seleccionar sus niveles. Se descartó el borrador. |
| Reversión y sesiones | Se quitó la asignación por la UI. Ana volvió a tener únicamente Estudiante. Al recargar su sesión fue redirigida a iniciar sesión. |
| Limpieza | Se eliminó únicamente la definición temporal de rol y sus tres permisos, después de comprobar que no tenía asignaciones. El catálogo volvió a mostrar los 4 roles originales. |

## Observaciones menores

- El estado vacío de solicitudes usa «Aún no hay solicitudes de inscripción» incluso cuando lo provoca un filtro. Sería más preciso diferenciar la ausencia de registros de la ausencia de resultados.
- Al introducir manualmente un ID de trayecto fuera del alcance, el selector queda sin etiqueta visible. La colección vacía y la protección funcionan; falta un tratamiento visual del filtro no disponible.
- Next emitió una advertencia de rendimiento por el logo detectado como LCP sin carga prioritaria. No se observaron excepciones JavaScript en los recorridos funcionales probados.

## Límites de esta prueba

No es una certificación exhaustiva de todos los módulos. No se guardaron períodos, aprobaciones, bajas ni resultados académicos. No se reprodujeron en navegador carreras concurrentes ni combinaciones de múltiples roles con permisos diferentes. Los datos locales no tienen solicitudes en el segundo trayecto, por lo que la prueba de solicitudes comprobó filtro y navegación, complementando las pruebas de aislamiento con datos persistidos de integración.

No hubo cambios de código, commits ni despliegue. La sesión administradora permaneció abierta, se cerró la sesión de prueba revocada y se restauró el viewport.

## Correcciones posteriores solicitadas

Se resolvieron las tres observaciones de UI:

- El estado vacío considera estado, trayecto y períodos abiertos, tanto en la página institucional como en la de plataforma. El mensaje habla de los filtros seleccionados.
- Un trayecto ausente del catálogo visible muestra «Trayecto no disponible». Conserva el ID del filtro y los resultados vacíos; seleccionar «Todos los trayectos» elimina el filtro normalmente.
- El logo del inicio institucional y el encabezado móvil de autenticación usan carga inmediata (`loading="eager"`).

Verificación posterior: formato, ESLint y TypeScript correctos. En navegador se comprobaron el trayecto válido sin solicitudes, un ID ausente de las opciones, el retorno a los cuatro resultados originales y el atributo de carga del logo. A 320 px, `scrollWidth` fue 314 px, sin desbordamiento. No aparecieron errores ni advertencias de consola en el recorrido de comprobación. No se modificaron permisos ni datos de usuarios para esta corrección.

## Ampliación de breakpoints

Se inspeccionaron capturas y dimensiones del DOM en el navegador integrado, con anchos CSS efectivos (sin cambiar el zoom):

| Viewport | Formulario de usuario y roles | Listado de solicitudes |
| --- | --- | --- |
| 360 × 800 | Correcto | Correcto |
| 375 × 812 | Correcto | Correcto |
| 414 × 896 | Correcto | Correcto |
| 640 × 900 | Correcto | Correcto |
| 768 × 1024 | Correcto | Correcto |
| 820 × 1180 | Correcto | Correcto |
| 1024 × 768 | Correcto | Correcto |
| 1280 × 800 | Correcto | Correcto |
| 1440 × 900 | Correcto | Correcto |
| 1920 × 1080 | Correcto | Correcto |

En todos los tamaños, el documento permaneció dentro del ancho visible. Los controles del formulario no salieron de la pantalla y se alcanzaron sus acciones finales. La tabla mantiene desplazamiento horizontal interno cuando sus columnas no caben; no provoca desplazamiento horizontal de la página. El formulario se comprobó con la asignación original de Estudiante, sin volver a conceder el rol temporal.

También se revisó el detalle de una solicitud a 360, 375, 767, 768, 1024 y 1440 px, incluyendo el cambio de navegación móvil a lateral entre 767 y 768 px. A 375 px se abrió el menú de acciones y se navegó al detalle. A 360 px se abrió el panel de navegación móvil y se accedió al listado de períodos. El formulario de nuevo período se inspeccionó a 360, 640, 768 y 1024 px y se canceló sin guardar.

No se encontraron nuevos defectos visuales en estas vistas ni errores o advertencias en la consola consultada al finalizar. Se restauró el viewport. Esta ampliación no modificó código de aplicación ni datos; complementa las pruebas funcionales anteriores y no equivale a una comprobación de todos los módulos o dispositivos físicos.
