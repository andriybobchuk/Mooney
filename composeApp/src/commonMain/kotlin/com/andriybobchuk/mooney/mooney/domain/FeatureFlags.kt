package com.andriybobchuk.mooney.mooney.domain

object FeatureFlags {
    /**
     * Set to true for development builds.
     * Controls: dev DB file, dev toolbar overlay, mock data tools.
     * IMPORTANT: Must be false for release builds.
     */
    const val isDebug = false

    const val goalsEnabled = true
    const val exchangeEnabled = false
    const val analyticsEnabled = true
    const val exportImportEnabled = true

    /**
     * When `true`, force-show ads on every eligible placement (skip the
     * first-3-sessions grace + the interstitial cooldown) AND use the
     * Google test ad unit IDs so they always fill regardless of AdMob
     * propagation/review status. Lets you verify ad UX on a production
     * build without waiting through real-money inventory.
     *
     * Premium check still applies — Premium users never see ads no matter
     * what this is set to.
     *
     * **Must be `false` for App Store submission.** Shipping with test ad
     * IDs violates AdMob ToS and gets Apple rejections.
     */
    const val adsAlwaysShow = false

    /**
     * Master kill switch for all ads (banner + interstitial + rewarded + the
     * in-feed native row). When `false`, every ad placement returns nothing,
     * the AdMob SDK is not initialized, and no unit ID lookup runs — so the
     * app can ship without any ad plumbing even mattering.
     *
     * Turned off for the first store release so ads never render before
     * we've confirmed AdMob-console app + unit approval on both platforms.
     * Flip to `true` once real Android AdMob IDs are wired AND the iOS log
     * capture proves banners fill correctly on device.
     */
    const val adsEnabled = false

    /**
     * Hides the in-feed native-ad row on the Transactions list. Default off
     * while we tune the placement (it was distracting alongside the user's
     * own daily entries). Flip to true once the design lands and we want it
     * back without a release.
     */
    const val adsOnTransactionsEnabled = false

    /**
     * Fires an interstitial when the user enters the Analytics tab. Default
     * off — Analytics is a calm review surface, not the right beat for an
     * interruption. The banner at the bottom of the screen is enough.
     */
    const val interstitialOnAnalyticsEnabled = false
}
