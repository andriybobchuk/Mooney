package com.andriybobchuk.mooney

import android.app.Application
import com.andriybobchuk.mooney.core.ads.Ads
import com.andriybobchuk.mooney.core.notifications.ensureReminderNotificationChannel
import com.andriybobchuk.mooney.di.initKoin
import com.andriybobchuk.mooney.di.warmStartupSingletons
import com.andriybobchuk.mooney.e2e.E2eBootstrap
import org.koin.android.ext.koin.androidContext

class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MyApp)
        }
        E2eBootstrap.onApplicationCreate(this)
        // Warm the database + data cache off main so first composition doesn't
        // pay for Room.open() on the UI thread. On the e2e build we defer
        // warmup to MainActivity so it can wipe/seed the DB first.
        if (!E2eBootstrap.defersWarmStartup) {
            warmStartupSingletons()
        }
        // Hand the Application reference to the Ads actual before initialise()
        // — the common expect-fun takes no parameters so the SDK context has
        // to be stashed up-front. Mirrors the iOS bridge wiring.
        Ads.setApplication(applicationContext)
        Ads.initialize()
        // Notification channels must exist on Android O+ before the first
        // notify() call. Creating it here means WorkManager's worker doesn't
        // need to gamble on whether the channel was registered yet on cold
        // start, and the channel shows up in system settings even before the
        // user enables reminders.
        ensureReminderNotificationChannel(this)
    }
}
