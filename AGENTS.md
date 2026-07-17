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
- Use `@Builder` for complex object creation. Do not use a setter prefix.
- Avoid `@Data`; prefer `@Getter` and `@Setter` for granular control.

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
- Use `@Getter`, `@Setter` and `@Builder`.

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

- Custom domain exceptions extend `ResponseStatusException` when they map directly to an HTTP status.
- Use a global `@RestControllerAdvice` handler for consistent error responses.
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
- Log at appropriate levels: `DEBUG`, `INFO`, `WARN`, `ERROR`.
- Include contextual information (e.g. request IDs, user IDs) without logging sensitive data.
- Use placeholders (`{}`) instead of string concatenation.
- Use a lightweight structured format: `[Context] Action: message, key1: {}, key2: {}`.
    - Example: `log.info("[Auth] login succeeded for userId: {}", userId);`
    - Example: `log.error("[Auth] login failed: errorMessage: {}, userId: {}", errorMessage, userId);`
