package com.andriybobchuk.mooney.e2e

import android.app.Application
import android.content.Intent

/**
 * No-op stub for the `debug` build type. See the same-name file under
 * `androidE2e` for the real implementation used only by the E2E variant.
 *
 * Compiled only into debug; the release variant has its own copy; the e2e
 * variant has the real work. The three source sets never overlap, so the
 * three files coexist without redeclaration errors.
 */
object E2eBootstrap {
    fun onApplicationCreate(application: Application) = Unit
    fun onActivityCreate(intent: Intent?) = Unit
    val defersWarmStartup: Boolean = false
}
