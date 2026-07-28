package com.andriybobchuk.mooney.core.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Watches the DataStore-persisted activation flag and surfaces the
 * "Add Mooney to your home screen" bottom sheet exactly once after the user
 * hits the activation milestone (≥3 tx across ≥2 distinct days — the same
 * threshold `TransactionViewModel.maybeCheckActivation` fires the
 * `activated` event on).
 *
 * Show conditions (ALL must hold):
 *  1. `ANALYTICS_ACTIVATED_FIRED` is true (user is a real user, not a tourist).
 *  2. `WIDGET_ONBOARDING_SHOWN` is false (we've never surfaced this before).
 *
 * On dismiss (any exit path) we flip `WIDGET_ONBOARDING_SHOWN` to true so
 * the sheet never bothers the user again. Manual re-entry lives under
 * Settings → Widgets (see [WidgetsPickerScreen]).
 *
 * Hosted from `App.kt` so it can float above every screen.
 */
@Composable
fun WidgetOnboardingHost(
    platform: WidgetOnboardingPlatform,
    onSeeAllWidgets: () -> Unit
) {
    val dataStore = koinInject<DataStore<Preferences>>()
    val analyticsTracker = koinInject<AnalyticsTracker>()
    var showSheet by remember { mutableStateOf(false) }

    // One-shot poll on composition — we only need to check when the tree is
    // first laid out. Later activation events refresh via re-composition
    // because TransactionViewModel already emits a state change that trickles
    // up through the ViewModels App reads.
    LaunchedEffect(Unit) {
        try {
            val prefs = dataStore.data.first()
            val activated = prefs[PreferencesKeys.ANALYTICS_ACTIVATED_FIRED] ?: false
            val alreadyShown = prefs[PreferencesKeys.WIDGET_ONBOARDING_SHOWN] ?: false
            if (activated && !alreadyShown) showSheet = true
        } catch (_: Throwable) {
            // DataStore read failing is not worth crashing; skip the sheet.
        }
    }

    if (showSheet) {
        WidgetOnboardingSheet(
            platform = platform,
            analyticsTracker = analyticsTracker,
            onSeeAllWidgets = onSeeAllWidgets,
            onDismiss = {
                showSheet = false
                // Persist the one-shot flag. We accept the tiny race window
                // where a user could see the sheet twice on a device that
                // crashes between dismiss and the DataStore edit — that's
                // annoying but never destructive, and the flag lands on the
                // second dismiss anyway.
                try {
                    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                        dataStore.edit { it[PreferencesKeys.WIDGET_ONBOARDING_SHOWN] = true }
                    }
                } catch (_: Throwable) { /* best-effort */ }
            }
        )
    }
}
