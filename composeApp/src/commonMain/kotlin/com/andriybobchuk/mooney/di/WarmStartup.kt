package com.andriybobchuk.mooney.di

import com.andriybobchuk.mooney.core.data.database.AppDatabase
import com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/**
 * Eagerly resolve the heaviest singletons off the main thread so they're warm
 * by the time the first Composable injects them.
 *
 * Without this, the first `koinInject<TransactionDao>()` (etc.) inside `App()`
 * forces Room to open the database on the main thread during composition —
 * 100–500ms on fresh installs because of `SEED_DATABASE_CALLBACK`. Resolving
 * here lets the system splash mask that work.
 *
 * Call once, right after `initKoin()`.
 *
 * Note: runs on the app-scoped `Dispatchers.Default`-backed CoroutineScope.
 * `Dispatchers.IO` would be the textbook choice for disk I/O but it's
 * unavailable in commonMain (Native doesn't expose it); the underlying
 * SQLite + DataStore I/O is bridged through their own native dispatchers
 * anyway, so this scheduler choice only affects the orchestrating coroutine.
 */
fun warmStartupSingletons() {
    val koin = KoinPlatform.getKoin()
    val appScope = koin.get<CoroutineScope>()
    appScope.launch {
        try {
            koin.get<AppDatabase>()
            koin.get<AppDataCache>()
        } catch (_: Throwable) {
            // Best-effort warmup — never block the app for this.
        }
    }
}
