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
6. Verifica que el usuario o cuenta asociada siga habilitada; para usuarios institucionales esto incluye institución activa y persona no eliminada
7. Revoca el token actual
8. Genera un NUEVO raw refresh token en la **misma familia**
9. Genera un nuevo access token
10. Devuelve ambos

La consulta del token usa un lock pesimista de escritura. Dos requests no pueden rotar simultáneamente la misma fila: el segundo espera a que finalice el primero y luego observa el token revocado.

### Detección de reutilización

Si alguien reenvía un refresh token ya revocado:

1. Se cargan todos los tokens de la misma familia
2. Se revocan TODOS
3. Se desactivan TODAS las sesiones asociadas a esos tokens
4. Se lanza `TokenRefreshException.reuse()`

La transacción de refresh está configurada para confirmar estas revocaciones aunque termine lanzando `TokenRefreshException`. Sin esa excepción transaccional, responder `401` podría revertir exactamente las revocaciones realizadas para contener el incidente.

Cuando se desactivan las sesiones, también se desalojan sus entradas de `activeSessions` o `activePlatformSessions`.

Esto protege contra ataques de robo de tokens: si un atacante usa un token que la víctima ya consumió, el sistema invalida todo.

### Resultado ante dos rotaciones concurrentes

Si dos requests presentan el mismo refresh token al mismo tiempo:

1. Uno obtiene el lock, rota el token y confirma.
2. El segundo continúa después del commit y detecta que el token ya está revocado.
3. Se trata como reutilización: revoca la familia completa y desactiva las sesiones relacionadas.

El primer request puede haber recibido tokens, pero su access token deja de ser utilizable porque el filtro comprueba el estado de la sesión en cada request.

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
| Endpoint refresh | `POST /api/v1/auth/refresh` | `POST /api/v1/admin/auth/refresh` |
| Endpoint logout | `POST /api/v1/auth/logout` | `POST /api/v1/admin/auth/logout` |
