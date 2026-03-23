---
name: new-feature
description: Scaffold a new feature with ViewModel, Screen, and DI registration
model: claude-sonnet-4-6
allowed-tools: Read,Write,Edit,Glob,Grep,Bash(git:*)
argument-hint: FeatureName
user-invocable: true
---

# /new-feature — Scaffold a New Feature

Creates the full structure for a new Mooney feature following established patterns.

**Arguments:** `$ARGUMENTS` (the feature name, e.g., "Budget")

## Step 1: Research Existing Patterns

Before creating anything, read an existing feature for reference:
- `mooney/presentation/assets/AssetsViewModel.kt` — ViewModel pattern
- `mooney/presentation/assets/AssetsScreen.kt` — Screen pattern
- `di/Modules.kt` — DI registration pattern

## Step 2: Create Files

For feature name `$ARGUMENTS`, create:

1. **`mooney/presentation/{feature}/{Feature}ViewModel.kt`** — UiState (data class), Actions (sealed interface), Events (sealed class), ViewModel with `onAction()`
2. **`mooney/presentation/{feature}/{Feature}Screen.kt`** — Composable with state + onAction params
3. **`mooney/domain/usecase/{Feature}UseCase.kt`** — if business logic needed

## Step 3: Wire Up

1. Register ViewModel in `di/Modules.kt` → `viewModelOf(::{Feature}ViewModel)`
2. Add route in `app/Route.kt`
3. Add composable in `app/NavigationHost.kt`

## Step 4: Verify

- All files follow naming conventions from CLAUDE.md
- UiState is immutable (val only)
- Actions sealed interface handles all user interactions
- Loading + error states handled
- No hardcoded strings

Report what was created and what the user needs to do next (tests, navigation wiring, etc.)
