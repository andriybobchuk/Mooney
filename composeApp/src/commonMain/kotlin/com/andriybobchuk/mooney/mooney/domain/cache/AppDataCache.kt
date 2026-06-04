package com.andriybobchuk.mooney.mooney.domain.cache

import com.andriybobchuk.mooney.core.data.database.AssetCategoryEntity
import com.andriybobchuk.mooney.core.data.database.PendingTransactionEntity
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.Goal
import com.andriybobchuk.mooney.mooney.domain.RecurringTransaction
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.UserCurrency
import kotlinx.coroutines.flow.StateFlow

/**
 * App-scoped, always-on snapshot of every reactive data domain the UI reads.
 * Lives for the entire app process, independent of any ViewModel lifecycle.
 *
 * Why this exists:
 *  - ViewModels would otherwise pay a "first-emission" cost every time a screen
 *    is entered. With the cache pre-loaded at app start, ViewModels read the
 *    latest snapshot synchronously and render data on the very first frame —
 *    no shimmer flash between tabs, no flash of an empty state before data
 *    arrives.
 *  - Clean architecture: presentation asks the domain layer for a *snapshot*,
 *    not a Flow of repository state. Only the data layer knows how to keep it
 *    fresh.
 *
 * Implementations MUST start collecting their inputs eagerly (e.g.
 * SharingStarted.Eagerly inside an app-scoped CoroutineScope) so the snapshot
 * is warm by the time the first screen reads it.
 */
interface AppDataCache {
    val snapshot: StateFlow<AppDataSnapshot>
}

/**
 * The complete denormalized state every screen needs to render. Lists are
 * already null-filtered, so consumers don't have to repeat that defensive
 * step.
 *
 * [isReady] flips to `true` the moment every input Flow has emitted at least
 * once. UI uses this single boolean to decide:
 *   - `false` → show shimmer (true cold start, no data loaded yet)
 *   - `true` AND list is empty → show empty state (load complete, no data exists)
 *   - `true` AND list has items → render normally
 *
 * Critically: the **empty state must NEVER render while [isReady] is `false`**,
 * because then the first frame after install or process-death would flash the
 * onboarding/"add your first X" placeholder before the database emit arrives.
 */
data class AppDataSnapshot(
    val transactions: List<Transaction>,
    val accounts: List<Account>,
    val categories: List<Category>,
    val goals: List<Goal>,
    val recurringTransactions: List<RecurringTransaction>,
    val pendingTransactions: List<PendingTransactionEntity>,
    val assetCategories: List<AssetCategoryEntity>,
    val userCurrencies: List<UserCurrency>,
    val isReady: Boolean
) {
    companion object {
        val Empty = AppDataSnapshot(
            transactions = emptyList(),
            accounts = emptyList(),
            categories = emptyList(),
            goals = emptyList(),
            recurringTransactions = emptyList(),
            pendingTransactions = emptyList(),
            assetCategories = emptyList(),
            userCurrencies = emptyList(),
            isReady = false
        )
    }
}
