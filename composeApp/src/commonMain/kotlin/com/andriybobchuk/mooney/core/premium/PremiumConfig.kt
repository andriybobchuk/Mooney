package com.andriybobchuk.mooney.core.premium

/**
 * Configurable limits for free tier. Values can be overridden by remote config in the future.
 */
object PremiumConfig {
    // Free tier limits — also used by Apple Review to test the paywall flow.
    var maxFreeAccounts: Int = 20
    var maxFreeCustomCategories: Int = 50
}
