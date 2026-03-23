---
name: quick-build
description: Build the Android app and report results
allowed-tools: Bash(./gradlew:*)
argument-hint: "[debug|release]"
user-invocable: true
---

# /quick-build — Build Android App

Builds the Mooney Android app and reports any compilation errors.

**Arguments:** `$ARGUMENTS` (optional: "debug" or "release", defaults to debug)

## Steps

1. Run the build:
```bash
./gradlew :composeApp:assemble${ARGUMENTS:-Debug} 2>&1 | tail -30
```

2. Report results:
   - If successful: confirm and note APK location
   - If failed: show the relevant error lines and suggest fixes
