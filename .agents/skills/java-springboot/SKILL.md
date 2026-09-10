---
name: java-springboot
description: Resolve Boero API decisions about entity behavior, use-case orchestration, persistence and HTTP boundaries.
---

# Boero API design

Use [the repository contract](../../../AGENTS.md) for decisions that are not settled by adjacent implementation. Generic Spring examples do not override these domain boundaries:

- Entities own their state transitions, calculations and intrinsic relationship invariants. Do not introduce formal DDD machinery or artificial behavior for reference catalogs.
- Use cases coordinate repositories, transactions, cache invalidation, notifications and workflows across entities. An entity may inspect related state but must not arbitrarily mutate another entity.
- Controllers enforce permission boundaries; request/response records define the API. Application exceptions remain independent of Spring Web.
- Changes involving persistent invariants, deletion or reusable identifiers follow the migration, locking and tenant-scoping contract.

Reuse the existing Gradle setup, Lombok constructors/loggers and test fixtures. A design question does not authorize a refactor, dependency change or migration.
