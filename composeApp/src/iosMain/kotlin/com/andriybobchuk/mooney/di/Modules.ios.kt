package com.andriybobchuk.mooney.di

import com.andriybobchuk.mooney.core.data.database.MooneyDatabaseFactory
import com.andriybobchuk.mooney.core.data.preferences.PreferencesDataStoreFactory
import com.andriybobchuk.mooney.core.data.preferences.StartupPrefs
import com.andriybobchuk.mooney.core.platform.FileHandler
import com.andriybobchuk.mooney.core.premium.BillingManager
import com.andriybobchuk.mooney.core.premium.IosBillingManager
import com.andriybobchuk.mooney.core.widgets.IosWidgetSnapshotWriter
import com.andriybobchuk.mooney.core.widgets.WidgetSnapshotWriter
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        single<HttpClientEngine> { Darwin.create() }
        single { MooneyDatabaseFactory() }
        single { PreferencesDataStoreFactory() }
        single { StartupPrefs() }
        single { FileHandler() }
        single<BillingManager> { IosBillingManager() }
        // Widgets — iOS impl writes to App-Group UserDefaults + pings
        // WidgetKit via the IosWidgetKitBridge registered from iOSApp.swift.
        single { IosWidgetSnapshotWriter() }.bind<WidgetSnapshotWriter>()
    }
