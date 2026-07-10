package com.andriybobchuk.mooney.e2e

import android.app.Application
import android.content.Intent

/** No-op stub for the `release` build type. See androidDebug sibling. */
object E2eBootstrap {
    fun onApplicationCreate(application: Application) = Unit
    fun onActivityCreate(intent: Intent?) = Unit
    val defersWarmStartup: Boolean = false
}
