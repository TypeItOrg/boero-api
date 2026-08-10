# Plan de implementación — Fase 1: Academic Core

## Estado

La Fase 1 está implementada en `boero-api`. Este documento reemplaza el plan
preliminar y describe las decisiones cerradas, el contrato implementado y los
criterios de validación.

## 1. Objetivo y alcance

El módulo académico permite configurar la estructura curricular de una
institución antes de incorporar cursadas, inscripciones, evaluaciones y
trayectoria de estudiantes.

Incluye:

- `AcademicYear`;
- `TrainingPath`;
- `StudyPlan`;
- `AcademicLevel`;
- `AcademicSpace`;
- `Instrument`;
- `StudyPlanSpace`;
- `Prerequisite`.

Quedan fuera de esta fase las ofertas académicas, comisiones, docentes,
horarios, inscripciones, evaluaciones, calificaciones y resultados de
estudiantes. `Instrument` queda como catálogo institucional independiente: no
se lo vincula todavía con espacios ni ofertas.

## 2. Decisiones cerradas

### Multi-tenancy y rutas

Todas las operaciones institucionales reciben `institutionId` en la ruta y
se exponen bajo `/api/v1/institutions/{institutionId}`. Esto es obligatorio
porque `InstitutionAccessAspect` valida la pertenencia del usuario antes de
ejecutar el controlador. Los casos de uso vuelven a filtrar por institución y
la base de datos protege las relaciones con claves foráneas compuestas.

### Identidad y nombres

- Las entidades usan UUIDv7 como en el resto del proyecto.
- Los nombres se guardan con espacios externos eliminados y espacios internos
  colapsados.
- La unicidad es insensible a mayúsculas y acentos mediante índices únicos
  funcionales en PostgreSQL.
- Las búsquedas usan la función `UNACCENT_LOWER` ya existente en el proyecto.

### Fechas y estados

Las fechas de vigencia son `LocalDate` y, cuando se informa una fecha de un
par, se exige la otra.

`AcademicYear`:

```text
PLANNED -> ACTIVE -> CLOSED
```

No se permite reabrir un ciclo cerrado. Solo puede existir un ciclo `ACTIVE`
por institución; además, la unicidad de `(institution_id, year)` se aplica en
la base de datos.

`StudyPlan`:

```text
DRAFT -> ACTIVE -> INACTIVE
```

Un plan activo requiere fecha de inicio, trayecto activo y al menos un espacio
incorporado. Un plan activo queda congelado: niveles, espacios del plan y
correlatividades solo se pueden modificar mientras el plan está en `DRAFT`.
Para pasar a `INACTIVE` se exige una fecha final no anterior al inicio.

`AcademicLevel` no tiene estado `active`; su alta, edición y eliminación se
permiten únicamente cuando el plan está en borrador.

### Catálogos y relaciones

- `AcademicSpace` se identifica por `(institution_id, name, type)`.
- Un espacio académico solo puede incorporarse una vez al mismo plan y nivel.
- El orden se mantiene único dentro del nivel; los espacios sin nivel tienen
  su propia secuencia.
- Los tipos de espacio son `SUBJECT`, `WORKSHOP`, `SEMINAR`, `PRACTICE` y
  `OTHER`.
- La obligatoriedad se simplifica a `REQUIRED` y `OPTIONAL`.
- La modalidad de aprobación es `PROMOTION`, `FINAL_EXAM` o
  `PROMOTION_OR_FINAL_EXAM`.
- Una correlatividad solo puede unir espacios del mismo plan, no puede apuntar
  a sí misma y es única por `(target, required, requirement_stage)`.
- Las correlatividades se validan como un único grafo dirigido. Las altas y
  modificaciones bloquean pesimistamente el plan antes de recorrer el grafo,
  evitando ciclos introducidos por concurrencia.

## 3. Persistencia

La migración
`20260725210059__add_academic_core.sql` crea las ocho tablas académicas,
restricciones, índices de búsqueda y relaciones tenant-aware.

Relaciones relevantes:

