# Estado de Seguridad, Aislamiento y Concurrencia

## Propósito

Este documento describe las garantías que están implementadas actualmente. Sirve como contrato técnico para revisar cambios, diseñar endpoints y coordinar trabajo entre backend, frontend y QA.

No describe funcionalidades futuras como si ya existieran. Cuando una garantía dependa de una convención de implementación, se indica explícitamente.

## Estado general

| Área | Estado | Garantía principal |
|---|---|---|
| Autenticación dual | Implementado | Usuarios institucionales y cuentas plataforma tienen credenciales, sesiones y tokens separados |
| Aislamiento institucional | Implementado | Un principal institucional no puede operar sobre un `institutionId` diferente al de su JWT |
| Rotación de refresh tokens | Implementado | Rotación por familia, lock pesimista y detección de reutilización |
| Revocación por institución inactiva | Implementado | Se cierran sesiones y login, refresh y access token vuelven a comprobar el estado |
| Autorización dinámica | Implementado | Roles y permisos se resuelven fuera del JWT y se cachean con invalidación explícita |
| Última autoridad institucional | Implementado | Revocación y eliminación usan lock institucional y rechazan dejar cero autoridades |
| Integridad entre tenants | Implementado | Entidades críticas validan coherencia de institución y las consultas incluyen el tenant |
| Conflictos concurrentes de unicidad | Implementado | Las restricciones se fuerzan con `flush()` y se convierten a excepciones HTTP 409 |
| Pruebas PostgreSQL de refresh | Implementado | Se prueba commit tras reutilización y rotación concurrente con Testcontainers |
| Migraciones de esquema | Implementado | Flyway versiona las transiciones y Hibernate valida el resultado contra las entidades |

## Invariantes que no deben romperse

### Identidad institucional

- Un `User` pertenece a una sola `Institution`.
- La `Person` asociada al usuario debe pertenecer a esa misma institución.
- Una `PersonRoleAssignment` debe referenciar una persona de la institución indicada.
- Un rol institucional específico solo puede asignarse dentro de su institución; los roles globales de sistema pueden aplicarse a cualquier institución.
- Las búsquedas y mutaciones de personas reciben simultáneamente `personId` e `institutionId` cuando el recurso está bajo un tenant.

### Habilitación de una cuenta

Un usuario institucional es utilizable solamente si:

```text
user.enabled
AND institution.active
AND NOT person.deleted
```

Esta regla se aplica en autenticación, refresh y validación de sesión. No debe reemplazarse por una comprobación aislada de `UserSession.active`.

### Refresh tokens

- El token crudo solo se entrega al cliente.
- La base almacena SHA-256 del token.
- Cada token pertenece a una sesión y familia.
- La fila se bloquea antes de rotarla.
- Presentar un token revocado se considera reutilización, no una expiración normal.
- La reutilización revoca toda la familia y las sesiones vinculadas.
- Las revocaciones deben confirmarse aunque el endpoint termine respondiendo `401`.

### Autorización

- Los permisos no se confían al contenido del JWT.
- Un cambio de rol debe invalidar las entradas correspondientes de caché.
- `PLATFORM_ADMIN` administra recursos globales mediante `/api/v1/admin/**`; las cuentas plataforma no usan las rutas institucionales como acceso implícito.
- Crear una persona con rol privilegiado requiere permiso de asignación o administración de plataforma.
- Nunca debe aceptarse un `institutionId` del body o path sin contrastarlo con el principal o con una guarda equivalente.

## Capas de protección

```text
Request
  → JwtAuthenticationFilter
      → firma, expiración y blacklist
      → sesión utilizable
  → anotación de rol o permiso
  → @RequiresInstitutionAccess
  → controller
  → use case transaccional
  → repositorio filtrado por institución
  → restricciones y claves foráneas
```

Las capas son complementarias. La validación en controller impide una operación ilegítima temprano; el filtro por institución en persistencia evita que un error en una capa superior termine exponiendo otro tenant; las restricciones preservan integridad ante concurrencia o errores de código.

## Concurrencia

### Rotación de refresh

Los repositorios de refresh token usan `PESSIMISTIC_WRITE` al buscar por hash. El comportamiento esperado con dos requests simultáneos es una rotación exitosa seguida de detección de reutilización. La familia y sesión terminan revocadas.

### Última autoridad

Antes de contar autoridades y revocar un rol o eliminar una persona, se bloquea la fila de `Institution`. Todas las operaciones que preservan este invariante deben adquirir el mismo lock antes del conteo.

