# Tech Debt & Refactoring Roadmap

Living document. Updated each tech debt resolution session. Read the actual code before acting on any item — references may shift as we refactor.

---

## Phase 0: Remove Unused Code & Features (Do First)

### Dead Code
- [ ] **Book routes in Route.kt** — `BookGraph`, `BookList`, `BookDetail` tutorial leftovers. Delete.
- [ ] **Commented-out recurring transaction UI in TransactionScreen.kt** — ~60 lines for composables that don't exist. DB entities stay, dead UI goes.
- [ ] **Unused params in TransactionsScreenContent** — `onAcceptPending`, `onRejectPending`, `onEditPending` never wired.
- [ ] **Test function in AccountViewModel** — `testReconciliationDialog()` with `println` statements. Delete.

### Features to Remove Entirely
- [ ] **Asset analytics / diversification** — Remove `CalculateAssetDiversificationUseCase`, the `updateAssetsAnalytics()` method in AssetsViewModel, all pie chart / percentage breakdown UI. Simplify to account list by category.
- [ ] **Dynamic theme switching** — Remove `ThemeSwitcher.kt` and theme selection UI. Pick one theme, hardcode it.
- [ ] **Smart suggestions for recurring transactions** — Remove auto-pattern detection logic. Keep only user-created recurring transactions. DB tables must remain for schema integrity.

### Features to Reassess
- [ ] **Export/Import** — `DataExportImportManager`, `FileHandler`, `DataExportImportSection`. Caused DB issues before. Keep or remove until Firebase is done?

---

## Phase 1: Critical Architecture Fixes

- [ ] **runBlocking on main thread** — AnalyticsViewModel calls `runBlocking { getTransactionsUseCase().first() }` in two places. ANR risk. Convert to `suspend` + `viewModelScope.launch`.
- [ ] **Domain imports presentation** — `AnalyticsUseCases.kt` imports `TopCategorySummary` and `formatWithCommas` from presentation layer. Move model to domain, formatting to presentation.
- [ ] **Direct repository access** — AccountViewModel calls `repository.upsertAccount()` bypassing use case layer. Route through use case.
- [ ] **Test data in production** — AnalyticsViewModel uses `GlobalConfig.testExchangeRates` in calculator constructors. ExchangeViewModel has hardcoded rates and `Random.nextDouble()`. Use injected real rates.

---

## Phase 2: Extract Use Cases from ViewModels

Everything is a use case. ViewModels are orchestrators only.

- [ ] **AssetsViewModel** — `updateAssetsAnalytics()` has 65 lines of portfolio calculations. `convertToUiAssets()` does exchange rate math. `onNetWorthLabelClick()` mixes 3 concerns. Extract to use cases.
- [ ] **AnalyticsViewModel (572 lines)** — `getAllCategoriesForSheetType()` is 69 lines of category grouping/trending. Historical data aggregation is 33 lines. Tax calculation duplicates filtering. Extract all to use cases.
- [ ] **TransactionViewModel** — Daily total aggregation (groupBy + mapValues) inline instead of using existing `CalculateDailyTotalUseCase`. Date range filtering inline.
- [ ] **GoalsViewModel** — Goal construction with defaults belongs in `CreateGoalUseCase`.
- [ ] **SettingsViewModel** — Pinned category validation (max limit checking) belongs in use case. Fix inconsistent `.value =` vs `.update {}`.

---

## Phase 3: Repository Refactoring

- [ ] **DefaultCoreRepositoryImpl (207 lines)** — `getAllTransactions()` has 38 lines of dynamic transfer category construction. `getTransactionById()` duplicates it. Extract to service/use case.
- [ ] **Category usage tracking** — conflates business rules with persistence. Separate concerns.

---

## Phase 4: Architecture Improvements

- [ ] **Remove GlobalConfig** — `testExchangeRates` and `baseCurrency` should be injected via DI, not global singleton.
- [ ] **State management consistency** — All ViewModels must use `_uiState.update {}`, never `_uiState.value = ...`
- [ ] **Create missing use cases** — CategoryAnalysis, HistoricalAnalytics, TaxCalculation, CurrencyConversion, ValidatePinnedCategories, CreateGoal

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
