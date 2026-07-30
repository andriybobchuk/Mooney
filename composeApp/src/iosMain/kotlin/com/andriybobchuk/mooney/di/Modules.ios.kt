package com.andriybobchuk.mooney.di

import com.andriybobchuk.mooney.core.data.database.MooneyDatabaseFactory
import com.andriybobchuk.mooney.core.data.preferences.PreferencesDataStoreFactory
import com.andriybobchuk.mooney.core.data.preferences.StartupPrefs
import com.andriybobchuk.mooney.core.platform.AppRestarter
import com.andriybobchuk.mooney.core.platform.FileHandler
import com.andriybobchuk.mooney.core.premium.BillingManager
import com.andriybobchuk.mooney.core.premium.IosBillingManager
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        single<HttpClientEngine> { Darwin.create() }
        // StartupPrefs must resolve first so the DB factory can read the
        // demo-mode toggle synchronously at construction time.
        single { StartupPrefs() }
        single { MooneyDatabaseFactory(get()) }
        single { PreferencesDataStoreFactory() }
        single { FileHandler() }
        single<BillingManager> { IosBillingManager() }
        single { AppRestarter() }
    }
