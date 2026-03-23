package com.andriybobchuk.mooney

import androidx.compose.ui.window.ComposeUIViewController
import com.andriybobchuk.mooney.app.App
import com.andriybobchuk.mooney.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) { App() }
