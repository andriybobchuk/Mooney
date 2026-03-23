---
description: Kotlin coding conventions for Mooney KMP project
globs: "**/*.kt"
---

# Kotlin Conventions

## Coroutine Error Handling

**NEVER swallow `CancellationException` in coroutines**

```kotlin
import kotlin.coroutines.cancellation.CancellationException

// CORRECT
try {
    apiCall()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    _uiState.update { it.copy(error = e.message) }
}

// WRONG — breaks structured concurrency
try { apiCall() } catch (e: Exception) { }

// WRONG — runCatching catches CancellationException
viewModelScope.launch { runCatching { apiCall() }.onFailure { } }
```

## State Management

- Use `_uiState.update { it.copy(...) }` — atomic updates, never `_uiState.value = ...`
- UiState must be an immutable `data class` with `val` only
- Always handle loading, success, AND error states
- Clear loading state in error path (prevent stuck loading)

## Lifecycle

- Use `collectAsStateWithLifecycle()`, never `collectAsState()`
- Use `LaunchedEffect` for one-time events in Composables
- Never hold Context references in ViewModels
- Never use `print()`/`println()` — use proper logging

## Code Organization

- ViewModels: Business logic and state management only
- Screens: UI rendering only — all interactions go through `onAction`
- Use Cases: One public `operator fun invoke()` per use case
- Extract complex Composable sections as private `@Composable` functions
