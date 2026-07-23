package com.andriybobchuk.mooney.core.data.category

/**
 * Typed accessors over Firebase Remote Config.
 *
 * Every key has a compile-time-safe fallback so the app behaves sensibly
 * before the first successful fetch and forever on platforms where RC init
 * quietly fails (offline, Firebase misconfig). Defaults are picked to match
 * what the app ships without any RC involvement so misfetches never surprise.
 *
 * ## Versioning
 * We stamp a user property `install_version` on first launch (see
 * NavigationHost). Firebase RC console conditions can target this property
 * to serve different values to older installs vs new ones — that's how "old
 * users keep this, new users get that" is expressed without adding a schema
 * dimension client-side. Firebase's built-in `first_open_time` handles pure
 * date-based rollouts too, no extra client work needed.
 */
object RemoteConfigKeys {

    // ---- keys ----

    // Per-platform paywall. `false` = fully free tier, no limits, no premium
    // UI. `true` = paywall shown, limits enforced. Kept separate so we can
    // roll out iOS-first (as we do today) without changing Android UX.
    private const val PAYWALL_ENABLED_ANDROID = "paywall_enabled_android"
    private const val PAYWALL_ENABLED_IOS = "paywall_enabled_ios"

    // Per-platform ads. Master kill switch complements FeatureFlags.adsEnabled
    // — either being false suppresses every ad placement. The RC key lets us
    // enable ads for one platform at a time once AdMob approvals land.
    private const val ADS_ENABLED_ANDROID = "ads_enabled_android"
    private const val ADS_ENABLED_IOS = "ads_enabled_ios"

    // Goals feature — full section on/off. If off, the entry point on Assets
    // stops showing and the deeplink returns null.
    private const val GOALS_ENABLED = "goals_enabled"

    // Free-tier limits, only enforced when paywall is enabled for the
    // current platform. Ints stored as strings so RC's default type works.
    private const val FREE_ACCOUNTS = "free_accounts"
    private const val FREE_CATEGORIES = "free_categories"

    // ---- accessors ----

    fun paywallEnabled(): Boolean {
        val key = if (isIosPlatform) PAYWALL_ENABLED_IOS else PAYWALL_ENABLED_ANDROID
        return RemoteConfig.getString(key).toBooleanStrictOrNull() ?: DEFAULT_PAYWALL_ENABLED
    }

    fun adsEnabled(): Boolean {
        val key = if (isIosPlatform) ADS_ENABLED_IOS else ADS_ENABLED_ANDROID
        return RemoteConfig.getString(key).toBooleanStrictOrNull() ?: DEFAULT_ADS_ENABLED
    }

    fun goalsEnabled(): Boolean =
        RemoteConfig.getString(GOALS_ENABLED).toBooleanStrictOrNull() ?: DEFAULT_GOALS_ENABLED

    fun freeAccounts(): Int =
        RemoteConfig.getString(FREE_ACCOUNTS).toIntOrNull() ?: DEFAULT_FREE_ACCOUNTS

    fun freeCategories(): Int =
        RemoteConfig.getString(FREE_CATEGORIES).toIntOrNull() ?: DEFAULT_FREE_CATEGORIES

    // ---- defaults ----
    // These match the current shipping values so a total RC failure keeps
    // the app behaving exactly as today's build.
    private const val DEFAULT_PAYWALL_ENABLED = true
    private const val DEFAULT_ADS_ENABLED = false
    private const val DEFAULT_GOALS_ENABLED = true
    private const val DEFAULT_FREE_ACCOUNTS = 5
    private const val DEFAULT_FREE_CATEGORIES = 5
}

/** True on iOS builds, false on Android. Used to pick per-platform RC keys. */
expect val isIosPlatform: Boolean
