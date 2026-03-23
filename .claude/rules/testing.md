---
description: Testing conventions, patterns, and requirements for Mooney
globs: "**/*Test*.kt"
---

# Testing Rules — Mooney

## Every Use Case Must Have Tests

This is non-negotiable. Use cases contain ALL business logic. Untested business logic = bugs in production with people's money.

## Test Infrastructure

- **Base class:** `ViewModelTestBase` — sets up `Dispatchers.Main` for coroutine testing
- **Fake DAOs:** `FakeDaos.kt` — in-memory implementations of all Room DAOs
- **Flow testing:** Use Turbine (`app.cash.turbine`) for testing Flow emissions
- **Coroutines:** Use `kotlinx-coroutines-test` with `runTest`

## Test File Location

```
commonTest/kotlin/com/andriybobchuk/mooney/
├── domain/usecase/
│   ├── CalculateNetWorthUseCaseTest.kt
│   ├── AddTransactionUseCaseTest.kt
│   └── ...
├── presentation/
│   ├── TransactionViewModelTest.kt
│   └── ...
└── testutil/
    ├── ViewModelTestBase.kt
    ├── FakeDaos.kt
    └── TestFixtures.kt (create as needed)
```

## Use Case Test Pattern

```kotlin
class CalculateNetWorthUseCaseTest {
    // 1. Set up fakes/test data
    private val fakeProvider = object : ExchangeRateProvider { ... }
    private val sut = CalculateNetWorthUseCase(CurrencyManagerUseCase(fakeProvider))

    // 2. Test each behavior path
    @Test fun `single account in base currency`() { ... }
    @Test fun `converts foreign currency correctly`() { ... }
    @Test fun `empty accounts returns zero`() { ... }
    @Test fun `negative amounts handled correctly`() { ... }

    // 3. Helper factory for test data
    private fun account(amount: Double = 0.0, currency: Currency = Currency.PLN) = Account(...)
}
```

## ViewModel Test Pattern

```kotlin
class TransactionViewModelTest : ViewModelTestBase() {
    // Inject fakes for all use case dependencies
    private val fakeTransactionDao = FakeTransactionDao()
    private val getTransactionsUseCase = GetTransactionsUseCase(/* with fakes */)

    private fun createViewModel() = TransactionViewModel(
        getTransactionsUseCase = getTransactionsUseCase,
        // ... other use cases with fakes
    )

    @Test fun `initial state has empty transactions`() = runTest {
        val vm = createViewModel()
        assertEquals(emptyList(), vm.state.value.transactions)
    }

    @Test fun `adding transaction updates state`() = runTest {
        val vm = createViewModel()
        vm.upsertTransaction(testTransaction)
        advanceUntilIdle()
        assertTrue(vm.state.value.transactions.isNotEmpty())
    }
}
```

## What to Test

### Must Test (CRITICAL — financial accuracy):
- Currency conversion calculations
- Net worth computations
- Transaction total calculations (daily, monthly, by category)
- Account balance changes after transactions
- Transfer logic (money moves between accounts correctly)
- Goal progress calculations

### Must Test (state management):
- Initial ViewModel state
- State after each action
- Error states (loading cleared, error message set)
- Loading states (set before async, cleared after)

### Should Test (edge cases):
- Empty lists
- Single item
- Zero amounts
- Negative amounts
- Same currency conversions (should be identity)
- Multiple currencies in same calculation

### Don't Test:
- Compose UI rendering (use @Preview instead)
- Koin module wiring (integration test, not unit)
- Room SQL queries (tested by Room's own test suite)
- Simple getters/setters with no logic

## Rules

- Use `assertFailsWith` for exception tests, never `try/catch`
- Use `assertEquals(expected, actual, delta)` for Double comparisons
- Name tests descriptively: `fun \`net worth converts foreign currency to base\`()`
- One assertion per test when possible (clear failure messages)
- Use factory helpers for test data (don't repeat Account/Transaction construction)
