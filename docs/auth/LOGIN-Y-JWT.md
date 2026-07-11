# Login y JWT

## Login institucional

Endpoint: `POST /api/v1/auth/login`

El flujo (`LoginUseCase`, `src/main/java/.../auth/services/LoginUseCase.java`):

1. Construye el principal compuesto: `institutionId:documentNumber`
2. `AuthenticationManager` autentica con ese principal + password
3. Si las credenciales son inválidas, lanza `InvalidCredentialsException`
4. Si ok, extrae el `User` autenticado
5. Crea una `UserSession` con IP, user-agent y flag `rememberMe`
6. Genera un `familyId` UUID y un raw refresh token UUID
7. Guarda el refresh token como hash SHA-256
8. Genera el access token JWT
9. Devuelve `AuthResponse` con access token + raw refresh token

## Login plataforma

Endpoint: `POST /api/v1/auth/platform/login`

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
