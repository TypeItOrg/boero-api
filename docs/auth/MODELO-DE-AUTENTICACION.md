# Modelo de Autenticación

## Resumen

La API tiene dos modelos de autenticación independientes:

- **Institucional**: para personas asociadas a una institución. Credenciales: documento + institución + password.
- **Plataforma**: para administradores globales de la plataforma. Credenciales: email + password.

## Usuario institucional

Cada usuario institucional (`User`, `src/main/java/.../auth/entities/User.java`) está asociado a una `Person` y a una `Institution`. La combinación `(institution_id, person_id)` es única.

El `User` implementa `UserDetails`. Su `getUsername()` devuelve el número de documento (`src/main/java/.../auth/entities/User.java:65-67`). Los `UserDetailsService` convierten el par `institutionId:documentNumber` en un `User`.

### Entidades involucradas

- `User` — cuenta autenticable con contraseña hasheada (BCrypt)
- `Person` — datos de la persona (nombre, apellido, documento)
- `Institution` — institución a la que pertenece

Una cuenta institucional solo se considera habilitada cuando se cumplen simultáneamente estas condiciones:

- `User.enabled` es `true`.
- La `Institution` está activa.
- La `Person` no está eliminada lógicamente.

Además, antes de persistir o actualizar un `User`, la entidad verifica que `User.institution` y `Person.institution` sean la misma. Esta validación evita construir una cuenta que autentique a una persona bajo otra institución.

## Usuario plataforma

Las cuentas de plataforma (`PlatformAccount`, `src/main/java/.../auth/entities/PlatformAccount.java`) son independientes de cualquier institución. Se autentican con email + password.

- `PlatformAccount` — cuenta global con email y contraseña

## ¿Por qué dos modelos?

Separar cuentas de gestión global (`PlatformAccount`) de cuentas ligadas a una institución (`User`). Un `PlatformAccount` puede administrar varias instituciones; un `User` pertenece a una única institución y su existencia depende de ella.

## ¿Cómo distingue Spring cuál es cuál?

`CompositeUserDetailsService` (`src/main/java/.../auth/security/CompositeUserDetailsService.java`) actúa como router:

- Si el username empieza con `platform:`, carga un `PlatformAccount` mediante `PlatformUserDetailsService`
- Si no, interpreta el username como `institutionId:documentNumber` y carga un `User` mediante `InstitutionalUserDetailsService`

Los formateadores `InstitutionalUsername` y `PlatformUsername` definen este formato.

## Tabla comparativa

| Aspecto | Institucional | Plataforma |
|---|---|---|
| Credenciales | documento + institución + password | email + password |
| Entidad | `User` | `PlatformAccount` |
| Sesión | `UserSession` | `PlatformSession` |
| Refresh token | `RefreshToken` | `PlatformRefreshToken` |
| Login | `POST /api/v1/auth/login` | `POST /api/v1/auth/platform/login` |
| Rol | `SystemRoleCode` (institucional) | `PlatformRoleCode` (global) |
