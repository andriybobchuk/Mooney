package com.andriybobchuk.mooney.core.premium

import com.andriybobchuk.mooney.core.data.category.RemoteConfigKeys

/**
 * Free-tier caps. Values are pulled through Remote Config so we can adjust
 * them post-release without cutting a build. The var overrides remain for
 * dev tools that override at runtime; the RC accessors are the primary
 * read path for gates like AccountAdd and Category limits.
 */
object PremiumConfig {
    val maxFreeAccounts: Int get() = RemoteConfigKeys.freeAccounts()
    val maxFreeCustomCategories: Int get() = RemoteConfigKeys.freeCategories()
}
