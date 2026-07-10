package com.andriybobchuk.mooney.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific banner ad view.
 *
 * - iOS: hosts a real `BannerView` via Compose Multiplatform's `UIKitView`.
 *   The `isDarkTheme` flag is applied as `overrideUserInterfaceStyle` on the
 *   BannerView so ad creatives that respect the trait environment render in
 *   the same mode as the app. Container background is transparent so the
 *   Compose parent (which paints the app's surface color) shows through
 *   before the ad load completes — no more white flash.
 * - Android: hosts a `com.google.android.gms.ads.AdView`. Same rules apply.
 *
 * Place this inside an eligibility-gated wrapper like [AdBannerSlot] — don't
 * call directly from screens, or banners can show for premium / new users.
 */
@Composable
expect fun MooneyBannerAdView(
    adUnitId: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
)
