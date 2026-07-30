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

    // Per-platform paywall. Both platforms have their own key so we can kill
    // one without touching the other — Play Store review and App Review land
    // on independent timelines and the paywall status is what most often
    // triggers a rejection.
    private const val PAYWALL_ENABLED_IOS = "paywall_enabled_ios"
    private const val PAYWALL_ENABLED_ANDROID = "paywall_enabled_android"

    // Per-platform ads kill switch. Independent iOS/Android keys so we can
    // roll out ads on one store while the other's review is still pending
    // (Apple and Google approve on independent timelines). Both default OFF
    // — flip in Firebase Remote Config when the ad units are validated in
    // the field.
    private const val ADS_ENABLED_IOS = "ads_enabled_ios"
    private const val ADS_ENABLED_ANDROID = "ads_enabled_android"

    // Goals feature — full section on/off. If off, the entry point on Assets
    // stops showing and the deeplink returns null.
    private const val GOALS_ENABLED = "goals_enabled"

    // Free-tier limits, only enforced when paywall is enabled for the
    // current platform. Ints stored as strings so RC's default type works.
    private const val FREE_ACCOUNTS = "free_accounts"
    private const val FREE_CATEGORIES = "free_categories"

    // ---- accessors ----

    fun paywallEnabled(): Boolean {
        // Android is hard-locked off — no Google Payments merchant account
        // exists yet, so there's nothing StoreKit-equivalent can transact
        // against. To lift: create the merchant account + subscriptions in
        // Play Console, then delete this early-return.
        if (!isIosPlatform) return false
        // iOS reads the live RC key — the iOS paywall / monthly subscription
        // is shipping in production today.
        return RemoteConfig.getString(PAYWALL_ENABLED_IOS).toBooleanStrictOrNull()
            ?: DEFAULT_PAYWALL_ENABLED_IOS
    }

    @Suppress("FunctionOnlyReturningConstant")
    fun adsEnabled(): Boolean {
        // HARD OVERRIDE — locked off on both platforms until we're ready to
        // start serving real ads. Same lift procedure as [paywallEnabled]:
        // delete the early-return and let [ADS_ENABLED_IOS] / [ADS_ENABLED_ANDROID]
        // + their defaults drive it.
        return false
    }

    fun goalsEnabled(): Boolean =
        RemoteConfig.getString(GOALS_ENABLED).toBooleanStrictOrNull() ?: DEFAULT_GOALS_ENABLED

    fun freeAccounts(): Int =
        RemoteConfig.getString(FREE_ACCOUNTS).toIntOrNull() ?: DEFAULT_FREE_ACCOUNTS

    fun freeCategories(): Int =
        RemoteConfig.getString(FREE_CATEGORIES).toIntOrNull() ?: DEFAULT_FREE_CATEGORIES

    // ---- defaults ----
    // Mirror the Firebase Remote Config console "Default value" column so a
    // total RC failure produces the same behavior as the initial console
    // rollout. Update both places in lockstep whenever the console changes.
    private const val DEFAULT_PAYWALL_ENABLED_IOS = true
    // Default OFF for Android — safer to require an explicit console flip
    // once real-money billing has been validated on-device, rather than
    // rolling out to all Android users the moment this ships.
    private const val DEFAULT_PAYWALL_ENABLED_ANDROID = false
    // Both ad defaults are OFF so a total RC failure doesn't accidentally
    // start serving ads to the field. Enabling ads requires an explicit
    // console flip, per platform.
    private const val DEFAULT_ADS_ENABLED_IOS = false
    private const val DEFAULT_ADS_ENABLED_ANDROID = false
    private const val DEFAULT_GOALS_ENABLED = true
    private const val DEFAULT_FREE_ACCOUNTS = 20
    private const val DEFAULT_FREE_CATEGORIES = 15
}

/** True on iOS builds, false on Android. Used to pick per-platform RC keys. */
expect val isIosPlatform: Boolean
