package com.andriybobchuk.mooney.mooney.data.cache

import com.andriybobchuk.mooney.core.data.database.AssetCategoryDao
import com.andriybobchuk.mooney.core.data.database.CategoryDao
import com.andriybobchuk.mooney.core.data.database.PendingTransactionDao
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache
import com.andriybobchuk.mooney.mooney.domain.cache.AppDataSnapshot
import com.andriybobchuk.mooney.mooney.domain.usecase.GetAccountsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetCategoriesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetGoalsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetRecurringTransactionsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetTransactionsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetUserCurrenciesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * In-memory implementation of [AppDataCache].
 *
 * Combines every reactive data source into one always-warm
 * `StateFlow<AppDataSnapshot>`. `combine` only emits after every input has
 * emitted at least once, so the very first non-Empty snapshot is the one we
 * mark `isReady = true` — no need to track readiness per-field.
 *
 * Categories are special: the existing repository pre-builds them into an
 * in-memory map (`CoreRepository.reloadCategories()` + `getCategoriesUseCase()`).
 * We treat the `categoryDao.getAll()` Flow as a "something changed" tick,
 * reload the repository cache, and emit the resulting in-memory list. That's
 * exactly the pattern `TransactionViewModel.loadDataForBottomSheet()` already
 * uses; centralizing it here means no screen has to remember to do it.
 */
class DefaultAppDataCache(
    getTransactionsUseCase: GetTransactionsUseCase,
    getAccountsUseCase: GetAccountsUseCase,
    getGoalsUseCase: GetGoalsUseCase,
    getRecurringTransactionsUseCase: GetRecurringTransactionsUseCase,
    getUserCurrenciesUseCase: GetUserCurrenciesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val coreRepository: CoreRepository,
    categoryDao: CategoryDao,
    assetCategoryDao: AssetCategoryDao,
    pendingTransactionDao: PendingTransactionDao,
    appScope: CoroutineScope
) : AppDataCache {

    // combine() supports up to 5 flows directly; we group into two tuples
    // and a fan-in below for the remaining sources.
    private val coreTuple = combine(
        getTransactionsUseCase().map { it.filterNotNull() },
        getAccountsUseCase().map { it.filterNotNull() },
        categoryDao.getAll().map {
            // Refresh the repository's in-memory map before reading; the use
            // case is sync and reads from that map, so without the reload it
            // wouldn't reflect new/renamed/deleted categories.
            coreRepository.reloadCategories()
            getCategoriesUseCase()
        }
    ) { transactions, accounts, categories ->
        CoreTuple(transactions, accounts, categories)
    }

    private val extrasTuple = combine(
        getGoalsUseCase(),
        getRecurringTransactionsUseCase(),
        pendingTransactionDao.getAllPending()
    ) { goals, recurring, pending ->
        ExtrasTuple(goals, recurring, pending)
    }

    override val snapshot: StateFlow<AppDataSnapshot> = combine(
        coreTuple,
        extrasTuple,
        assetCategoryDao.getAll(),
        getUserCurrenciesUseCase()
    ) { core, extras, assetCategories, userCurrencies ->
        AppDataSnapshot(
            transactions = core.transactions,
            accounts = core.accounts,
            categories = core.categories,
            goals = extras.goals,
            recurringTransactions = extras.recurring,
            pendingTransactions = extras.pending,
            assetCategories = assetCategories,
            userCurrencies = userCurrencies,
            // combine() only fires after every input emits; reaching this
            // closure means everything is ready.
            isReady = true
        )
    }.stateIn(
        scope = appScope,
        // Eager + app scope: cache lives for the whole process so tab switches
        // and screen re-entries read pre-warmed data on the first frame.
        started = SharingStarted.Eagerly,
        initialValue = AppDataSnapshot.Empty
    )

    private data class CoreTuple(
        val transactions: List<com.andriybobchuk.mooney.mooney.domain.Transaction>,
        val accounts: List<com.andriybobchuk.mooney.mooney.domain.Account>,
        val categories: List<com.andriybobchuk.mooney.mooney.domain.Category>
    )

    private data class ExtrasTuple(
        val goals: List<com.andriybobchuk.mooney.mooney.domain.Goal>,
        val recurring: List<com.andriybobchuk.mooney.mooney.domain.RecurringTransaction>,
        val pending: List<com.andriybobchuk.mooney.core.data.database.PendingTransactionEntity>
    )
}
