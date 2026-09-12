---
name: conventional-commit
description: Draft Conventional Commit messages or create requested commits from selected changes.
---

# Conventional commits

Use the user's request to distinguish message drafting from committing.

- For a message or review request, inspect the relevant diff and return the message. Do not stage or commit.
- For an authorized commit, inspect the branch, worktree and index; group only the requested changes by logical intent. Preserve unrelated files and existing staged work. Use exact paths or hunks; an existing staged change is not automatically in scope.
- Complete authorized commits without repeated confirmation. Push only when requested or already authorized separately. Do not switch branches, amend history or expand the commit scope to resolve an unrelated problem.
- If a required hook fails, fix failures caused by the selected changes. Report unrelated failures without bypassing hooks or including unrelated fixes.

Use `type(scope): description`, with a lowercase imperative subject and a scope that identifies the affected area. Types: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert. Separate commits when the requested changes have independent purposes.

Add a body or footer when it clarifies a substantive change, records an issue, or explains a breaking change; honor a user request for a subject-only message. Do not wrap messages in XML.

Before committing, verify that the staged content of the selected paths matches the intended diff. If unrelated staging shares a selected file, isolate the approved hunks without discarding the user's index state.

Return the created commit identifiers and a concise description of their scope, or the drafted message when no commit was requested.
