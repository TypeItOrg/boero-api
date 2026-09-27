# Seeder de inscripciones: desarrollo y staging

Requiere Bash, una base con las migraciones aplicadas y una institución activa con
sus roles de sistema inicializados por la API. No crea instituciones ni forma
parte de Flyway o del arranque de la aplicación.

## Ejecución

Con el PostgreSQL del Compose local iniciado:

```sh
SEED_INSTITUTION=cboero SEED_YEAR=2026 make seed-demo
```

`cboero` es solo un ejemplo: usar el **código actual** de la institución destino.
No hay un UUID de institución incorporado al seeder. Si falta el código, no existe
o la institución está inactiva, la carga falla sin modificar datos.

Para una conexión directa (por ejemplo, staging), usar el cliente `psql` y las
variables de conexión estándar de PostgreSQL:

```sh
export PGHOST=host-del-entorno PGPORT=5432 PGDATABASE=nombre-de-la-base PGUSER=usuario
# Configurar PGPASSWORD o PGPASSFILE según la conexión del entorno.
SEED_TRANSPORT=psql SEED_INSTITUTION=codigo-destino SEED_YEAR=2026 make seed-demo
```

También se puede seleccionar otro Compose mediante `SEED_COMPOSE_FILE` y su
servicio con `SEED_DB_SERVICE`. Los comandos solo ejecutan la carga: no levantan
servicios, aplican migraciones ni despliegan la aplicación.

### Configuración

| Variable | Valor predeterminado / significado |
| --- | --- |
| `SEED_INSTITUTION` | Obligatoria: código de una institución existente y activa |
| `SEED_YEAR` | Año actual; fijarlo para reproducir la carga |
| `SEED_REFERENCE_DATE` | 1 de septiembre de `SEED_YEAR`; fecha de altas y referencia de edades |
| `SEED_TIMEZONE` | `America/Argentina/Cordoba`, para los límites del período |
| `SEED_DATASET` | `boero-2025`, nombre del directorio de datos |
| `SEED_TRANSPORT` | `compose` o `psql` |
| `SEED_COMPOSE_FILE` | Opcional; archivo Compose alternativo |
| `SEED_DB_SERVICE` | `postgres`, solo para transporte Compose |

La fecha de referencia debe pertenecer al ciclo elegido. No cambia el reloj de
la API: un período de un año pasado/futuro no pasa a estar vigente hoy por ejecutar
el seed. Si otro ciclo ya está activo, el nuevo se crea planificado sin cerrar el
existente; su activación se gestiona desde la aplicación.

Las variables están documentadas también en `scripts/seed/seed.env.example`.
El runner no lee archivos `.env` automáticamente. Para cargar una copia propia:

```sh
set -a
. /ruta/a/seed.env
set +a
make seed-demo
```

## Organización

- `scripts/seed/run.sh`: selecciona transporte y ensambla todos los archivos antes
  de conectar; un archivo faltante no puede terminar en una carga parcial.
- `scripts/seed/enrollment-demo-context.sql`: contexto de ejecución y transacción.
- `scripts/seed/enrollment-demo-ids.sql`: identidad estable por institución,
  dataset y clave lógica; compatibilidad con el registro anterior.
- `scripts/seed/datasets/boero-2025/catalog.sql`: catálogo académico y descripciones.
- `scripts/seed/datasets/boero-2025/scenario.sql`: personas ficticias, edades,
  credencial demo, docentes, instrumentos, turnos, cupos y grilla horaria.
- `scripts/seed/enrollment-demo-validate.sql`: validación de roles y consistencia
  del dataset antes de cargar entidades.
- `scripts/seed/enrollment-demo.sql`: carga de datos; sin borrado histórico.

Los nombres de materias y la edición curricular 2025 son datos explícitos del
catálogo, no configuración del entorno. No cambiar claves lógicas de registros
existentes para modificar sus nombres. Otro dataset debe respetar las tablas
`seed_*` del incluido y las restricciones de nombres/documentos del dominio:
seleccionar otro nombre de dataset no habilita duplicar catálogos incompatibles
dentro de una misma institución.

## Acceso

Contraseña compartida de desarrollo: **`BoeroDemo2026!`**.
Ingresar con el código actual de la institución y el documento correspondiente.

| Documento | Persona ficticia | Rol |
| --- | --- | --- |
| 99000001 | Lucía Ferreyra | Postulante infantil (4 años a la fecha de referencia) |
| 99000002 | Mateo Soria | Postulante adulto |
| 99000003 | Camila Roldán | Estudiante |
| 99000004 | Julián Pereyra | Estudiante |
| 99000005 | Valeria Molina | Docente |
| 99000006 | Gabriel Acosta | Docente |
| 99000007 | Mariana Suárez | Administrador institucional |
| 99000008 | Nicolás Herrera | Administrador institucional |

Los correos usan nombre y apellido (por ejemplo, `lucia.ferreyra@example.test`),
son ficticios y las cuentas
se crean verificadas. Los estudiantes tienen ficha y legajo. Los roles conservan
los permisos definidos por la aplicación, con alcance institucional.

## Oferta académica

La estructura proviene del sitio institucional y de las seis planillas 2025 de
CAVI, CAVB Instrumento/Canto, CAVA Instrumento/Canto y Profesorado de Música.
Incluye seis planes, veinte niveles y cien ubicaciones curriculares; el primer
nivel de cada propuesta cuenta con cursos para realizar la prueba.

