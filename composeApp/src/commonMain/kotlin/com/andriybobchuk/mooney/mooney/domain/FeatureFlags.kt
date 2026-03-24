package com.andriybobchuk.mooney.mooney.domain

object FeatureFlags {
    /**
     * Set to true for development builds.
     * Controls: dev DB file, dev toolbar overlay, mock data tools.
     * IMPORTANT: Must be false for release builds.
     */
    const val isDebug = true

    const val goalsEnabled = false
    const val exchangeEnabled = false
    const val analyticsEnabled = true
    const val exportImportEnabled = true
}
