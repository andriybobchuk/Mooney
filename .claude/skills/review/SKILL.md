---
name: review
description: Principal engineer-level code audit. Analyzes changed files for bugs, concurrency issues, memory leaks, logical errors, edge cases, and platform-specific gotchas.
model: claude-opus-4-6
allowed-tools: Read,Glob,Grep,Bash(git:*),Bash(./gradlew:*)
argument-hint: [all|path/to/file]
user-invocable: true
disable-model-invocation: true
---

# /review — Principal Engineer Code Audit

Deep code review of implementation changes. Reads every changed file, reasons about correctness, and reports bugs, concurrency issues, memory leaks, logical errors, and edge cases.

**Usage:**
- `/review` — Review all changes on current branch vs main/master
- `/review all` — Same as above
- `/review path/to/File.kt` — Review a single file

**Arguments:** `$ARGUMENTS`

## Step 1: Identify Scope

### If reviewing branch changes (default or `all`):

```bash
git diff --name-only main...HEAD 2>/dev/null || git diff --name-only master...HEAD
```

Also check uncommitted changes:
```bash
git diff --name-only
git diff --name-only --staged
```

Categorize files:
- `.kt` files → Kotlin review
- `Screen.kt` → UI review
- `ViewModel.kt` → State management review
- `Entity.kt` → Database review
- `Test.kt` → Test coverage review

### If reviewing a single file:
Use provided path and also read related files (ViewModel if reviewing Screen, etc.)

## Step 2: Read ALL Changed Files

Read every changed file completely. For each file, also read:
- The ViewModel if reviewing a Screen
- The State/Action/Event classes if reviewing a ViewModel
- The Repository if reviewing a UseCase
- The migration if reviewing an Entity

## Step 3: Audit Each File

For every changed file, check ALL of the following categories. Think carefully about each one — this is the most important step.

### 3a. Concurrency & Threading

**KMP (Kotlin):**
- `viewModelScope.launch` without specifying dispatcher — is Main needed?
- Multiple `launch` blocks mutating `_uiState` — race condition?
- `_uiState.value = ...` instead of `_uiState.update { }` — not atomic
- Missing `@Volatile` import (`kotlin.concurrent.Volatile` required in commonMain)
- `SharedFlow` emitted from background thread without `flowOn(Dispatchers.Main)`
- `suspend` functions called without proper scope/cancellation handling
- `runBlocking` usage in production code (blocks thread)

**Android (Compose):**
- `collectAsState()` instead of `collectAsStateWithLifecycle()` — collects when backgrounded
- Side effects not wrapped in `LaunchedEffect`/`DisposableEffect`
- Missing `remember { }` for lambdas/objects that cause unnecessary recomposition
- `mutableStateOf` without `remember` — resets on every recomposition

### 3b. Error Handling

**Critical Issues:**
- `catch (e: Exception)` without rethrowing `CancellationException`
- `runCatching` in coroutines — swallows cancellation
- Force unwrap (`!!`) on nullable types
- Empty catch blocks without logging
- Missing error state in UI

### 3c. Memory & Lifecycle

- ViewModel holding Context reference
- Missing cleanup in `onCleared()`
- Coroutines not cancelled properly
- Observers not removed
- Large objects in state that should be computed

### 3d. Database & Room

- Non-nullable fields in Entity that should be nullable
- Missing migration for schema changes
- Wrong type converters
- Missing indices on frequently queried columns
- Transaction not used for multi-table operations

### 3e. Architecture Violations

- Business logic in UI layer
- Direct database access from ViewModel
- Repository returning Entity instead of domain model
- UseCase doing more than one thing
- Hardcoded strings in UI

### 3f. Testing Gaps

- New ViewModel methods without tests
- New state transitions without test coverage
- Error paths not tested
- Edge cases not covered
- Missing @Preview for new composables

### 3g. Mooney-Specific Checks

- Transaction categories properly validated
- Account balance calculations correct
- Currency conversion handled properly
- Recurring transactions logic sound
- Financial calculations use proper decimal handling

### 3h. Code Quality

- Unused imports or variables
- Commented-out code left in
- TODO comments that should be addressed
- Inconsistent naming conventions
- Complex functions that need refactoring (>30 lines)

## Step 4: Output Structured Review

Format the review as:

```
Code Review — Mooney Finance App
================================
Branch: {branch} → main
Files reviewed: {N}
Focus: {feature area}

{For each issue:}

[CRITICAL] {file}:{line}
  {Category}: {One-line description}
  {Why this is a problem}
  Fix: {Concrete fix suggestion}

[WARNING] {file}:{line}
  {Category}: {Description}
  {Explanation}
  Fix: {Suggestion}

[SUGGESTION] {file}:{line}
  {Category}: {Description}
  Consider: {Improvement}

Clean files: {list files with no issues}
================================
Summary: {N} critical, {N} warnings, {N} suggestions
{Overall assessment in 1-2 sentences}
```

**Severity levels:**
- **CRITICAL** — Will cause crashes, data loss, or incorrect financial calculations
- **WARNING** — Likely bug, memory leak, or state management issue
- **SUGGESTION** — Code improvement or best practice violation

## Rules

- **Read-only** — Never modify files, only report findings
- **Be specific** — Include file paths and line numbers
- **No false positives** — Only report issues you're confident about
- **Prioritize** — Financial calculation errors are always CRITICAL
- **Focus on the diff** — Don't review unchanged code
- **Think production** — What could fail when users depend on this for finances?