### Seed de permisos y roles

El seed usa `pg_advisory_xact_lock` fuera del perfil `test`. El lock cubre toda la transacción de sincronización y evita carreras entre réplicas durante el arranque.

### Restricciones únicas

Las comprobaciones `exists...` producen mensajes tempranos, pero no reemplazan una restricción de base. Los casos de creación y actualización fuerzan `flush()` dentro del use case para capturar una carrera y traducirla a una excepción de dominio HTTP 409.

## Cachés

| Caché | Se invalida cuando |
|---|---|
| `activeSessions` | logout, reutilización de refresh, eliminación de persona o desactivación institucional |
| `activePlatformSessions` | logout, reutilización de refresh, desactivación o cambio de credenciales de cuenta plataforma |
| `personPermissions` | asignación/revocación de rol institucional y seed |
| `platformAccountPermissions` | asignación/revocación de rol plataforma y seed |
| `platformAccountRoles` | asignación/revocación de rol plataforma y seed |

Redis aplica un TTL de 5 minutos a las entradas de Spring Cache. En staging y producción usa AOF sobre un volumen persistente para conservar la blacklist de access tokens después de un reinicio. Al agregar una mutación nueva de sesiones, roles o permisos, debe agregarse su invalidación en la misma entrega.

## Persistencia e índices relevantes

- Sesiones: índice por propietario y estado activo.
- Refresh tokens: índices por familia y sesión.
- Personas: índice por institución y eliminación lógica.
- Asignaciones de roles: índices por persona/institución y por institución/rol.
- Sesiones y refresh tokens tienen asociaciones de solo lectura para materializar claves foráneas sin reemplazar los IDs escalares usados por los casos de uso.

### Evolución del esquema

Flyway es la única herramienta que crea o modifica el esquema. Hibernate usa `ddl-auto=validate` y detiene el arranque si las entidades no coinciden con la base.

- Cada cambio persistente incluye la modificación del `@Entity` y una migración SQL nueva.
- Los nombres usan timestamps UTC con formato `yyyyMMddHHmmss__description.sql`.
- Una migración aplicada no se modifica ni se renombra; cualquier corrección avanza con una migración posterior.
- Las migraciones comunes viven en `db/migration`; `db/dev` contiene únicamente datos de desarrollo.
- La migración inicial se verifica contra PostgreSQL mediante Testcontainers antes de publicar la imagen.

## Cobertura automatizada

La suite incluye:

- Casos positivos y negativos de login, refresh y logout.
- Autorización MVC para principals institucionales y de plataforma.
- Acceso cruzado entre instituciones.
- Protección de la última autoridad.
- Desactivación institucional y revocación de sesiones.
- Asignación inicial de roles privilegiados.
- Integridad JPA y restricciones de persistencia.
- Conversión de carreras de unicidad a conflictos.
- PostgreSQL real con Testcontainers para reutilización y rotación concurrente de refresh tokens.

Los tests H2 siguen siendo útiles para slices rápidos, pero las garantías que dependen de locks o semántica transaccional de PostgreSQL deben cubrirse con Testcontainers.

## Checklist para nuevas funcionalidades

Antes de incorporar una operación institucional nueva:

1. ¿El endpoint exige permiso o rol explícito?
2. ¿Se valida que el `institutionId` pertenezca al principal?
3. ¿La consulta de repositorio filtra también por institución?
4. ¿La entidad resultante mantiene coherencia de tenant?
5. ¿La mutación afecta sesiones, roles o permisos cacheados?
6. ¿Existe una carrera entre el chequeo y la escritura?
7. ¿La restricción de base y la excepción HTTP representan el mismo conflicto?
8. ¿Hay pruebas positiva, negativa, de otro tenant y, si corresponde, concurrente?

## Referencias de implementación

- `auth/services/RefreshTokenUseCase`
- `auth/services/PlatformRefreshTokenUseCase`
- `auth/services/IsSessionActiveUseCase`
- `auth/filters/JwtAuthenticationFilter`
- `authorization/services/SessionRevocationService`
- `authorization/services/InitialRoleAssignmentGuard`
- `authorization/services/RevokePersonRoleUseCase`
- `authorization/services/PermissionRoleSeed`
- `authorization/security/InstitutionAccessAspect`
- `institutional/services/UpdateInstitutionStatusUseCase`
- `institutional/services/DeletePersonUseCase`
