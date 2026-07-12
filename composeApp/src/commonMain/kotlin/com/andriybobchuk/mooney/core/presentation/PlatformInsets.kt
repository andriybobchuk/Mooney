package com.andriybobchuk.mooney.core.presentation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable

/**
 * Bottom safe-area insets used for the design-system's bottom sheets.
 *
 * - Android: `WindowInsets.navigationBars` — needed because the app runs
 *   edge-to-edge on Android 15 (targetSdk 35), so without this the primary
 *   CTA sits under the gesture pill and users close the app instead of
 *   tapping "Save".
 * - iOS: zero — the ComposeUIViewController already lives inside the OS
 *   safe area, and adding another inset visually hides the home indicator
 *   AND doubles up the bottom padding on every sheet.
 */
@Composable
expect fun platformBottomSafeInsets(): WindowInsets

/**
 * Bottom reserve at the base of the global [BottomNavigationBar], sized so
 * the tab icons never sit on top of the swipe area:
 *
 * - Android: sized to the reported navigationBars inset — auto-adjusts to
 *   whatever the current device reports.
 * - iOS: fixed 20dp — the original behaviour before the Android edge-to-edge
 *   fix landed; iPhone doesn't need dynamic sizing here.
 */
@Composable
expect fun BottomBarBottomSpacer()
