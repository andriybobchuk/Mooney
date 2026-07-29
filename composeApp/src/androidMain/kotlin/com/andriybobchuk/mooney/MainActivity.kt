package com.andriybobchuk.mooney

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.andriybobchuk.mooney.app.App
import com.andriybobchuk.mooney.core.notifications.NotificationTelemetry
import com.andriybobchuk.mooney.core.platform.FilePickerLauncher
import com.andriybobchuk.mooney.core.premium.ActivityProvider
import com.andriybobchuk.mooney.e2e.E2eBootstrap
import com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val activityProvider: ActivityProvider by inject()
    private val appDataCache: AppDataCache by inject()
    // Bridge between the ActivityResult contract (must be registered before
    // the Activity reaches STARTED) and the suspending FileHandler API.
    // Without calling attach() below, `pickAndReadTextFile` silently returns
    // null → Settings → Import + Import CSV look "broken" with no error
    // (user reported this against 26.07.06 on Android).
    private val filePickerLauncher: FilePickerLauncher by inject()

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
        // MUST run before setContent / any suspend attempt to pick a file:
        // Android's registerForActivityResult contract has to be registered
        // before the Activity hits STARTED, else it throws IllegalStateException.
        filePickerLauncher.attach(this)
        maybeFireNotificationOpened(intent)

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