Consultar [fuentes, materias y supuestos](enrollment-academic-sources.md). Los
horarios, cupos, docentes y fechas de inscripción son operativos para pruebas.
Las fuentes consultadas no incluyen el régimen de correlatividades.

## Recorrido manual sugerido

1. Entrar como Lucía e iniciar una solicitud en CAVI, nivel 1. En Escolaridad,
   informar nivel Inicial, una institución y «Sala de 4»; completar los datos
   del responsable y enviar.
2. Entrar como Mariana, revisar la solicitud y aprobar/asignar los cursos
   grupales y una franja individual disponible.
3. Volver a entrar como Lucía y consultar la inscripción resultante.
4. Con Mateo, iniciar una solicitud en CAVB Instrumento con Guitarra y recorrer
   los casos adultos de escolaridad: sin escolarización, estudios incompletos o
   nivel universitario con respuesta explícita sobre el secundario. Usar esa
   solicitud para recorrer rechazo o cancelación.
5. Con Camila y Julián, recorrer la inscripción de estudiantes existentes en
   primer año del Profesorado, seleccionando espacios de su plan.
6. Reejecutar el comando con la misma institución y ciclo conserva las personas,
   fechas de nacimiento y actividad generada durante la práctica.

No se precargan solicitudes, matrículas a cursos ni resultados académicos.
Los niveles superiores están completos en los planes; requieren crear cursos y
ampliar el período para probar su inscripción. No se simula un régimen de
correlatividades ausente de las fuentes.

## Reejecución y límites

Los identificadores son UUID v7 generados por PostgreSQL 18 (`uuidv7()`). El
registro `boero_demo.scoped_identifiers` conserva la correspondencia entre
institución, dataset, namespace y clave lógica. La misma carga en otra institución
obtiene sus propios UUID, aunque use los mismos documentos ficticios.

Si existe el registro anterior `boero_demo.identifiers`, el seeder determina su
institución a partir de las filas existentes y adopta sus UUID sin reescribir las
entidades. Si detecta propietarios mezclados, aborta en vez de asociarlos a la
institución equivocada. Incluir **todo el esquema `boero_demo` en los respaldos**;
no eliminarlo por separado ni volver a ejecutar versiones viejas del seeder.

La carga agrega faltantes y no actualiza registros existentes: conserva nombres,
fechas de nacimiento, contraseñas, estados y horarios. Las edades y los legajos se
calculan solo al crear personas/fichas. Cambiar la fecha de referencia no rejuvenece
usuarios existentes. Al cambiar de ciclo crea cursos y períodos nuevos, manteniendo
los anteriores. No se garantiza que horarios modificados manualmente sigan siendo
compatibles con nuevas clases: deben revisarse antes de ampliar ese escenario.

Un conflicto de unicidad, un catálogo incompatible o una referencia inválida
revierte toda la transacción; no se usa un `ON CONFLICT` genérico para ocultar esos
casos. Tampoco se reactivan registros dados de baja. Ante conflictos por registros
renombrados o eliminados, revisar el caso sin borrar el registro de IDs.
No usar `make reset-data` para repetir este escenario: elimina los volúmenes locales.

La refactorización portable tiene revisión estática y chequeo de sintaxis Bash.
No se ejecutó contra PostgreSQL ni staging, ni se ejecutaron tests automatizados;
la carga repetida, la adopción histórica y el recorrido de UI quedan pendientes
de verificación en ejecución.

## Limpiar las dos ofertas ficticias iniciales

La carga normal **ya no borra** las propuestas de la primera versión. Si todavía
existen y se decide retirarlas, respaldar primero y ejecutar por separado:

```sh
SEED_INSTITUTION=codigo-destino make seed-demo-cleanup-legacy
```

Solo considera los IDs registrados de esas dos ofertas. Conserva las comprobaciones
de estado, actividad de inscripción y claves foráneas: si cambiaron o tienen
referencias incompatibles, aborta. No elimina usuarios, no reinicia el escenario
actual y no se ejecuta automáticamente como parte de otra carga.

## Reparar identificadores de una carga anterior

El seed anterior convertía MD5 directamente a UUID, sin respetar versión ni
variante. El frontend rechaza esos valores. Conservar la validación de la UI.

1. Hacer un respaldo completo local con `pg_dump -Fc`, incluyendo todos los esquemas.
2. Ejecutar `SEED_INSTITUTION=codigo-destino make seed-demo-repair-ids` con el
   usuario propietario de PostgreSQL. Requiere poder cambiar `session_replication_role` dentro de la transacción.
3. Volver a iniciar sesión con las cuentas demo y abrir las pantallas desde sus
   listados: las URLs y los tokens anteriores contienen los IDs antiguos.
4. Ejecutar `SEED_INSTITUTION=codigo-destino make seed-demo` para completar el escenario.

La reparación reconoce los IDs de personas/cuentas demo y de la oferta oficial
por sus claves originales. Actualiza las columnas UUID, incluidas referencias e
historiales, sin recrear registros. Bloquea las tablas durante la operación y
revierte si encuentra referencias en texto/JSON/arrays, cambia cualquier dato
además de los IDs, o queda una clave foránea inválida. El registro de equivalencias
permite consultar el ID nuevo por `legacy_id`. No modifica migraciones Flyway.
Las dos ofertas ficticias anteriores a la oferta oficial requieren revisión si
siguen presentes; esta reparación no elimina esa actividad ni esos registros.
