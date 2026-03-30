package com.andriybobchuk.mooney.core.premium

/**
 * Configurable limits for free tier. Values can be overridden by remote config in the future.
 */
object PremiumConfig {
    var maxFreeAccounts: Int = 2
    var maxFreeCustomCategories: Int = 3
}
