# Autorización y Permisos

## ¿Por qué `getAuthorities()` devuelve vacío?

`User` (`src/main/java/.../auth/entities/User.java`) y `PlatformAccount` (`src/main/java/.../auth/entities/PlatformAccount.java`) devuelven `List.of()` de `getAuthorities()`.

Esto es intencional. No cargamos permisos ni en el JWT ni en las authorities de Spring Security. En su lugar, se resuelven desde la base de datos y se cachea el resultado por principal.

## ¿Por qué no usamos `@PreAuthorize`?

En lugar de `@PreAuthorize("hasAuthority('...')")`, el proyecto usa anotaciones propias (`@RequiresPermission`, `@RequiresAnyPermission`, `@RequiresPlatformRole`) manejadas por aspectos. Las razones:

- Centralizar la lógica de autorización en `AuthorizationService`
- Usar enums del proyecto (`PermissionCode`, `PlatformRoleCode`) como valores de anotación, sin necesidad de strings SpEL
- Un solo mecanismo para permisos institucionales y roles plataforma

## Resolución dinámica de permisos

`AuthorizationService` (`src/main/java/.../authorization/services/AuthorizationService.java`) es el punto de entrada:

1. Recibe el `Authentication` del `SecurityContext`
2. Determina el tipo de principal (`JwtAuthenticatedUser` vs `JwtAuthenticatedPlatformAccount`)
3. Delega en `AuthorityResolver` con los IDs correspondientes

### Para usuarios institucionales

`AuthorityResolver.resolveForPerson(personId, institutionId)`:

1. Busca los role IDs asignados a esa persona en esa institución (`PersonRoleAssignmentRepository`)
2. Mapea esos roles a permission codes (`RolePermissionRepository`)
3. Devuelve un `Set<PermissionCode>`

### Para cuentas plataforma

`AuthorityResolver.resolveForPlatformAccount(platformAccountId)`:

1. Busca los role IDs asignados a esa cuenta (`PlatformAccountRoleRepository`)
2. Mapea a permission codes (`RolePermissionRepository`)
3. También puede resolver `PlatformRoleCode` específicos para `@RequiresPlatformRole`

## Anotaciones de autorización

| Anotación | Parámetro | ¿Qué verifica? |
|---|---|---|
| `@RequiresPermission` | `PermissionCode` | Que el principal tenga ese permiso específico |
| `@RequiresAnyPermission` | `PermissionCode[]` | Que el principal tenga al menos uno de los permisos |
| `@RequiresPlatformRole` | `PlatformRoleCode` | Que el principal sea cuenta plataforma con ese rol |

### Aspectos que las interceptan

- `PermissionAuthorizationAspect` — intercepta `@RequiresPermission` y `@RequiresAnyPermission`
- `RoleAuthorizationAspect` — intercepta `@RequiresPlatformRole`

Ambos leen el `Authentication` del `SecurityContext`, llaman a `AuthorizationService` y lanzan `AccessDeniedException` si la verificación falla (`AuthorizationAspectSupport.denyUnless()`).

## Guarda de llamada institucional

`@RequiresInstitutionAccess`, interceptada por `InstitutionAccessAspect`, protege controllers cuyo recurso está identificado por `institutionId`. Exige un principal institucional y que el ID del JWT coincida con el ID de la ruta. Las cuentas plataforma no atraviesan esta guarda.

`InstitutionalCallerGuard` aplica la misma separación en operaciones de servicio o autorización que necesitan una comprobación explícita. Estas dos capas evitan que un usuario de la institución A lea o modifique recursos de la institución B cambiando un path variable.

## Frontera administrativa

Todas las operaciones de administración global viven bajo `/api/v1/admin/**`. Salvo `POST /admin/auth/login` y `POST /admin/auth/refresh`, Spring Security exige dinámicamente el rol `PLATFORM_ADMIN` para todo el namespace.

Los controllers administrativos conservan además `@RequiresPlatformRole(PLATFORM_ADMIN)` como defensa en profundidad. La gestión administrativa de personas y roles usa rutas propias bajo `/admin/institutions/{institutionId}/**`; no reutiliza las rutas institucionales ni sus permisos.

## Caché e invalidación

`AuthorityResolver` usa tres cachés:

| Caché | Clave conceptual | Contenido |
|---|---|---|
| `personPermissions` | persona + institución | permisos institucionales |
| `platformAccountPermissions` | cuenta plataforma | permisos de plataforma |
| `platformAccountRoles` | cuenta plataforma | roles de plataforma |

Las entradas Redis tienen TTL de 5 minutos. La asignación o revocación de roles desaloja las claves afectadas, por lo que el cambio tiene efecto en el siguiente request sin esperar al vencimiento del JWT ni del TTL. El seed limpia los tres cachés después de sincronizar el catálogo.

## Trade-off

- **Ventaja**: cambiar un rol tiene efecto inmediato mediante invalidación, sin esperar a que el token expire.
- **Ventaja**: requests repetidos no consultan la base en cada autorización.
- **Costo**: la corrección depende de invalidar la caché en cada operación que cambie roles o permisos; el TTL limita el impacto de una omisión.

## Flujo completo

```
Request → JwtAuthenticationFilter (autentica) → Controller
  → @RequiresPermission (anotación) → PermissionAuthorizationAspect
  → AuthorizationService.hasPermission()
  → AuthorityResolver.resolveForPerson() → caché Redis o BD
  → Si autorizado → ejecuta el método
  → Si no → AccessDeniedException → 403
```
