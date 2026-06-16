# Refresh Token y Logout

## Refresh token

Los refresh tokens permiten renovar un access token sin pedir credenciales nuevamente.

### Almacenamiento

El token crudo (UUID aleatorio) se entrega una sola vez al cliente. En la base de datos se guarda su hash SHA-256 en Base64. Nunca se persiste el valor plano.

Cada refresh token pertenece a:

- **sessionId** — la sesión que lo creó
- **familyId** — identificador de familia para detectar reutilización

### Rotación

Endpoint: `POST /api/v1/auth/refresh`

El flujo (`RefreshTokenUseCase`, `src/main/java/.../auth/services/RefreshTokenUseCase.java`):

1. Recibe el raw refresh token
2. Calcula su hash SHA-256 y lo busca en BD
3. Si el token ya está revocado → detecta reutilización y revoca toda la familia
4. Si expiró → error `TOKEN_REFRESH_INVALID`
5. Verifica que la sesión siga activa
6. Revoca el token actual
7. Genera un NUEVO raw refresh token en la **misma familia**
8. Genera un nuevo access token
9. Devuelve ambos

### Detección de reutilización

Si alguien reenvía un refresh token ya revocado:

1. Se cargan todos los tokens de la misma familia
2. Se revocan TODOS
3. Se desactivan TODAS las sesiones asociadas a esos tokens
4. Se lanza `TokenRefreshException.reuse()`

Esto protege contra ataques de robo de tokens: si un atacante usa un token que la víctima ya consumió, el sistema invalida todo.

## Logout

Endpoint: `POST /api/v1/auth/logout`

El flujo (`LogoutUseCase`, `src/main/java/.../auth/services/LogoutUseCase.java`):

1. Parsea el access token actual
2. Extrae su `tokenId` y lo agrega a la blacklist de Redis con TTL igual al tiempo restante del token (mínimo 1 minuto)
3. Revoca todos los refresh tokens de la sesión
4. Marca la sesión como inactiva (`endedAt`)

### Blacklist en Redis

`TokenBlacklistService` (`src/main/java/.../auth/services/TokenBlacklistService.java`):

- Clave: `jwt:blacklist:<tokenId>`
- El filtro JWT verifica esta blacklist antes de aceptar cualquier token
- La entrada expira automáticamente cuando el token original expiraría

## Diferencias institucional vs plataforma

Ambos flujos son idénticos en lógica, pero usan entidades distintas:

| Aspecto | Institucional | Plataforma |
|---|---|---|
| Sesión | `UserSession` | `PlatformSession` |
| Refresh token | `RefreshToken` | `PlatformRefreshToken` |
| Use case | `RefreshTokenUseCase` | `PlatformRefreshTokenUseCase` |
| Logout | `LogoutUseCase` | `PlatformLogoutUseCase` |
| Endpoint refresh | `POST /api/v1/auth/refresh` | `POST /api/v1/auth/platform/refresh` |
| Endpoint logout | `POST /api/v1/auth/logout` | `POST /api/v1/auth/platform/logout` |
