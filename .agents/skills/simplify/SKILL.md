---
name: simplify
description: Simplify a requested code scope or diff while preserving behavior and public contracts.
---

# Simplify touched code

Use this skill when simplification or cleanup is part of the request, not for every code edit. Apply the repository's AGENTS.md and conventions of the affected code.

Remove unused code and needless indirection when references and contracts support it. Keep abstractions that enforce authorization, validation, transaction boundaries, error handling or shared UI behavior. Prefer a direct implementation over introducing a new framework for a small duplication.

Preserve public contracts, observable behavior and unrelated work. Comments explaining invariants or surprising behavior are useful; comments restating syntax are not. Use the language's existing style rather than importing conventions from another stack.

Complete the requested scope. Do not begin a repository-wide cleanup, add tests or run suites merely because this skill was loaded. Use the verification authorized for the task and report material limitations.
