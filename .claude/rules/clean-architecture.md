---
description: Clean Architecture enforcement - SOLID principles, layer boundaries, use case patterns
globs: "**/*.kt"
---

# Clean Architecture Rules — Mooney

## Layer Dependency Rules

```
Presentation → Domain → (Data interfaces only)
Presentation NEVER imports Data directly
Domain NEVER imports Presentation or Data implementations
Data implements Domain interfaces
```

**Violations to flag immediately:**
- ViewModel importing a DAO directly (must go through UseCase → Repository)
- Screen/Composable containing business logic (must be in UseCase)
- UseCase importing Android/iOS platform code
- Domain layer importing Room, Ktor, or DataStore classes

## Everything Is a Use Case

**ALL business logic MUST live in a UseCase class.** No exceptions.

ViewModels are orchestrators — they call use cases and manage UI state. They must NOT contain:
- Calculations (net worth, totals, conversions, analytics)
- Data transformations (entity → domain model, grouping, filtering)
- Validation logic (form validation, input sanitization)
- Business rules (transfer logic, recurring transaction processing)

**Pattern:**
```kotlin
// CORRECT — logic in use case
class CalculateMonthlyTotalUseCase {
    operator fun invoke(transactions: List<Transaction>, month: MonthKey): Double {
        return transactions.filter { it.date.month == month }.sumOf { it.amount }
    }
}

// WRONG — logic in ViewModel
class TransactionViewModel(...) : ViewModel() {
    fun getMonthlyTotal(): Double {
        return state.value.transactions.filter { ... }.sumOf { it.amount }
    }
}
```

## Use Case Design Principles

1. **Single Responsibility** — one use case does exactly one thing
2. **Pure Functions When Possible** — `operator fun invoke()` with inputs and outputs, no side effects
3. **Constructor Injection** — all dependencies via constructor (for testability)
4. **No Framework Dependencies** — use cases must not depend on ViewModel, Compose, Koin, or Android/iOS
5. **Return Domain Models** — never return entities or DTOs from use cases

## SOLID Enforcement

### Single Responsibility
- One class = one reason to change
- If a UseCase has more than ~30 lines, consider splitting

### Open/Closed
- Extend behavior through new use cases, not by modifying existing ones
- Use interfaces for repositories and data sources

### Liskov Substitution
- Fake implementations in tests must behave identically to real ones
- All DAO fakes must maintain data consistency (insert then query = same data)

### Interface Segregation
- Repository interfaces should be focused (not one god-repository)
- DAOs already follow this pattern — keep it

### Dependency Inversion
- Domain layer defines interfaces (Repository, ExchangeRateProvider)
- Data layer implements them
- Presentation depends on domain abstractions, never data implementations

## Low Coupling / High Cohesion

**Low Coupling:**
- ViewModels only know use cases (never DAOs, never repositories)
- Use cases only know repository interfaces (never implementations)
- Screens only know state + action callback (never ViewModel internals)

**High Cohesion:**
- Group related use cases in subdirectories (`usecase/assets/`, `usecase/settings/`)
- Keep feature presentation together (`presentation/assets/` = ViewModel + Screen + components)
- Domain models live together in `domain/Model.kt` or `domain/` package

## When Creating New Code

1. **Need a calculation?** → Create a UseCase
2. **Need to fetch data?** → Create a UseCase that calls Repository
3. **Need to transform data for UI?** → Create a UseCase (e.g., `ConvertAccountsToUiUseCase`)
4. **Need to validate input?** → Create a UseCase
5. **Need to combine multiple operations?** → Create a UseCase that composes other use cases
6. **Displaying data?** → ViewModel calls use cases, exposes state, Screen renders it
