# Tech Debt

Tracked items to clean up when time permits.

## Dead Code
- **Book routes in Route.kt** — `BookGraph`, `BookList`, `BookDetail` are leftovers from a tutorial/template. All references in NavigationHost are commented out. Safe to remove.
- **Commented-out recurring transaction UI in TransactionScreen.kt** — ~60 lines of dead code for `RecurringTransactionDialog` and `PendingTransactionItem` (neither composable exists). The DB entities must stay, but this dead UI code can go.
- **Unused params in TransactionsScreenContent** — `onAcceptPending`, `onRejectPending`, `onEditPending` are defined but never wired.

## Features to Revisit
- **Export/Import feature** — `DataExportImportManager`, `FileHandler`, `DataExportImportSection` in Settings. This was the feature that originally caused database issues. Needs testing.
- **Recurring transactions** — Entities and DAOs exist (required for DB schema v8) but no working UI. Implement or decide to drop via a proper migration.
