# Casos Particulares

## ¿Por qué `UserSession` existe si Spring es stateless?

Spring Security está configurado con `SessionCreationPolicy.STATELESS`. Sin embargo, el proyecto mantiene sesiones lógicas en la base de datos (`UserSession`, `PlatformSession`) porque:

- Cada access token está ligado a un `sessionId`
- El filtro JWT verifica que la sesión siga activa en cada request
- Permite revocar todos los tokens de una sesión (logout, incidentes de seguridad)
- Permite listar sesiones activas (`GET /api/v1/auth/sessions`)

## ¿Por qué los permisos no van en el JWT?

Incluir permisos en el JWT haría que:
- Cambiar un rol no tenga efecto hasta que el token expire
- El token sea más grande (especialmente con muchos permisos)
- Revocar permisos requiera blacklistear tokens activos

La decisión fue token pequeño + resolución dinámica. El costo: una query extra por request autorizado.

## Bootstrap del admin de plataforma

`PlatformAdminBootstrap` (`src/main/java/.../authorization/services/PlatformAdminBootstrap.java`) se ejecuta al iniciar la aplicación:

1. Lee `app.platform-admin.email` y `app.platform-admin.password` de la configuración
2. Si no están configurados, salta el bootstrap (útil para desarrollo)
3. Crea un `PlatformAccount` si no existe
4. Le asigna el rol `PLATFORM_ADMIN`

## Seeding de permisos y roles

`PermissionRoleSeed` (`src/main/java/.../authorization/services/PermissionRoleSeed.java`) se ejecuta con prioridad máxima al iniciar:

1. Sincroniza todos los `PermissionCode` del enum con la tabla `permissions`
2. Sincroniza `SystemRoleCode` con la tabla de roles institucionales
3. Sincroniza `PlatformRoleCode` con la tabla de roles de plataforma
4. Asigna permisos a roles según un mapeo fijo
5. En dev/test, asigna el rol `APPLICANT` a personas sin rol

## Bootstrap de authority institucional

`BootstrapInstitutionalAuthorityUseCase` (endpoint `POST /api/v1/platform/institutions/{id}/authority/{personId}`) es la única forma de crear el primer `INSTITUTIONAL_AUTHORITY` de una institución. Solo `PLATFORM_ADMIN` puede ejecutarlo.

La primera authority no se puede crear con el endpoint regular de asignación de roles porque ese endpoint requiere permisos institucionales que todavía no existen.

## Protección del último authority

`RevokePersonRoleUseCase` (`src/main/java/.../authorization/services/RevokePersonRoleUseCase.java`) verifica que no se pueda revocar el último `INSTITUTIONAL_AUTHORITY` de una institución. Si solo queda uno, lanza `LastInstitutionalAuthorityRevocationException` (HTTP 409).

## Cambio de roles sin revocar sesiones

Cuando se asigna o revoca un rol, la sesión del usuario **no se revoca**. Esto es una decisión deliberada de UX: como los permisos se resuelven dinámicamente desde la base de datos en cada request, el cambio de rol tiene efecto inmediato sin obligar al usuario a volver a loguearse.

`SessionRevocationService` (`src/main/java/.../authorization/services/SessionRevocationService.java`) sigue existiendo como utilidad disponible para uso futuro (por ejemplo, un admin que quiera cerrar todas las sesiones de un usuario manualmente), pero los flujos automáticos de asignación/revocación de roles no lo invocan.

## Usuario de desarrollo

`InstitutionalDevUserBootstrap` (`src/main/java/.../auth/services/InstitutionalDevUserBootstrap.java`) solo se activa en perfiles `dev` y `test`. Crea una persona y usuario de prueba con rol `APPLICANT`. Configurable mediante `app.institutional-dev-user.*`.

## Sesiones activas

El endpoint `GET /api/v1/auth/sessions` lista las sesiones activas del usuario autenticado, indicando cuál es la sesión actual. Útil para que el usuario vea desde qué dispositivos tiene sesión iniciada.
