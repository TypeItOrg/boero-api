# Autorización y Permisos

## ¿Por qué `getAuthorities()` devuelve vacío?

`User` (`src/main/java/.../auth/entities/User.java:86-88`) y `PlatformAccount` (`src/main/java/.../authorization/entities/PlatformAccount.java:80-82`) devuelven `List.of()` de `getAuthorities()`.

Esto es intencional. No cargamos permisos ni en el JWT ni en las authorities de Spring Security. En su lugar, los resolvemos dinámicamente desde la base de datos en cada request.

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

`InstitutionalCallerGuard` verifica que el caller sea un `JwtAuthenticatedUser` y que pertenezca a la institución indicada. Se usa en operaciones de gestión de roles para evitar que un usuario de institución A intente modificar roles de institución B.

## Trade-off

- **Ventaja**: permisos siempre frescos — cambiar un rol tiene efecto inmediato, sin esperar a que el token expire
- **Desventaja**: query extra a la base de datos por cada request autorizado

## Flujo completo

```
Request → JwtAuthenticationFilter (autentica) → Controller
  → @RequiresPermission (anotación) → PermissionAuthorizationAspect
  → AuthorizationService.hasPermission()
  → AuthorityResolver.resolveForPerson() → BD
  → Si autorizado → ejecuta el método
  → Si no → AccessDeniedException → 403
```
