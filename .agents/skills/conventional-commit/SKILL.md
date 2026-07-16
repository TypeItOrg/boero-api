---
name: conventional-commit
description: 'Prompt and workflow for generating conventional commit messages using a structured XML format. Guides users to create standardized, descriptive commit messages in line with the Conventional Commits specification, including instructions, examples, and validation.'
---

### Instructions

```xml
	<description>This file contains a prompt template for generating conventional commit messages. It provides instructions, examples, and formatting guidelines to help users write standardized, descriptive commit messages in accordance with the Conventional Commits specification.</description>
```

### Workflow

**Follow these steps:**

1. Run `git status` to review changed files.
2. Run `git diff` or `git diff --cached` to inspect changes.
3. Group changes by logical intent (feature, fix, refactor, docs, infra, etc.). Each group must represent one cohesive purpose.
4. For each group:
   - Select only the files/hunks that belong to that intent.
   - Stage them with `git add <file>` (or partial staging if needed).
   - Construct one Conventional Commit message using the XML structure below.
5. Generate one message per group (not a single message for all changes).
6. After generating each commit message, run the following command in your integrated terminal (no confirmation needed):

```bash
git commit -m "type(scope): description"
```

7. Repeat until all groups are committed.

### Commit Message Structure

```xml
<commit-message>
	<type>feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert</type>
	<scope>()</scope>
	<description>A short, imperative summary of the change in all lowercase</description>
	<footer>(optional: e.g. breaking change: details, or issue references)</footer>
</commit-message>
```

### Examples

```xml
<examples>
	<example>feat(parser): add support for array parsing</example>
	<example>fix(ui): adjust main button alignment</example>
	<example>docs(auth): document admin namespace and security boundary</example>
	<example>refactor(db): optimize user queries for performance</example>
	<example>chore(dev): add staging and log targets to makefile</example>
	<example>feat(auth): isolate platform admin actions to admin namespace</example>
</examples>
```

### Validation

```xml
<validation>
	<type>Must be one of the allowed types. See <reference>https://www.conventionalcommits.org/en/v1.0.0/#specification</reference></type>
	<scope>Optional, but recommended for clarity. Must be in lowercase.</scope>
	<description>Required. Use the imperative mood, all lowercase, and balanced specificity. Do not be overly specific (e.g., listing filenames, classes, or parameter details) but avoid being too generic (e.g., "update docs", "fix bug", "update file").</description>
	<footer>Use for breaking changes or issue references.</footer>
</validation>
```

### Final Step

```xml
<final-step>
	<cmd>git commit -m "type(scope): description"</cmd>
	<note>Replace with the constructed message for each logical group. Do not use commit body. Make sure type, scope, and description are all lowercase.</note>
</final-step>
```
