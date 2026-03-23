# Tech Debt & Refactoring Roadmap

Living document. Updated each tech debt resolution session.

---

## Phase 0: Remove Unused Code & Features (Do First)

### Dead Code
- [ ] **Book routes in Route.kt** — `BookGraph`, `BookList`, `BookDetail` are tutorial leftovers. All NavigationHost references commented out. Delete them.
- [ ] **Commented-out recurring transaction UI in TransactionScreen.kt** — ~60 lines of dead code for `RecurringTransactionDialog`, `PendingTransactionItem`. DB entities must stay, but UI code goes.
- [ ] **Unused params in TransactionsScreenContent** — `onAcceptPending`, `onRejectPending`, `onEditPending` defined but never wired.
- [ ] **Test function in production** — `AccountViewModel.testReconciliationDialog()` (lines 184-213) with `println` statements. Delete entirely.
- [ ] **`sharedKoinViewModel` in NavigationHost** — already removed but verify no remnants.

### Features to Remove Entirely
- [ ] **Asset analytics / diversification** — Remove `CalculateAssetDiversificationUseCase`, the `updateAssetsAnalytics()` method in AssetsViewModel (65 lines), and all related pie chart / percentage breakdown UI. Simplify AssetsScreen to just list accounts by category.
- [ ] **Dynamic theme switching** — Remove `ThemeSwitcher.kt` and theme selection UI from Settings. Pick one theme and hardcode it.
- [ ] **Smart suggestions for recurring transactions** — Remove `PendingTransactionEntity` logic that auto-detects patterns. Keep only user-created recurring transactions (manual setup). The `PendingTransactionEntity` and `RecurringTransactionEntity` tables must remain in schema for DB integrity, but remove all smart-suggestion logic.

### Features to Reassess
- [ ] **Export/Import feature** — `DataExportImportManager`, `FileHandler`, `DataExportImportSection` in Settings. Originally caused DB issues. Decide: keep and fix, or remove until Firebase migration is done.

---

## Phase 1: Critical Architecture Fixes

### runBlocking on Main Thread (ANR risk)
- [ ] **AnalyticsViewModel lines 191-199** — `runBlocking { getTransactionsUseCase().first() }` blocks UI thread. Convert to `suspend` function inside `viewModelScope.launch`.
- [ ] **AnalyticsViewModel lines 280-288** — Second `runBlocking` call, same issue.

### Layer Boundary Violations
- [ ] **AnalyticsUseCases.kt lines 8-9** — Domain layer imports presentation (`TopCategorySummary`, `formatWithCommas`). Move `TopCategorySummary` to domain, move `formatWithCommas` to a shared util or keep in presentation and do the mapping in ViewModel.

### Direct Data Layer Access from Presentation
- [ ] **AccountViewModel line 330** — `repository.upsertAccount()` bypasses use case layer in `performAccountMetadataUpdate()`. Route through `AddAccountUseCase`.

### Test Data in Production Code
- [ ] **AnalyticsViewModel line 50** — `GlobalConfig.testExchangeRates` passed to calculator constructors. Should use injected exchange rates via `CurrencyManagerUseCase`.
- [ ] **ExchangeViewModel lines 143-148** — Hardcoded base rates (3.67, 4.35, 0.1, 1.0) and `Random.nextDouble()` for test data. Replace with actual API data or remove sample generation.

---

## Phase 2: Extract Use Cases from ViewModels

### AnalyticsViewModel (572 lines — biggest offender)
- [ ] **Extract `CategoryAnalysisUseCase`** — from `getAllCategoriesForSheetType()` (lines 290-358, 69 lines). Groups transactions by category, calculates totals, trends, percentages.
- [ ] **Extract `HistoricalAnalyticsUseCase`** — from historical data aggregation (lines 149-181, 33 lines). Generates month-over-month summaries.
- [ ] **Extract `TaxCalculationUseCase`** — from `calculateTaxes()` (lines 142-147). Filters for "ZUS" and "PIT" transaction categories.
- [ ] **Extract metric calculation logic** — from lines 109-133. Complex conditional metric computation (25 lines).

### AssetsViewModel
- [ ] **Extract asset-to-UI conversion** — `convertToUiAssets()` (lines 178-204) performs exchange rate calculations. Should be a use case.
- [ ] **Clean up `onNetWorthLabelClick()`** — lines 222-231 mix currency cycling + total update + analytics recalculation.

### TransactionViewModel
- [ ] **Extract daily total aggregation** — lines 129-134 do `.groupBy { it.date.dayOfMonth }.mapValues { ... }` inline. Should use `CalculateDailyTotalUseCase` (which exists but isn't used here).
- [ ] **Extract date range filtering** — lines 89-97 filter transactions by month boundaries. Should be in use case.

### GoalsViewModel
- [ ] **Extract goal construction** — lines 147-164 build `Goal` objects with defaults. Should be `CreateGoalUseCase`.

### SettingsViewModel
- [ ] **Extract pinned category validation** — lines 166-178 do max limit checking. Should be `ValidatePinnedCategoriesUseCase`.
- [ ] **Fix state update inconsistency** — lines 53, 65 use `.value = ...copy()` instead of `.update {}`.

---

## Phase 3: Repository Refactoring

### DefaultCoreRepositoryImpl (207 lines, too fat)
- [ ] **Extract transfer category construction** — lines 69-107 contain 38 lines of dynamic category creation logic (checking `transfer_to_` prefix, looking up destination accounts). Should be a `TransferCategoryService` or use case.
- [ ] **Remove duplicated logic** — `getTransactionById()` (lines 109-142) repeats the same transfer category construction.
- [ ] **Separate category usage tracking** — lines 160-175 conflate tracking (business rule) with persistence (data concern).

---

## Phase 4: Architecture Improvements

### Remove GlobalConfig Anti-Pattern
- [ ] **`GlobalConfig.testExchangeRates`** — used in AnalyticsViewModel, AssetsViewModel, ExchangeViewModel. Replace with injected rates via `CurrencyManagerUseCase` everywhere.
- [ ] **`GlobalConfig.baseCurrency`** — should be a user preference, not a hardcoded global.

### State Management Consistency
- [ ] All ViewModels must use `_uiState.update { it.copy(...) }` — never `_uiState.value = ...`
- [ ] Audit all 7 ViewModels for consistent pattern.

### Missing Use Cases to Create
- [ ] `CategoryAnalysisUseCase` — category grouping/trending for analytics
- [ ] `HistoricalAnalyticsUseCase` — month-over-month aggregation
- [ ] `TaxCalculationUseCase` — ZUS/PIT filtering
- [ ] `CurrencyConversionUseCase` — exchange rate display
- [ ] `ValidatePinnedCategoriesUseCase` — pinned category max limit
- [ ] `CreateGoalUseCase` — goal object construction with defaults

---

## Metrics

| Metric | Current | Target |
|--------|---------|--------|
| Use cases with tests | 1/22 | 22/22 |
| ViewModels with tests | 0/7 | 7/7 |
| Business logic in ViewModels | 15+ instances | 0 |
| `runBlocking` calls | 2 | 0 |
| Layer boundary violations | 1 | 0 |
| Lines in largest ViewModel | 572 (Analytics) | <200 |

---

*Last updated: 2026-03-23*
