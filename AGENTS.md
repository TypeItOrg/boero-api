## Project Overview

Spring Boot 4.0.6 API built with Gradle 9.4.1, Java 21.
Uses Lombok, JPA, PostgreSQL, Redis, Spring Security, JWT access/refresh tokens, Docker and Testcontainers.

## Build & Tooling

- **Build tool:** Gradle 9.4.1 (`./gradlew`).
- **Java version:** 21.
- **Spring Boot:** 4.0.6.
- **Formatter:** Spotless with `googleJavaFormat()`. Source of truth for Java formatting.
- **Docker:** multi-stage `Dockerfile` with `prod` and `dev` targets.
- **Compose:** `compose.yaml` is only for local development. Shared staging and production infrastructure lives in `boero-infra`.
- **Makefile:** shortcuts for common tasks.
- **Environment:** copy `.env.dev.example` to `.env.dev`. Never commit real env files.

### Common commands

| Command | Description |
|---|---|
| `./gradlew bootRun` | Run the application locally |
| `./gradlew test` | Run all tests |
| `./gradlew spotlessApply` | Format all Java source files |
| `./gradlew spotlessCheck` | Verify formatting without changing files |
| `make dev` | Start local development environment with Docker Compose |
| `make test` | Run `./gradlew --no-daemon test` |
| `make format` | Run `./gradlew spotlessApply` |
| `make format-check` | Run `./gradlew spotlessCheck` |

## Code Formatting

- The project uses **Spotless with google-java-format**.
- Do not rely on manual indentation rules. Run `./gradlew spotlessApply` before committing.
- CI should run `./gradlew spotlessCheck`.
- Configure IntelliJ to use the google-java-format plugin (`.idea/google-java-format.xml` is already committed).

## Java Style

- Use UTF-8 encoding.
- Use descriptive names for classes, methods, and variables.
- Prefer explicit types, but `var` is allowed for local variables when the type is obvious from the right-hand side.
- Declare method parameters and local variables as `final` where possible. Apply this rule to new code; do not refactor the entire codebase just to add `final`.
- All method parameters should be `final` in new code.
- Avoid mutations of objects, especially when using for-each loops or Stream API `forEach()`.
- Avoid magic numbers and strings; use constants instead.
- Check emptiness and nullness before operations on collections and strings.
- Avoid methods using `throws` clause; prefer unchecked exceptions.
- Avoid comments, except for: cron expressions, regex patterns, TODOs, or given/when/then separation in tests.
- Use `@Override` annotation when overriding methods.
- Avoid `Objects.isNull()` and `Objects.nonNull()` for one or two variables; prefer direct null checks.
- Wrap multiple conditions in a boolean variable for better readability.
- Prefer early returns.
- Avoid `else` statements when not necessary.

## Lombok Annotations

- Use `@RequiredArgsConstructor` for dependency injection via constructor.
- Use `@Slf4j` for logging.
- Use `@Builder` for complex object creation when construction does not bypass entity invariants. Do not use a setter prefix.
- Avoid `@Data`; prefer `@Getter` and add `@Setter` only where unrestricted mutation is intentional.

## Spring Annotations

- **`@Service`**: for business logic classes. The project names most of them `*UseCase`.
- **`@Repository`**: optional for Spring Data interfaces that extend `JpaRepository`; Spring Data detects them automatically.
- **`@RestController`**: for web controllers.
- **`@Component`**: for generic Spring components.
- **`@Configuration`**: for Spring configuration classes.
- **`@Autowired`**: prefer constructor injection for production code; field injection is acceptable only in tests.
- **`@ConfigurationProperties`**: for binding related properties. Avoid multiple `@Value` annotations; use this when there are more than 2 properties.
- **`@Transactional`**: annotate transactional methods explicitly. Prefer `@Transactional(readOnly = true)` for read-only operations. Do not annotate service classes at class level unless every method is transactional.
- **`@Validated`**: to enable Bean Validation on method parameters or classes.
- **`@PreAuthorize`**: at the controller layer when using Spring Security to enforce method-level security.
- **`@Order`**: allowed only for ordering initialization/bootstrap components. Avoid it for dependency resolution.
- Avoid circular dependencies.

## Project Conventions

### Database migrations

- Create migrations with `make migration <name>` so the filename uses the current UTC timestamp (`yyyyMMddHHmmss`).
- Do not invent sequential or artificial timestamps.
- Once a migration is applied to a persistent environment, never modify or rename it.
- If a filename must be corrected before production and the affected environment is disposable, recreate that environment instead of using Flyway repair, out-of-order execution, or ignored migration patterns.

### Package structure

Organize code by domain under `ar.edu.utn.frvm.typeit.boero_api`:

- `auth.*`
- `authorization.*`
- `institutional.*`
- `security.*`
- `common.*`
- `config.*`

Each domain package contains sub-packages such as:

- `controllers`
- `services` (or `*UseCase` classes annotated with `@Service`)
- `interfaces` (repositories)
- `entities`
- `payloads` (requests/responses)
- `exceptions`
- `config`, `security`, `filters` as needed

### DTOs and payloads

- Use Java records for request/response/payload DTOs.
- Use `@Builder` on records when fluent construction improves readability.
- Keep mapping simple: response records may contain a static factory method `public static X from(Entity entity)`.

### Entities

