package com.andriybobchuk.mooney.core.widgets

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache
import com.andriybobchuk.mooney.mooney.domain.usecase.CurrencyManagerUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

/**
 * Watches the app data cache, rebuilds the widget snapshot on every meaningful
 * change, and persists it to the platform-specific location that widget hosts
 * read from.
 *
 * Runs once, app-scoped. Owned by `MyApp` on Android and by the iOS boot path
 * (via Koin) — subsequent `start()` calls are no-ops.
 *
 * ## Debounce
 *
 * The cache fires on every DB change; typing "Coffee" into a transaction can
 * cause several intermediate snapshots. We debounce by [WRITE_DEBOUNCE_MS] so
 * the widget host only sees the final settled state. Writes are cheap but
 * broadcasting a Glance update is not — Android throttles UI-thread work if
 * we spam it.
 *
 * ## Generation counter
 *
 * The `updateGeneration` field on `WidgetDataSnapshot` needs to survive
 * process death (a widget that already rendered generation N should skip
 * re-rendering if it sees the same N after our process restarts). We persist
 * it in DataStore so cold start reads the last value and increments from
 * there.
 */
class WidgetSnapshotCoordinator(
    private val cache: AppDataCache,
    private val currencyManager: CurrencyManagerUseCase,
    private val builder: BuildWidgetSnapshotUseCase,
    private val writer: WidgetSnapshotWriter,
    private val dataStore: DataStore<Preferences>,
    private val analyticsTracker: AnalyticsTracker
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val forceRefreshBus = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    // Started guard is only ever flipped from `start()` on the app boot
    // thread, so a plain Boolean is enough here — no volatile/atomic needed.
    private var started = false

    fun start() {
        if (started) return
        started = true

        val cacheTicks = cache.snapshot
            .filter { it.isReady }
        // Base-currency changes should also rebuild (net worth flips).
        val baseCurrencyTicks = GlobalConfig.baseCurrencyFlow

        // `transformLatest` cancels the previous debounce whenever a new
        // upstream tick lands, so the debounce window resets — settling on
        // the *last* keystroke rather than emitting a stale intermediate.
        @Suppress("MagicNumber")
        merge(cacheTicks, baseCurrencyTicks, forceRefreshBus)
            .debounce(WRITE_DEBOUNCE_MS)
            .transformLatest { emit(Unit) }
            .onEach { rebuildAndWrite() }
            .launchIn(scope)
    }

    /**
     * Manually kick a rebuild. Used by the "Force refresh" dev button and by
     * the post-tx callback so widgets update within milliseconds of the user
     * hitting Save (rather than waiting for the cache observer + debounce).
     */
    fun requestRefresh() {
        forceRefreshBus.tryEmit(Unit)
    }

    private suspend fun rebuildAndWrite() {
        try {
            val app = cache.snapshot.value
            if (!app.isReady) return
            val rates = currencyManager.getCurrentExchangeRates()
            val nextGen = nextGeneration()
            val snapshot = builder(
                app = app,
                rates = rates,
                baseCurrency = GlobalConfig.baseCurrency,
                generation = nextGen
            )
            writer.write(snapshot)
            analyticsTracker.trackEvent(
                AnalyticsEvent.WidgetDataRefreshed(
                    generation = nextGen,
                    mood = snapshot.mooleyMood.name.lowercase()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            // Widget writes are best-effort — a failure should never crash the
            // main app. Log for Crashlytics + move on.
            analyticsTracker.recordException(t, "WidgetSnapshotCoordinator")
        }
    }

    private suspend fun nextGeneration(): Long {
        var next = 0L
        dataStore.edit { prefs ->
            val current = prefs[GENERATION_KEY] ?: 0L
            next = current + 1
            prefs[GENERATION_KEY] = next
        }
        return next
    }

    /** Public API for reading the current generation without incrementing. */
    suspend fun currentGeneration(): Long =
        dataStore.data.first()[GENERATION_KEY] ?: 0L

    private companion object {
        val GENERATION_KEY = longPreferencesKey("widget_update_generation")

        // 350ms sweeps up a burst of typing / bulk imports without making the
        // user notice the delay. Same order of magnitude as Compose's default
        // recomposition debounce.
        const val WRITE_DEBOUNCE_MS: Long = 350L
    }
}

/**
 * `viewModelScope`-friendly extension used from the post-transaction path so
 * widget writes happen immediately after Save rather than at the debounce end.
 * Called from `TransactionViewModel.upsertTransaction`.
 */
fun CoroutineScope.kickWidgetRefresh(coordinator: WidgetSnapshotCoordinator) {
    launch { coordinator.requestRefresh() }
}
