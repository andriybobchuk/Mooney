package com.andriybobchuk.mooney.core.ads

/**
 * Common entry point the rest of the app uses to interact with ads.
 *
 * Same shape as [com.andriybobchuk.mooney.core.analytics.Analytics] — the iOS
 * actual delegates to a Swift bridge wired in `iOSApp.swift`. The Android
 * actual will plug Google Mobile Ads directly (no bridge needed since the
 * SDK is JVM).
 *
 * This object is a thin dispatcher; all gating, eligibility, frequency
 * capping, and placement decisions live in [AdEligibilityUseCase] and the
 * presentation layer.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object Ads {
    /** Idempotent. Safe to call multiple times. */
    fun initialize()

    /**
     * Pre-load an interstitial so it's ready when the placement triggers.
     * Callers should pass [AdUnitIds.interstitial] — the explicit-id parameter
     * exists so we can A/B test different ad units later without changing
     * call sites.
     */
    fun preloadInterstitial(adUnitId: String)

    /**
     * Returns true if a preloaded interstitial was actually shown. Frequency
     * capping should be checked BEFORE calling this — the SDK won't enforce
     * Mooney-specific rules.
     */
    fun showInterstitialIfReady(): Boolean

    /** Pre-load a rewarded ad. Pass [AdUnitIds.rewarded]. */
    fun preloadRewarded(adUnitId: String)

    /**
     * @param onReward fired when the user earns the reward (NOT when they
     *     dismiss without watching to completion). Use to grant the temp
     *     premium unlock / CSV export / etc.
     */
    fun showRewarded(onReward: () -> Unit, onDismissed: () -> Unit)
}
