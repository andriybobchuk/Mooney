package com.andriybobchuk.mooney

import androidx.compose.ui.window.ComposeUIViewController
import com.andriybobchuk.mooney.app.App
import com.andriybobchuk.mooney.di.initKoin
import com.andriybobchuk.mooney.di.warmStartupSingletons
import com.andriybobchuk.mooney.e2e.E2eBootstrap

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
        // No-op on production launches; when Maestro passes `--e2e` the
        // bootstrap wipes the e2e DB, seeds a fixture, and installs Koin
        // overrides. Runs BEFORE warm-startup so the DB resolve happens
        // against the seeded state.
        E2eBootstrap.onApplicationLaunch()
        // Mirror the Android side — get the heaviest singletons (DB, AppDataCache)
        // resolving on a background dispatcher before the first composition
        // touches them. See WarmStartup.kt.
        warmStartupSingletons()
    }
) { App() }
