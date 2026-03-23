---
description: Testing conventions and philosophy
globs: "**/*Test*.kt"
---

# Testing Conventions

## Philosophy

Test business logic meaningfully — never write tests just for coverage.

## Rules

- Test every business logic path (if/else branches, error handling, state transitions)
- Use `runTest` + fake dependencies for ViewModel tests
- Use `assertFailsWith` for exception tests, never `try/catch` without `fail()`
- Test behavior, not implementation — verify what the code does, not how

## Good vs Bad Tests

```kotlin
// CORRECT — tests real behavior
@Test fun `fetchData sets loading then updates with response`() = runTest {
    val viewModel = createViewModel(repository = FakeRepository(response = mockData))
    viewModel.fetchData()
    assertEquals(mockData, viewModel.uiState.value.data)
}

// WRONG — tests nothing meaningful
@Test fun `getter returns value`() {
    val state = UiState(isLoading = true)
    assertEquals(true, state.isLoading)
}
```

## Financial Calculation Tests

- Always test edge cases: zero amounts, negative amounts, currency conversion rounding
- Test with multiple currencies when applicable
- Verify account balance calculations are correct after transactions
