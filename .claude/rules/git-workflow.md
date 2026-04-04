---
description: Git commit and branching rules for Mooney project
globs: "**/*"
---

# Git Workflow Rules

## Branching Strategy

- **`dev`** — default working branch. All daily work, features, and fixes go here.
- **`master`** — release branch. Merging to master triggers automatic TestFlight deployment.
- **ALWAYS push to `dev`** unless explicitly told to release/deploy.
- **To release:** merge `dev` into `master` and push master. Only do this when the user asks to release.
- **NEVER push directly to `master`** for regular work — always go through `dev`.

## Commit Rules

- **NEVER use `git add .` or `git add -A` or `git add *`** — always add files explicitly by name
- **NEVER mention AI, Claude, or automated generation** in commit messages
- **NEVER add Co-Authored-By trailers** to commits
- **Commit messages must be short human-like one-liners** — no multi-line, no verbose explanations
- Examples: "Fix dark mode text contrast", "Add USoftware salary category", "Restore Room DB v8 schema"
