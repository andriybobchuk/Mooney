package com.andriybobchuk.mooney.di

import com.andriybobchuk.mooney.core.data.database.MooneyDatabaseFactory
import com.andriybobchuk.mooney.core.data.preferences.PreferencesDataStoreFactory
import com.andriybobchuk.mooney.core.platform.FileHandler
import com.andriybobchuk.mooney.core.premium.ActivityProvider
import com.andriybobchuk.mooney.core.premium.AndroidBillingManager
import com.andriybobchuk.mooney.core.premium.BillingManager
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        single<HttpClientEngine> { OkHttp.create() }
        single { MooneyDatabaseFactory(androidApplication()) }
        single { PreferencesDataStoreFactory(androidApplication()) }
        single { FileHandler(androidApplication()) }
        single { ActivityProvider() }
        single<BillingManager> { AndroidBillingManager(androidApplication(), get()) }
    }