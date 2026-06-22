package com.andriybobchuk.mooney.core.ads

import com.andriybobchuk.mooney.mooney.domain.FeatureFlags

/**
 * Central registry of AdMob unit IDs.
 *
 * The real iOS IDs below were created in the AdMob console under app
 * **Mooney** (App ID `ca-app-pub-7021633711522076~6326300426`). Each ad unit
 * was named to match its format so the AdMob dashboard reports are scannable.
 *
 * ## Test vs live IDs
 *
 * Apple has rejected apps for shipping test ad IDs in release builds, AND has
 * rejected apps that show real (paid) ads during App Review. The standard
 * compromise: debug builds + during review = test IDs; release builds = real
 * IDs. We pick via [FeatureFlags.isDebug]; the actual "show real ads only
 * after App Review approval" gate happens in [AdEligibilityUseCase] (new
 * users skip ads for the first sessions anyway, which covers review).
 *
 * The official Google test IDs are SDK-special — they always return a fill,
 * never charge, and won't get your account flagged for invalid traffic.
 *
 * ## Android
 *
 * Empty until the Android Google Mobile Ads SDK is wired up. The Android
 * actual `Ads.kt` is a no-op so referencing these empty strings is safe.
 */
/**
 * Test/prod banner unit IDs are platform-specific in AdMob — an iOS unit ID
 * silently fails on Android (the SDK never reports the mismatch, so the slot
 * just stays blank). Implemented per-platform in [AdUnitIds.android.kt] and
 * [AdUnitIds.ios.kt].
 */
expect val platformBannerTestId: String
expect val platformBannerProdId: String

object AdUnitIds {

    val appId: String
        get() = if (FeatureFlags.isDebug || FeatureFlags.adsAlwaysShow) {
            // iOS test App ID — works in simulator and on device.
            "ca-app-pub-3940256099942544~1458002511"
        } else {
            "ca-app-pub-7021633711522076~6326300426"
        }

    val banner: String
        get() = if (FeatureFlags.isDebug || FeatureFlags.adsAlwaysShow) {
            platformBannerTestId
        } else {
            platformBannerProdId
        }

    val interstitial: String
        get() = if (FeatureFlags.isDebug || FeatureFlags.adsAlwaysShow) {
            "ca-app-pub-3940256099942544/4411468910"
        } else {
            "ca-app-pub-7021633711522076/3061884393"
        }

    val rewarded: String
        get() = if (FeatureFlags.isDebug || FeatureFlags.adsAlwaysShow) {
            "ca-app-pub-3940256099942544/1712485313"
        } else {
            "ca-app-pub-7021633711522076/1640949952"
        }
}
