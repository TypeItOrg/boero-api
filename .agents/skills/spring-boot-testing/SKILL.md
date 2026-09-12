---
name: spring-boot-testing
description: Choose or implement focused Boero API tests using its Spring Boot, Gradle and fixture setup.
---

# Boero API testing

Preserve the requested scope and the repository's AGENTS.md. Use the Spring Boot-managed JUnit Jupiter version and existing fixtures; do not add dependencies, coverage quotas or architecture changes because a tutorial recommends them.

A test case count is not evidence that a method needs refactoring. Complete the requested tests and mention independently useful refactors separately. Do not stop for a refactor discussion unless a concrete problem prevents the requested work.

Read only the reference needed:

| Task | Reference |
| --- | --- |
| Choose between unit, slice and integration tests | [Slice overview](references/test-slices-overview.md) |
| Controller behavior and authorization | [WebMvcTest](references/webmvctest.md); [existing MockMvc assertions](references/mockmvc-classic.md) or [MockMvcTester](references/mockmvc-tester.md) |
| Queries, constraints and transactions | [DataJpaTest](references/datajpatest.md), [PostgreSQL containers](references/testcontainers-jdbc.md) |
| HTTP client behavior | [RestClientTest](references/restclienttest.md), [RestTestClient](references/resttestclient.md) |
| Mock Spring dependencies | [MockitoBean](references/mockitobean.md) |
| Assertions | [Scalar assertions](references/assertj-basics.md), [collection assertions](references/assertj-collections.md) |
| Diagnose slow test startup | [Context caching](references/context-caching.md) |
| Requested Spring Boot test migration | [Boot 4 migration](references/sb4-migration.md) |
| Deliberate use of an existing fixture-generation dependency | [Instancio](references/instancio.md) |

Use fixtures under `src/test/java/ar/edu/utn/frvm/typeit/boero_api/support` and feature-specific test factories. Deterministic lifecycle dates, identifiers and tenant relationships matter more than minimizing fixture lines.

When test execution is authorized, use the narrowest relevant Gradle task: `fastTest` excludes integration tests; `integrationTest` selects `@IntegrationTest` cases; `test` runs the complete suite. Use `--tests` for a focused class/method. Respect an explicit request not to run tests. This skill does not itself authorize test execution or database startup.

Fix and rerun failures caused by the requested change within the authorized scope. Report unrelated failures separately. Check PostgreSQL behavior with PostgreSQL integration tests when required by the repository contract; an H2 slice does not establish locking or foreign-key behavior in PostgreSQL.

Existing MockMvc tests are valid; adopting MockMvcTester is not a prerequisite. If coverage reporting is requested, use Gradle and the agreed target rather than adding a Maven configuration or inventing a threshold.
