package com.andriybobchuk.mooney

import android.app.Application
import com.andriybobchuk.mooney.di.initKoin
import org.koin.android.ext.koin.androidContext

class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MyApp)
        }
    }
}
