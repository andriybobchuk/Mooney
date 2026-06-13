package com.andriybobchuk.mooney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.andriybobchuk.mooney.app.App
import com.andriybobchuk.mooney.core.premium.ActivityProvider
import com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val activityProvider: ActivityProvider by inject()
    private val appDataCache: AppDataCache by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() MUST be called before super.onCreate so the
        // system keeps the splash visible across the launch → first-frame
        // transition. Without setKeepOnScreenCondition, the splash dismisses
        // the moment the activity is interactive — typically before our
        // Compose tree has a chance to paint anything meaningful, producing
        // the brief white flash users see today.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        activityProvider.setActivity(this)

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
}
