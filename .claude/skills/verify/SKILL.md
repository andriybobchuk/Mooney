---
name: verify
description: Run the exact CI gate locally (detekt + Android assemble + unit tests + iOS compile) and report pass/fail. Use before declaring any code change complete.
allowed-tools: Bash(./gradlew:*)
user-invocable: true
---

# /verify — Run the CI Gate Locally

Runs the same checks CI runs, so "passed locally" actually means "will pass CI".
Catches the failure classes that have repeatedly slipped through a bare
`compileDebugKotlinAndroid`: detekt violations (`TooGenericExceptionThrown`,
`TooManyFunctions`, `LongParameterList`) and the iOS target.

## Steps

1. Run the full gate (mirrors `.github/workflows/ci.yml`):
```bash
./gradlew detekt :composeApp:assembleDebug :composeApp:testDebugUnitTest :composeApp:compileKotlinIosArm64 --build-cache 2>&1 | tail -40
```

2. Report results honestly:
   - **All green** → state "CI gate passed" and list which checks ran.
   - **detekt failed** → run `./gradlew detekt 2>&1 | grep -E "^.*\\.kt:[0-9]+"` to surface the exact rule + file:line, then fix and re-run. Common fixes: add a scoped `@Suppress("RuleName")` only when the rule is a false positive, otherwise refactor.
   - **compile/test failed** → show the `e:` / failing-test lines, diagnose root cause, fix, re-run.

3. NEVER report the task complete until this gate is fully green. Paste the final passing tail as proof.

## Notes
- This is the gate, not a substitute for `/review` — run `/review` too for logic/correctness on non-trivial changes.
- If only Kotlin/common code changed and iOS is unaffected, the iOS compile is still worth running; it's cheap with the build cache and catches `expect/actual` and import drift.
