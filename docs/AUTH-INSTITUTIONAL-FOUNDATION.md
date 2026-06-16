# Fundación de Autenticación e Institucional

Este documento resume el estado de la rama `feat/auth-institutional-foundation`. Sirve como referencia para entender qué piezas se incorporaron, cómo se relacionan y qué decisiones técnicas conviene mantener presentes al continuar el desarrollo.

## Alcance

La rama introduce una base funcional para autenticación, sesiones de usuario, refresh tokens, modelo institucional y persistencia con auditoría. También deja una estrategia de testing por capas, evitando depender únicamente de tests de contexto.

El objetivo fue dejar una fundación consistente para registrar usuarios asociados a una institución, autenticarlos con JWT, administrar sesiones activas y validar que los mapeos JPA principales funcionen correctamente.

## Modelo institucional

El modelo institucional contiene entidades para representar ubicación y pertenencia: `Country`, `Province`, `City`, `Address`, `Institution` y `Person`.

Una `Institution` pertenece a una dirección y una `Person` pertenece a una institución. Sobre esa persona se monta el `User`, que es la entidad autenticable. Esta separación permite distinguir datos institucionales y de identidad de los datos propios de acceso.

El campo `postalCode` fue eliminado de `City`, dejando la ciudad enfocada en su identidad territorial y relación con provincia.

## Modelo de autenticación

La autenticación se apoya en `User`, `UserSession` y `RefreshToken`.

`User` representa la cuenta autenticable. Está asociada a una `Person` y a una `Institution`, implementa `UserDetails` y conserva la contraseña codificada con BCrypt.

`UserSession` representa una sesión lógica de aplicación. La aplicación sigue siendo stateless a nivel HTTP, pero guarda sesiones para validar access tokens, listar sesiones activas y cerrar sesiones explícitamente.

`RefreshToken` representa tokens persistidos y hasheados. El token crudo solo se entrega al cliente; en base de datos se guarda el hash SHA-256 en Base64. Cada refresh token pertenece a una sesión y a una familia (`familyId`), lo que permite detectar reutilización y revocar una familia completa ante un posible replay.

## Flujos de autenticación

El registro (`POST /auth/register`) busca la institución, valida que no exista una persona con el mismo documento dentro de esa institución, crea la `Person`, crea el `User` con contraseña codificada y responde `userId`, `documentNumber` e `institutionId`.

El login (`POST /auth/login`) busca el usuario por documento e institución, valida la contraseña y el estado del usuario, crea una `UserSession`, genera un refresh token inicial y emite un access token JWT. Si `rememberMe` está activo, la expiración del refresh token usa la duración extendida configurada.

El refresh (`POST /auth/refresh`) implementa rotación. El servidor busca el hash del refresh token recibido, valida expiración y sesión activa, revoca el token actual, genera uno nuevo dentro de la misma familia y emite un nuevo access token. Si se recibe un refresh token ya revocado, se interpreta como reutilización y se revoca toda la familia junto con sus sesiones asociadas.

El logout (`POST /auth/logout`) agrega el identificador del access token (claim `jti`) a blacklist hasta su expiración, revoca los refresh tokens de la sesión actual y marca la sesión como inactiva con `endedAt`.

Las sesiones activas se consultan con `GET /auth/sessions`, que devuelve una respuesta paginada e indica cuál es la sesión actual. El usuario autenticado se consulta con `GET /auth/me`.

## JWT y seguridad

Los access tokens se generan en `JwtService`. Incluyen `sub` como ID del usuario, `jti` como identificador único del token, `documentNumber`, `institutionId`, `sessionId`, fecha de emisión y expiración.

`JwtAuthenticationFilter` valida cada request protegido. El token debe ser válido, no estar blacklisteado y pertenecer a una sesión activa. Cuando pasa esas validaciones, el filtro construye un `JwtAuthenticatedUser` y lo guarda en el `SecurityContext`.

`SecurityConfig` deshabilita `formLogin`, `httpBasic`, logout de Spring Security, remember-me y sesiones HTTP. La política de sesión es `STATELESS`, y el filtro JWT se registra antes de `UsernamePasswordAuthenticationFilter`.

