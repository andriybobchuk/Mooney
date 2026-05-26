package com.andriybobchuk.mooney.mooney.data.cache

import com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache
import com.andriybobchuk.mooney.mooney.domain.cache.AppDataSnapshot
import com.andriybobchuk.mooney.mooney.domain.usecase.GetAccountsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetTransactionsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Default in-memory implementation of [AppDataCache].
 *
 * Combines the two heavy reactive inputs (all transactions, all accounts)
 * into a single [AppDataSnapshot] [StateFlow]. The Flow is started eagerly
 * in [appScope] so its first emission is already cached by the time any
 * screen requests it. There is no caching to disk — Room itself is the
 * persistent store; this class just keeps the latest *converted* shape in
 * memory so ViewModels don't have to recompute it.
 */
class DefaultAppDataCache(
    getTransactionsUseCase: GetTransactionsUseCase,
    getAccountsUseCase: GetAccountsUseCase,
    appScope: CoroutineScope
) : AppDataCache {

    override val snapshot: StateFlow<AppDataSnapshot> = combine(
        getTransactionsUseCase().map { it.filterNotNull() },
        getAccountsUseCase().map { it.filterNotNull() }
    ) { transactions, accounts ->
        AppDataSnapshot(
            transactions = transactions,
            accounts = accounts,
            isReady = true
        )
    }.stateIn(
        scope = appScope,
        // Eagerly + appScope: the cache stays alive for the whole process,
        // never tears down between screens. This is what makes tab switches
        // free — the most recent snapshot is always sitting in `value`.
        started = SharingStarted.Eagerly,
        initialValue = AppDataSnapshot.Empty
    )
}
