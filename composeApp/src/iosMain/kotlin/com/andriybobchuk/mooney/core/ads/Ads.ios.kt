package com.andriybobchuk.mooney.core.ads

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object Ads {
    // Module-internal so iosMain Composables (the banner view actual) can
    // pull a UIView out of the bridge without going through every method on
    // [Ads]. External callers still go through the actual fun surface below.
    internal var bridge: IosAdsBridge? = null
        private set

    /**
     * Wire from `iOSApp.swift` BEFORE the first composition reads the bridge.
     * Mirrors `Analytics.shared.setBridge(...)` in the existing iOS init.
     */
    fun setBridge(bridge: IosAdsBridge) {
        println("[Ads] setBridge called: $bridge")
        this.bridge = bridge
    }

    actual fun initialize() {
        // Match the Android early-return: if ads are hard-coded off for
        // this release we never call `MobileAds.shared.start(...)` on the
        // Swift bridge, so no AdMob SDK bootstrap runs and the advertising
        // identifier stays untouched. Restore SDK start once ads flip on.
        if (!com.andriybobchuk.mooney.mooney.domain.FeatureFlags.adsEnabled) return
        println("[Ads] initialize: bridge=${bridge}")
        bridge?.initialize()
    }

    actual fun preloadInterstitial(adUnitId: String) {
        bridge?.preloadInterstitial(adUnitId)
    }

    actual fun showInterstitialIfReady(): Boolean =
        bridge?.showInterstitialIfReady() ?: false

    actual fun preloadRewarded(adUnitId: String) {
        bridge?.preloadRewarded(adUnitId)
    }

    actual fun showRewarded(onReward: () -> Unit, onDismissed: () -> Unit) {
        // No bridge wired ⇒ no ad to show — treat as "user dismissed without
        // reward" so the caller's UI doesn't hang waiting for a callback.
        val b = bridge ?: run {
            onDismissed()
            return
        }
        b.showRewarded(onReward, onDismissed)
    }
}