## Auditoría JPA

La auditoría común está centralizada en `Auditable`, un `@MappedSuperclass` con `createdAt` y `updatedAt`. `JpaAuditingConfig` habilita `@EnableJpaAuditing`, por lo que Spring Data completa esas fechas automáticamente al persistir y actualizar entidades.

Esta decisión reemplaza callbacks repetidos con `@PrePersist` y `@PreUpdate`, y evita depender de triggers o defaults específicos de base de datos.

`RefreshToken` conserva solamente `createdAt`, porque no necesita `updatedAt` genérico. `UserSession` mantiene `startedAt` como dato de dominio, ya que representa cuándo comenzó la sesión y no una fecha de auditoría.

## Convención de IDs

Las entidades mantienen el atributo Java `id`, pero las columnas de base usan nombres prefijados. Por ejemplo, `User` mapea su ID a `user_id`, `Institution` a `institution_id` y `Person` a `person_id`.

Esta convención mejora la claridad del esquema sin forzar cambios innecesarios en el código Java. Los getters, builders y relaciones siguen trabajando con `id`, mientras que el modelo relacional evita columnas genéricas llamadas solamente `id`.

Cualquier SQL manual o migración futura debe usar los nombres prefijados, no `id`.

## Builders en entidades

Las entidades JPA relevantes usan builders de Lombok para simplificar la creación de objetos en servicios y tests. Se mantiene `@NoArgsConstructor` porque Hibernate lo necesita para instanciar entidades.

La regla práctica es usar builders para construir entidades nuevas con sus datos obligatorios y dejar setters para cambios de estado concretos, como revocar un token o cerrar una sesión.

## Testing

La rama deja una estrategia de tests robusta y por capas, combinando tests unitarios puros para la lógica de negocio y tests de slice para persistencia e infraestructura.

Los tests se organizan de la siguiente manera:

- **Tests unitarios de Lógica de Negocio (Use Cases y Servicios)**: Se testean los casos de uso (`LoginUseCase`, `RefreshTokenUseCase`, `LogoutUseCase`, `RegisterUserUseCase`, `GetCurrentUserUseCase`) y servicios de soporte (`JwtService`) de forma aislada utilizando Mockito (con `@ExtendWith(MockitoExtension.class)`). Esto asegura que la lógica crítica (como rotación de tokens, detección de reutilización, revocación en cascada de sesiones y encriptación de claves) esté completamente validada de manera rápida y sin acoplamiento a la base de datos o al contexto de Spring.
- **Tests JPA**: Usan `@DataJpaTest` con H2 en modo PostgreSQL. Cubren auditoría, mapeos y queries principales de repositorios.
- **Tests JSON**: Usan `@JsonTest` para fijar contratos de serialización de payloads de autenticación.
- **Tests MVC / Web**: Usan `@WebMvcTest`. Hay una clase para contratos HTTP exitosos de `AuthController` y otra dedicada a validaciones de payloads.
- **Tests de Integración**: `@SpringBootTest` queda como smoke test mínimo para comprobar que el contexto completo (incluyendo dependencias como Redis) levanta correctamente mediante Testcontainers.

## Comandos útiles

Para correr la suite de tests con el flujo habitual del proyecto:

```bash
make test
```

Para forzar la ejecución aunque Gradle considere tareas actualizadas:

```bash
./gradlew --no-daemon test --rerun-tasks
```

El `build.gradle` tiene configurado `testLogging` para mostrar los tests ejecutados.

## Puntos a cuidar al continuar

Los refresh tokens nunca deberían persistirse crudos. La base solo debe guardar su hash.

Un access token criptográficamente válido no alcanza si su sesión está inactiva o si el identificador del token (`jti`) fue blacklisteado.

Si se agregan nuevas entidades con `createdAt` y `updatedAt`, conviene extender `Auditable` en lugar de repetir callbacks.

Los tests nuevos deberían usar el slice más chico que pruebe el comportamiento necesario, reservando `@SpringBootTest` para integración completa o smoke tests.
