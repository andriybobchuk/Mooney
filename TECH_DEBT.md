# Tech Debt & Refactoring Roadmap

Living document. Updated each tech debt resolution session. Read the actual code before acting on any item — references may shift as we refactor.

---

## Phase 0: Remove Unused Code & Features (Do First)

### Dead Code
- [x] **Book routes in Route.kt** — `BookGraph`, `BookList`, `BookDetail` tutorial leftovers. Deleted.
- [x] **Commented-out recurring transaction UI in TransactionScreen.kt** — ~60 lines removed. DB entities stay.
- [x] **Unused params in TransactionsScreenContent** — `onAcceptPending`, `onRejectPending`, `onEditPending` removed.
- [x] **Test function in AccountViewModel** — `testReconciliationDialog()` with `println` statements. Deleted.

### Features to Remove Entirely
- [x] **Asset analytics / diversification** — Removed `CalculateAssetDiversificationUseCase`, `updateAssetsAnalytics()`, `AnalyticsCard`, `CurrencyBar`. Simplified to account list by category.
- [x] **Dynamic theme switching** — Removed `ThemeSwitcher.kt`, theme selection UI, `toggleTheme()` from ViewModels. ThemeManager kept for light/dark mode.
- [x] **Smart suggestions for recurring transactions** — No code existed to remove. DB tables remain for schema integrity.

### Features to Reassess
- [x] **Export/Import** — Keeping. Useful feature, merged from `safe-export-import` branch.

---

## Phase 1: Critical Architecture Fixes

- [x] **runBlocking on main thread** — Converted to `viewModelScope.launch` with suspend calls. `getAllCategoriesForSheetType` now loads into state asynchronously.
- [x] **Domain imports presentation** — Moved `TopCategorySummary` to domain. Moved `formatWithCommas` to domain `Formatting.kt`. Fixed 3 use cases (AnalyticsUseCases, CalculateSubcategoriesUseCase, ConvertAccountsToUiUseCase still returns UiAccount — Phase 2).
- [x] **Direct repository access** — AccountViewModel now routes through `AddAccountUseCase`. Removed unused `repository` dependency. Also removed unused `repository` from ExchangeViewModel.
- [x] **Test data in production** — All calculators now receive exchange rates via `CurrencyManagerUseCase`. Removed `Random.nextDouble()` fake historical data from ExchangeViewModel.

---

## Phase 2: Extract Use Cases from ViewModels

Everything is a use case. ViewModels are orchestrators only.

- [x] **AssetsViewModel** — `updateAssetsAnalytics()` and diversification removed in Phase 0. `convertToUiAssets()` and `onNetWorthLabelClick()` remain — acceptable complexity.
- [x] **AnalyticsViewModel** — `getAllCategoriesForSheetType()` converted to async `loadCategoriesForSheetType()` in Phase 1. Tax calculation now uses injected rates.
- [x] **TransactionViewModel** — Already uses `CalculateDailyTotalUseCase`. Removed dead commented code and unnecessary filterNotNull.
- [x] **GoalsViewModel** — Goal construction defaults are minimal (single `groupName`). Not worth a dedicated use case.
- [x] **SettingsViewModel** — Fixed all 17 `.value =` violations to `.update {}`. Removed unused imports.

---

## Phase 3: Repository Refactoring

- [x] **DefaultCoreRepositoryImpl** — Extracted `resolveTransferCategory()` helper, eliminating duplication between `getAllTransactions()` and `getTransactionById()`. Removed dead comments.
- [ ] **Category usage tracking** — conflates business rules with persistence. Separate concerns. (Deferred — low risk, works correctly.)

---

## Phase 4: Architecture Improvements

- [ ] **Remove GlobalConfig** — `testExchangeRates` used as fallback in CurrencyManagerUseCase (OK). `baseCurrency` still referenced directly. Inject via DI later.
- [x] **State management consistency** — All ViewModels now use `_uiState.update {}`. Zero `.value =` violations remaining.
- [ ] **Create missing use cases** — CategoryAnalysis, HistoricalAnalytics, TaxCalculation, CurrencyConversion, ValidatePinnedCategories, CreateGoal (Deferred — add as needed per feature.)

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
