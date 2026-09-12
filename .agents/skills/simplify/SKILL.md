---
name: simplify
description: Review code changed for the current task for unnecessary complexity while preserving behavior and scope.
---

# Simplify touched code

Apply the repository's AGENTS.md and conventions of the affected code. Use this skill for requested simplification or a relevant cleanup of the current change.

Remove unused code and needless indirection when references and contracts support it. Keep abstractions that enforce authorization, validation, transaction boundaries, error handling or shared UI behavior. Prefer a direct implementation over introducing a new framework for a small duplication.

Preserve public contracts, observable behavior and unrelated work. Comments explaining invariants or surprising behavior are useful; comments restating syntax are not. Use the language's existing style rather than importing conventions from another stack.

Complete the requested scope. Do not begin a repository-wide cleanup, add tests or run suites merely because this skill was loaded. Use the verification authorized for the task and report material limitations.
