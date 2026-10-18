package com.andriybobchuk.mooney.core.platform

import androidx.compose.runtime.Composable

/**
 * Composable orientation lock. Called from a screen's top-level
 * composable with `enabled = true` to force landscape while that
 * screen is on-screen, and `enabled = false` (or just letting the
 * composable leave composition) to restore the host app's default.
 *
 * - **Android**: rotates the hosting Activity via
 *   `activity.requestedOrientation`. Original value is restored when
 *   the composable leaves composition.
 * - **iOS**: no-op in this build because the `UISupportedInterface
 *   Orientations` plist entry only lists portrait. Expanding iOS
 *   support means adding landscape to the plist AND implementing a
 *   per-screen rotation flag in `iosApp.swift` (`AppDelegate.
 *   supportedInterfaceOrientationsFor(window:)`). Users on iOS can
 *   still see the timeline fine in portrait — the chart's horizontal
 *   scroll + pinch zoom gives them the equivalent breadth.
 */
@Composable
expect fun LockLandscape(enabled: Boolean)
