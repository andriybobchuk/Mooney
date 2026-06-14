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
    const val adsAlwaysShow = true
}
