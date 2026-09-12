# Login y JWT

## Login institucional

Flujo identifier-first en dos pasos:

1. `POST /api/v1/auth/login/identify` (`IdentifyLoginUseCase`): resuelve institución + DNI y emite un `loginAttemptId` de un solo uso con TTL corto.
2. `POST /api/v1/auth/login/password` (`PasswordLoginWithAttemptUseCase`) o passkey (`POST /api/v1/auth/passkeys/authentication/verify`): verifica la credencial, reclama el attempt atómicamente y recién entonces emite la sesión.

El flujo (`PasswordLoginWithAttemptUseCase`, `src/main/java/.../auth/services/PasswordLoginWithAttemptUseCase.java`):

1. Resuelve el `loginAttemptId` (inexistente/expirado/consumido → error)
2. Construye el principal compuesto: `institutionId:documentNumber`
3. `AuthenticationManager` autentica con ese principal + password
4. Si las credenciales son inválidas, lanza `InvalidCredentialsException` (el attempt NO se consume: reintentos permitidos)
5. Si ok, reclama el attempt y extrae el `User` autenticado
6. Crea una `UserSession` con IP, user-agent y flag `rememberMe`
7. Genera un `familyId` UUID y un raw refresh token UUID
8. Guarda el refresh token como hash SHA-256
9. Genera el access token JWT
10. Devuelve `AuthResponse` con access token + raw refresh token

El endpoint legacy de login directo (`POST /api/v1/auth/login`, `LoginUseCase`) fue eliminado: todo password login pasa por identifier-first. El rate limiting corresponde a Nginx.

## Login plataforma

Endpoint: `POST /api/v1/admin/auth/login`

Mismo flujo (`PlatformLoginUseCase`, `src/main/java/.../auth/services/PlatformLoginUseCase.java`) pero:

- El principal se construye como `platform:email`
- Crea `PlatformSession` y `PlatformRefreshToken`
- Genera access token con `JwtService.generatePlatformAccessToken()`

## Contenido del JWT

El JWT se genera con HMAC-SHA256 en `JwtService` (`src/main/java/.../auth/services/JwtService.java`).

### Claims para usuario institucional

| Claim | Descripción |
|---|---|
| `sub` | UUID del `User` |
| `jti` | UUID único del token |
| `accountType` | `INSTITUTION` |
| `documentNumber` | Número de documento |
| `institutionId` | ID de la institución |
| `personId` | ID de la persona |
| `sessionId` | ID de la sesión |
| `iat` | Fecha de emisión |
| `exp` | Fecha de expiración |

### Claims para cuenta plataforma

| Claim | Descripción |
|---|---|
| `sub` | UUID del `PlatformAccount` |
| `jti` | UUID único del token |
| `accountType` | `PLATFORM` |
| `email` | Email de la cuenta |
| `sessionId` | ID de la sesión |
| `iat` | Fecha de emisión |
| `exp` | Fecha de expiración |

## Validación del token en cada request

`JwtAuthenticationFilter` (`src/main/java/.../auth/filters/JwtAuthenticationFilter.java`) se ejecuta antes que cualquier controller:

1. Lee `Authorization: Bearer <token>`
2. Omite rutas públicas (`shouldNotFilter`)
3. Parsea con `JwtService.parseAccessToken()`
4. Si expirado → 401 `TOKEN_EXPIRED`
5. Si inválido → 401 `TOKEN_INVALID`
6. Si ok → extrae `tokenId`, `sessionId`, `accountType`
7. Verifica que el token no esté en blacklist (Redis)
8. Verifica que la sesión esté activa
9. Construye el principal (`JwtAuthenticatedUser` o `JwtAuthenticatedPlatformAccount`)
10. Lo guarda en el `SecurityContext`

Para una sesión institucional, “activa” no significa solamente `UserSession.active = true`. `IsSessionActiveUseCase` consulta que también estén habilitados el usuario y la institución, y que la persona no esté eliminada. Por eso desactivar una institución invalida los access tokens institucionales aunque el JWT todavía no haya expirado.

Los resultados de actividad de sesión se cachean en Redis para evitar una consulta por request. Las entradas tienen TTL global de 5 minutos y se eliminan explícitamente en logout, detección de reutilización y revocaciones administrativas. La invalidación explícita es la garantía principal; el TTL funciona como límite defensivo ante una entrada que no hubiera sido desalojada.

## Principales autenticados

`JwtPrincipal` (`src/main/java/.../auth/filters/JwtPrincipal.java`) es un sealed interface con dos implementaciones:

- `JwtAuthenticatedUser` — para requests institucionales. Incluye `userId`, `personId`, `documentNumber`, `institutionId`, `sessionId`, `tokenId`
- `JwtAuthenticatedPlatformAccount` — para requests plataforma. Incluye `platformAccountId`, `email`, `sessionId`, `tokenId`

## Configuración

En `JwtProperties` (`src/main/java/.../auth/config/JwtProperties.java`):

| Propiedad | Default |
|---|---|
| `access-token-expiration` | 15 minutos |
| `refresh-token-expiration` | 7 días |
| `remember-me-token-expiration` | 30 días |


## Verificación de email al registrarse

El autorregistro institucional crea la persona, el usuario y el rol aspirante con verificación pendiente. No se emiten sesiones hasta confirmar el correo. Las cuentas existentes y las altas administrativas quedan exentas; la verificación no modifica la habilitación administrativa.

El enlace se confirma desde la página pública del frontend mediante una acción explícita. Vence a las 24 horas y es de un solo uso. El reenvío tiene un intervalo mínimo de 60 segundos e invalida el enlace anterior. Una respuesta de reenvío no revela la existencia de la cuenta ni garantiza entrega SMTP.

Antes de verificar, se puede corregir el email indicando institución, documento y contraseña. La corrección invalida los enlaces anteriores de verificación y recuperación de contraseña. Recuperar la contraseña no verifica el email. Los tokens se guardan como hashes SHA-256 y los cambios se serializan bloqueando el usuario antes de consultar o consumir el token.

La URL pública usa `EMAIL_VERIFICATION_FRONTEND_URL`, con fallback a `PASSWORD_RECOVERY_FRONTEND_URL`. `EMAIL_VERIFICATION_TOKEN_EXPIRATION` y `EMAIL_VERIFICATION_RESEND_INTERVAL` permiten configurar los tiempos. Se conserva el envío SMTP asíncrono posterior al commit; ante fallos, la cuenta continúa pendiente y puede solicitar otro envío.
