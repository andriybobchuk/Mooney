package com.andriybobchuk.mooney

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.andriybobchuk.mooney.app.App
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.core.notifications.NotificationTelemetry
import com.andriybobchuk.mooney.core.premium.ActivityProvider
import com.andriybobchuk.mooney.e2e.E2eBootstrap
import com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache
import com.andriybobchuk.mooney.widgets.WidgetIntents
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val activityProvider: ActivityProvider by inject()
    private val appDataCache: AppDataCache by inject()
    private val analyticsTracker: AnalyticsTracker by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // E2E bootstrap: no-op on debug/release, real on the e2e variant.
        // Runs BEFORE splashScreen so DB wipe + fixture seeding happen while
        // the system splash is still displayed.
        E2eBootstrap.onActivityCreate(intent)
        // installSplashScreen() MUST be called before super.onCreate so the
        // system keeps the splash visible across the launch → first-frame
        // transition. Without setKeepOnScreenCondition, the splash dismisses
        // the moment the activity is interactive — typically before our
        // Compose tree has a chance to paint anything meaningful, producing
        // the brief white flash users see today.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        activityProvider.setActivity(this)
        maybeFireNotificationOpened(intent)
        maybeHandleWidgetIntent(intent)

        // Keep the system splash visible until the AppDataCache has emitted
        // at least once. By that point Room is open, the dep graph is fully
        // resolved, and the first Compose frame can paint real data. Caps
        // out implicitly at the system's own splash timeout, so a degenerate
        // cold start can't trap the user on the splash forever.
        splashScreen.setKeepOnScreenCondition {
            !appDataCache.snapshot.value.isReady
        }

        setContent {
            App()
        }
    }

    // When SINGLE_TOP + CLEAR_TOP relaunches the existing activity from a
    // notification tap, onCreate does NOT re-run — the new intent arrives
    // through onNewIntent. Fire the analytics event from both paths so we
    // capture both cold-start and warm-return attribution.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        maybeFireNotificationOpened(intent)
        maybeHandleWidgetIntent(intent)
    }

    /**
     * Widget taps arrive with `action = WIDGET_OPEN` and two extras: `kind`
     * (which widget was tapped) and `route` (where inside the app to go).
     * We fire `widget_tapped` for every arrival — the actual navigation is
     * driven by NavigationHost reading `intent.getStringExtra(EXTRA_ROUTE)`
     * on the next composition pass.
     *
     * Clears the extras after firing so a config change / process death
     * restore doesn't refire the event on the same intent instance.
     */
    private fun maybeHandleWidgetIntent(intent: Intent?) {
        if (intent?.action != WidgetIntents.ACTION_OPEN) return
        val kind = intent.getStringExtra(WidgetIntents.EXTRA_KIND) ?: return
        val route = intent.getStringExtra(WidgetIntents.EXTRA_ROUTE) ?: WidgetIntents.ROUTE_HOME
        try {
            analyticsTracker.trackEvent(
                AnalyticsEvent.WidgetTapped(kind = kind, action = route)
            )
        } catch (_: Throwable) {
            // Best-effort; a widget tap must never crash the app.
        }
        intent.removeExtra(WidgetIntents.EXTRA_KIND)
        // Keep EXTRA_ROUTE — NavigationHost reads it on setup so the user
        // lands on the right screen. Clearing it here would defeat the
        // deep-link. NavigationHost handles the "did we already consume it"
        // guard via its own remembered flag.
    }

    private fun maybeFireNotificationOpened(intent: Intent?) {
        val fromNotification = intent
            ?.getBooleanExtra(NotificationTelemetry.EXTRA_FROM_NOTIFICATION, false) == true
        if (fromNotification) {
            NotificationTelemetry().markOpened()
            // Clear the extra so a subsequent config change (rotation,
            // process-death restore) doesn't refire the event on the same
            // intent instance.
            intent?.removeExtra(NotificationTelemetry.EXTRA_FROM_NOTIFICATION)
        }
    }
}