- `study_plans (institution_id, training_path_id)` referencia al trayecto de
  la misma institución;
- `study_plan_spaces (institution_id, study_plan_id)` referencia al plan de
  la misma institución;
- `study_plan_spaces (institution_id, academic_space_id)` referencia al
  catálogo de la misma institución;
- `study_plan_spaces (study_plan_id, academic_level_id)` referencia un nivel
  del mismo plan;
- `prerequisites (study_plan_id, target/required_study_plan_space_id)`
  referencia espacios del mismo plan.

No se debe editar ni renombrar esta migración después de aplicarla en un
entorno persistente. Las migraciones futuras se generan con `make migration
<name>` para obtener el timestamp UTC del repositorio.

## 4. Permisos

Se agregan al catálogo institucional:

```text
ACADEMIC_YEAR_READ, ACADEMIC_YEAR_CREATE, ACADEMIC_YEAR_UPDATE,
ACADEMIC_YEAR_STATUS_UPDATE
TRAINING_PATH_READ, TRAINING_PATH_CREATE, TRAINING_PATH_UPDATE,
TRAINING_PATH_STATUS_UPDATE
STUDY_PLAN_READ, STUDY_PLAN_CREATE, STUDY_PLAN_UPDATE,
STUDY_PLAN_STATUS_UPDATE, STUDY_PLAN_CURRICULUM_UPDATE
ACADEMIC_SPACE_READ, ACADEMIC_SPACE_CREATE, ACADEMIC_SPACE_UPDATE,
ACADEMIC_SPACE_STATUS_UPDATE
INSTRUMENT_READ, INSTRUMENT_CREATE, INSTRUMENT_UPDATE,
INSTRUMENT_STATUS_UPDATE
```

Todos pertenecen al grupo `ACADEMIC`. Los controladores requieren permisos,
no roles rígidos, y cada permiso de creación, actualización o cambio de
estado declara la lectura del recurso correspondiente como dependencia
mediante `requiredPermissions()`.

## 5. Contrato HTTP implementado

El prefijo mostrado es `/api/v1`; los controladores usan el versionado
`Version.V1` del proyecto.

### Ciclos lectivos

```text
POST  /institutions/{institutionId}/academic-years
GET   /institutions/{institutionId}/academic-years?status=&page=&size=
GET   /institutions/{institutionId}/academic-years/{academicYearId}
PUT   /institutions/{institutionId}/academic-years/{academicYearId}
PATCH /institutions/{institutionId}/academic-years/{academicYearId}/status
```

### Trayectos formativos

```text
POST  /institutions/{institutionId}/training-paths
GET   /institutions/{institutionId}/training-paths?search=&active=&page=&size=
GET   /institutions/{institutionId}/training-paths/{trainingPathId}
PUT   /institutions/{institutionId}/training-paths/{trainingPathId}
PATCH /institutions/{institutionId}/training-paths/{trainingPathId}/status
```

### Planes de estudio

```text
POST  /institutions/{institutionId}/training-paths/{trainingPathId}/study-plans
GET   /institutions/{institutionId}/training-paths/{trainingPathId}/study-plans
GET   /institutions/{institutionId}/study-plans?search=&status=&page=&size=
GET   /institutions/{institutionId}/study-plans/{studyPlanId}
PUT   /institutions/{institutionId}/study-plans/{studyPlanId}
PATCH /institutions/{institutionId}/study-plans/{studyPlanId}/status
GET   /institutions/{institutionId}/study-plans/{studyPlanId}/curriculum
```

### Niveles y espacios del plan

```text
POST   /institutions/{institutionId}/study-plans/{studyPlanId}/academic-levels
GET    /institutions/{institutionId}/study-plans/{studyPlanId}/academic-levels
GET    /institutions/{institutionId}/academic-levels/{academicLevelId}
PUT    /institutions/{institutionId}/academic-levels/{academicLevelId}
DELETE /institutions/{institutionId}/academic-levels/{academicLevelId}

POST   /institutions/{institutionId}/study-plans/{studyPlanId}/spaces
GET    /institutions/{institutionId}/study-plans/{studyPlanId}/spaces
GET    /institutions/{institutionId}/study-plan-spaces/{studyPlanSpaceId}
PUT    /institutions/{institutionId}/study-plan-spaces/{studyPlanSpaceId}
DELETE /institutions/{institutionId}/study-plan-spaces/{studyPlanSpaceId}
```

