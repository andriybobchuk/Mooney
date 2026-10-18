package com.andriybobchuk.mooney.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber

/**
 * iOS orientation lock. The actual rotation happens in Swift —
 * [iOSApp.swift] exposes `AppDelegate.orientationLock` and overrides
 * `application(_:supportedInterfaceOrientationsFor:)` to return it.
 * This composable just posts an NSNotification that Swift observes to
 * flip the lock + ask the window scene to re-check the allowed
 * orientations.
 *
 * The notification name string matches the Swift observer registered
 * in [AppDelegate.application(_:didFinishLaunchingWithOptions:)] —
 * keep both in sync when renaming.
 */
@Composable
actual fun LockLandscape(enabled: Boolean) {
    DisposableEffect(enabled) {
        if (enabled) postLockNotification(true)
        onDispose {
            if (enabled) postLockNotification(false)
        }
    }
}

private fun postLockNotification(landscape: Boolean) {
    NSNotificationCenter.defaultCenter.postNotificationName(
        aName = "MooneyLockOrientation",
        `object` = NSNumber(bool = landscape)
    )
}