- JPA entities remain classes.
- Entities should contain behavior that naturally belongs to their state instead of serving only as data containers.
- Prefer intention-revealing methods such as `activate()`, `deactivate()`, `rename(...)`, `revoke(...)` or `changeAddress(...)` over public setters.
- An entity may inspect its existing relationships to derive its own state or validate a change. For example, a user may consider its person and institution when deciding whether authentication is allowed.
- An entity may call side-effect-free methods on a related entity when that behavior is required for its own decision.
- Do not let an entity arbitrarily mutate a related entity. A user may inspect whether its institution is active, but must not activate or deactivate the institution.
- Keep repository access, persistence coordination, transactions, cache operations, notifications and other I/O in use cases or infrastructure services.
- Keep workflows that coordinate multiple independent entities in a use case. Move only state changes, calculations and invariants that clearly belong to one entity into that entity.
- Validate relationship consistency inside the entity when it is intrinsic to that relationship, such as matching institution ownership or compatible role and permission scopes.
- Prefer named factory methods when entity creation has invariants. Builders remain acceptable for DTOs, tests, bootstrap data and entities whose construction has no relevant rules.
- Avoid class-level `@Setter` on entities with behavior. Expose only the mutation required by JPA and intention-revealing methods.
- Do not add artificial behavior to reference entities such as geographic catalogs when no business rule exists.
- Do not introduce formal DDD patterns solely to make entities richer. The goal is cohesive behavior, not aggregates, domain services or domain events.
- Add focused unit tests for entity behavior, invalid transitions and relationship invariants.

### Soft delete and reusable identifiers

- When a soft-deleted entity has a reusable natural identifier, enforce uniqueness only for active rows with a database partial unique index.
- Creation checks and operational lookups for that identifier must use the same active-row predicate as the index.
- Historical lookups may include deleted rows only through an explicitly historical path, such as a lookup by stable entity ID.

### API versioning

- Use the `Version` enum (e.g. `Version.V1`) and the `@PostMapping(version = ..., path = "...")` pattern.
- Use `UnversionedRestController` for controllers that do not need versioning.

### Security

- Dual authentication model: institutional (document + institution + password) and platform (email + password).
- JWT access tokens + refresh tokens with family rotation.
- Redis is used for token blacklisting.
- Use method-security annotations (`@RequiresPlatformRole`, `@RequiresPermission`, `@RequiresAnyPermission`) where appropriate.

### Git workflow

- Follow Conventional Commits (see `.agents/skills/conventional-commit/SKILL.md`).
- Suggested branch names: `feat/...`, `fix/...`, `refactor/...`, `docs/...`.
- Add any new required environment variables to `.env.example`.

## Exception Handling

- Custom application exceptions must not depend on Spring Web. Extend `ApplicationException` and select an `ErrorCategory`.
- Keep HTTP translation in `ApplicationExceptionHttpMapper` and `GlobalExceptionHandler`.
- Return the `ExceptionPayload` record with status, message and optional field errors.
- Centralize error messages in dedicated `*Messages` classes (e.g. `AuthMessages`, `ErrorMessages`).

## Testing

- Use JUnit 5 for unit and integration testing.
- Use Mockito for mocking dependencies.
- Use `@WebMvcTest(ControllerClass.class)` for testing Spring MVC controllers.
- Use `@DataJpaTest` for repository tests.
- Use `@SpringBootTest` for integration tests that require the full Spring context.
- Use `@MockitoBean` to mock Spring beans in slice tests.
- Use descriptive camelCase method names and `@DisplayName` to describe behavior.
- Avoid reflection in tests.
- Avoid business logic in tests; focus on behavior verification.

## Logging

- Use `@Slf4j` from Lombok.
- Use a lightweight structured format: `[Context] Action/message, key1: {}, key2: {}`.
- Placeholders (`{}`) MUST be used instead of String concatenation.
- All HTTP requests automatically include `requestId` in MDC via `RequestLoggingFilter` and return `X-Request-Id` response header.

### Log Levels
- **`ERROR`**: Unexpected server errors or unhandled exceptions that require investigation. Must include stack trace and be handled centrally in `GlobalExceptionHandler`.
- **`WARN`**: Abnormal but handled conditions (e.g. system fallbacks, rate limiting, external service unavailability). Do NOT use `WARN` for normal business validation rejections.
- **`INFO`**: Significant normal business operations (e.g. successful login, user creation, session revocation, institution update).
- **`DEBUG`**: Detailed technical diagnostics (e.g. cache operations, permission resolution details). Disabled for application packages in production.

### Sensitive Data Policy
- **NEVER** log passwords, JWTs, refresh tokens, authorization headers, cookies, or secrets.
- Avoid logging raw personal data (document numbers, emails, phone numbers). Prefer internal IDs (`userId`, `personId`, `institutionId`, `sessionId`, `roleId`).

### Exception Policy
- An unexpected error must generate **a single `ERROR` log with stack trace** at `GlobalExceptionHandler`. Internal layers should propagate exceptions rather than logging redundant stack traces.
- Expected HTTP/business errors (`400`, `401`, `403`, `404`, `409`) must NOT generate `ERROR` stack traces.

### Examples
- **Correct (`INFO`):** `log.info("[Auth] Login succeeded, userId: {}, institutionId: {}", userId, institutionId);`
- **Correct (`DEBUG`):** `log.debug("[Authorization] Authority snapshot resolved, userId: {}", userId);`
- **Incorrect:** `log.info("User login request: " + request);` *(Exposes passwords/DTOs and uses concatenation)*