### Catálogos institucionales

```text
POST  /institutions/{institutionId}/academic-spaces
GET   /institutions/{institutionId}/academic-spaces?search=&active=&type=&page=&size=
GET   /institutions/{institutionId}/academic-spaces/{academicSpaceId}
PUT   /institutions/{institutionId}/academic-spaces/{academicSpaceId}
PATCH /institutions/{institutionId}/academic-spaces/{academicSpaceId}/status

POST  /institutions/{institutionId}/instruments
GET   /institutions/{institutionId}/instruments?search=&active=&page=&size=
GET   /institutions/{institutionId}/instruments/{instrumentId}
PUT   /institutions/{institutionId}/instruments/{instrumentId}
PATCH /institutions/{institutionId}/instruments/{instrumentId}/status
```

### Correlatividades

```text
POST   /institutions/{institutionId}/study-plan-spaces/{targetId}/prerequisites
GET    /institutions/{institutionId}/study-plan-spaces/{targetId}/prerequisites
PUT    /institutions/{institutionId}/prerequisites/{prerequisiteId}
DELETE /institutions/{institutionId}/prerequisites/{prerequisiteId}
```

Las colecciones paginadas usan `PaginatedResponse`; las colecciones internas
del currículo se devuelven ordenadas y no paginadas.

## 6. Organización del código

El dominio se encuentra en `ar.edu.utn.frvm.typeit.boero_api.academic` y se
divide en `controllers`, `entities`, `enums`, `exceptions`, `interfaces`,
`payloads`, `services` y `validation`.

Las entidades exponen fábricas y métodos de intención (`activate`, `deactivate`,
`updateDraft`, `transitionTo`, `ensureDraft`) en lugar de setters públicos.
La coordinación de repositorios, transacciones, locks y políticas entre
entidades vive en los casos de uso. Cada operación de aplicación se expone
mediante una clase específica (`Create...UseCase`, `Get...UseCase`,
`Update...UseCase`, `Delete...UseCase`) con un único método público `execute()`.
Las reglas reutilizadas por varias operaciones permanecen en colaboradores
internos del paquete y no se exponen a los controladores.

## 7. Estrategia de pruebas y aceptación

La implementación incluye:

- pruebas unitarias de transiciones de `AcademicYear` y congelamiento de
  `StudyPlan`;
- prueba unitaria de rechazo de un ciclo indirecto de correlatividades;
- prueba MVC del contrato nested de `AcademicYear`;
- integración PostgreSQL/Testcontainers que valida la migración, el modelo JPA,
  la unicidad de un ciclo activo y el rechazo de una relación que cruza
  instituciones;
- actualización del test del catálogo de permisos para los nuevos permisos.

La aceptación de la fase requiere que:

1. compile el proyecto con Java 21;
2. `spotlessCheck` no reporte cambios;
3. la suite completa de Gradle pase;
4. la migración sea validada por PostgreSQL y Hibernate;
5. no exista una ruta académica sin `institutionId`;
6. las operaciones de escritura respeten permisos, institución y estado del
   plan;
7. la base de datos rechace relaciones académicas cross-tenant aun si se
   omite la capa de aplicación.

Comandos de verificación:

```bash
./gradlew --no-daemon spotlessCheck
./gradlew --no-daemon test
git diff --check
```

## 8. Próxima fase

La Fase 2 puede incorporar ofertas por ciclo, instrumentos ofrecidos, cursadas
e inscripciones apoyándose en las relaciones y estados definidos aquí. Esas
entidades deben referenciar planes activos y conservar la inmutabilidad de la
estructura curricular histórica.
