package com.andriybobchuk.mooney.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific banner ad view.
 *
 * - iOS: hosts a real `GADBannerView` via Compose Multiplatform's `UIKitView`.
 *   The view is built once per `adUnitId` and embedded directly; the
 *   GoogleMobileAds SDK handles its own measurement + reload cycle.
 * - Android: no-op until `play-services-ads` is wired post-launch.
 *
 * Place this inside an eligibility-gated wrapper like [AdBannerSlot] — don't
 * call directly from screens, or banners can show for premium / new users.
 */
@Composable
expect fun MooneyBannerAdView(adUnitId: String, modifier: Modifier = Modifier)
