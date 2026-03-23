---
name: test-all
description: Run all unit tests and report results
allowed-tools: Bash(./gradlew:*)
user-invocable: true
---

# /test-all — Run All Tests

Executes all unit tests and reports results.

## Steps

1. Run tests:
```bash
./gradlew test 2>&1 | tail -40
```

2. Report results:
   - If all pass: confirm with count
   - If failures: show failed test names and relevant error messages
   - Suggest fixes for common failures
