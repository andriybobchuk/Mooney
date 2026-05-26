package com.andriybobchuk.mooney.mooney.domain.cache

import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.Transaction
import kotlinx.coroutines.flow.StateFlow

/**
 * App-scoped, always-on snapshot of the underlying data every screen reads
 * from. Lives for the entire app process and is independent of any
 * ViewModel's lifecycle.
 *
 * Why this exists:
 *  - ViewModels were paying a small "first-emission" cost on every screen
 *    creation. With the cache pre-loaded at app start, ViewModels can read
 *    the latest data synchronously and render it on the very first frame —
 *    no shimmer flash between tabs.
 *  - Clean architecture boundary: the presentation layer asks the domain
 *    layer for a *snapshot*, not a Flow of repository state. The data layer
 *    is the only place that knows how to keep the snapshot fresh.
 *
 * Implementations MUST start collecting their inputs eagerly (e.g.
 * SharingStarted.Eagerly inside an app-scoped CoroutineScope) so the
 * snapshot is warm by the time the first screen reads it.
 */
interface AppDataCache {
    val snapshot: StateFlow<AppDataSnapshot>
}

/**
 * The complete denormalized state every screen needs to render. Lists are
 * already null-filtered, so consumers don't have to repeat that defensive
 * step. [isReady] flips to true after the first emission from each input
 * Flow has landed — screens use it to distinguish "true cold start, show
 * shimmer" from "data exists, render immediately".
 */
data class AppDataSnapshot(
    val transactions: List<Transaction>,
    val accounts: List<Account>,
    val isReady: Boolean
) {
    companion object {
        val Empty = AppDataSnapshot(
            transactions = emptyList(),
            accounts = emptyList(),
            isReady = false
        )
    }
}
