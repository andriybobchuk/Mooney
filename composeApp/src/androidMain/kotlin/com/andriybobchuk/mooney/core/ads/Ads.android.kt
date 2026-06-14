package com.andriybobchuk.mooney.core.ads

/**
 * Android implementation. Plug Google Mobile Ads here when wiring up Android
 * support (after the iOS-first launch). Today this is a no-op so the rest of
 * the app can call into [Ads] without crashing on Android while only iOS has
 * a live bridge.
 *
 * To wire up later:
 *   1. Add `com.google.android.gms:play-services-ads` to androidMain deps
 *   2. Replace the bodies below with `MobileAds.initialize(...)`,
 *      `InterstitialAd.load(...)`, `RewardedAd.load(...)`, etc.
 *   3. Cache loaded ads in a private property; surface them via the methods.
 *   4. Pull the current Activity from `ActivityProvider` (already in Koin) to
 *      present full-screen ads.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object Ads {
    actual fun initialize() = Unit
    actual fun preloadInterstitial(adUnitId: String) = Unit
    actual fun showInterstitialIfReady(): Boolean = false
    actual fun preloadRewarded(adUnitId: String) = Unit
    actual fun showRewarded(onReward: () -> Unit, onDismissed: () -> Unit) {
        // No-op until the Android SDK is wired up — see file header.
        onDismissed()
    }
}
