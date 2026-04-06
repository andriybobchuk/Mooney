package com.andriybobchuk.mooney.core.premium

/**
 * Configurable limits for free tier. Values can be overridden by remote config in the future.
 */
object PremiumConfig {
    // Lifted for launch — restore stricter limits once premium value is proven
    var maxFreeAccounts: Int = 100
    var maxFreeCustomCategories: Int = 100
}
