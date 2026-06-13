package com.andriybobchuk.mooney

import androidx.compose.ui.window.ComposeUIViewController
import com.andriybobchuk.mooney.app.App
import com.andriybobchuk.mooney.di.initKoin
import com.andriybobchuk.mooney.di.warmStartupSingletons

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
        // Mirror the Android side — get the heaviest singletons (DB, AppDataCache)
        // resolving on a background dispatcher before the first composition
        // touches them. See WarmStartup.kt.
        warmStartupSingletons()
    }
) { App() }
