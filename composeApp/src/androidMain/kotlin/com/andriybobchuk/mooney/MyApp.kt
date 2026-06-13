package com.andriybobchuk.mooney

import android.app.Application
import com.andriybobchuk.mooney.di.initKoin
import com.andriybobchuk.mooney.di.warmStartupSingletons
import org.koin.android.ext.koin.androidContext

class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MyApp)
        }
        // Kick the database + data cache off main so first composition doesn't
        // pay for Room.open() on the UI thread. See WarmStartup.kt.
        warmStartupSingletons()
    }
}
