# Escenario local de inscripciones

Con la API de desarrollo iniciada y sus migraciones y roles inicializados, ejecutar:

```sh
make seed-demo
```

El comando carga `scripts/seed/enrollment-demo.sql` en el servicio PostgreSQL del
Compose local. No forma parte de Flyway ni se ejecuta al iniciar la aplicación.
Usa el identificador original del Conservatorio Felipe Boero y conserva su código
editable (en la base local al cargarlo: `cboero`; en el seed original: `felipe-boero`).
No utilizar estas credenciales públicas en entornos productivos.

## Acceso

Contraseña compartida de desarrollo: **`BoeroDemo2026!`**.
Ingresar con el código actual de la institución y el documento correspondiente.

| Documento | Persona ficticia | Rol |
| --- | --- | --- |
| 99000001 | Lucía Ferreyra | Postulante infantil (4 años en 2026) |
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
6. Reejecutar `make seed-demo` actualiza las fechas de nacimiento de estas
   identidades deterministas, sin recrear solicitudes ni alterar sus estados.

No se precargan solicitudes, matrículas a cursos ni resultados académicos.
Los niveles superiores están completos en los planes; requieren crear cursos y
ampliar el período para probar su inscripción. No se simula un régimen de
correlatividades ausente de las fuentes.

## Reejecución y límites

Los identificadores son UUID v7 generados por PostgreSQL 18 (`uuidv7()`).
El registro local `boero_demo.identifiers` conserva la correspondencia entre cada
clave del fixture y su UUID: las reejecuciones reutilizan los IDs y conservan los
registros ya creados. Incluir ese esquema en los respaldos; no eliminarlo por separado.
Volver a ejecutar agrega lo que falta; no reinicia contraseñas, solicitudes,
estados, horarios ni inscripciones modificadas durante la práctica. Al cambiar de
año crea cursos y períodos para el nuevo ciclo, manteniendo los anteriores.
Un conflicto de datos o restricción revierte toda la carga; la sustitución de la
primera oferta ficticia está acotada a sus identificadores y se rechaza si tiene
actividad de inscripción. No usar `make reset-data` para repetir este escenario:
elimina los volúmenes locales.

La carga se verificó en PostgreSQL local. No se ejecutaron tests automatizados ni
se verificó todavía el recorrido completo por la interfaz.

## Reparar identificadores de una carga anterior

El seed anterior convertía MD5 directamente a UUID, sin respetar versión ni
variante. El frontend rechaza esos valores. Conservar la validación de la UI.

1. Hacer un respaldo completo local con `pg_dump -Fc`, incluyendo todos los esquemas.
2. Ejecutar `make seed-demo-repair-ids` con el usuario propietario del PostgreSQL
   local. Requiere poder cambiar `session_replication_role` dentro de la transacción.
3. Volver a iniciar sesión con las cuentas demo y abrir las pantallas desde sus
   listados: las URLs y los tokens anteriores contienen los IDs antiguos.
4. Ejecutar `make seed-demo` cuando se necesite completar el escenario.

La reparación reconoce los IDs de personas/cuentas demo y de la oferta oficial
por sus claves originales. Actualiza las columnas UUID, incluidas referencias e
historiales, sin recrear registros. Bloquea las tablas durante la operación y
revierte si encuentra referencias en texto/JSON/arrays, cambia cualquier dato
además de los IDs, o queda una clave foránea inválida. El registro de equivalencias
permite consultar el ID nuevo por `legacy_id`. No modifica migraciones Flyway.
Las dos ofertas ficticias anteriores a la oferta oficial requieren revisión si
siguen presentes; esta reparación no elimina esa actividad ni esos registros.
