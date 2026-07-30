package com.andriybobchuk.mooney.core.ads

import com.andriybobchuk.mooney.mooney.domain.FeatureFlags

/**
 * Central registry of AdMob unit IDs.
 *
 * The real iOS IDs are under app `ca-app-pub-7021633711522076~6326300426`;
 * Android IDs are under app `ca-app-pub-7021633711522076~6854132621`.
 * Each ad unit was named to match its format so the AdMob dashboard reports
 * are scannable.
 *
 * ## Test vs live IDs
 *
 * Apple has rejected apps for shipping test ad IDs in release builds, AND
 * has rejected apps that show real (paid) ads during App Review. The standard
 * compromise: debug builds + during review = test IDs; release builds = real
 * IDs. We pick via [FeatureFlags.isDebug]; the actual "show real ads only
 * after App Review approval" gate happens in AdEligibilityUseCase (new users
 * skip ads for the first sessions anyway, which covers review).
 *
 * The official Google test IDs are SDK-special — they always return a fill,
 * never charge, and won't get the AdMob account flagged for invalid traffic.
 *
 * ## Why every ID is per-platform
 *
 * AdMob unit IDs are strictly platform-scoped — an iOS unit ID passed to the
 * Android SDK (or vice versa) silently returns no-fill; the SDK never
 * surfaces the mismatch. Google's own test IDs are ALSO split per platform.
 * We keep the split explicit rather than trying to abstract it away.
 */
expect val platformAppTestId: String
expect val platformAppProdId: String
expect val platformBannerTestId: String
expect val platformBannerProdId: String
expect val platformInterstitialTestId: String
expect val platformInterstitialProdId: String
expect val platformRewardedTestId: String
expect val platformRewardedProdId: String

object AdUnitIds {

    val appId: String
        get() = if (FeatureFlags.isDebug || FeatureFlags.adsAlwaysShow) {
            platformAppTestId
        } else {
            platformAppProdId
        }

    val banner: String
        get() = if (FeatureFlags.isDebug || FeatureFlags.adsAlwaysShow) {
            platformBannerTestId
        } else {
            platformBannerProdId
        }

    val interstitial: String
        get() = if (FeatureFlags.isDebug || FeatureFlags.adsAlwaysShow) {
            platformInterstitialTestId
        } else {
            platformInterstitialProdId
        }

    val rewarded: String
        get() = if (FeatureFlags.isDebug || FeatureFlags.adsAlwaysShow) {
            platformRewardedTestId
        } else {
            platformRewardedProdId
        }
}
