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

    // Per-platform paywall. Android is hard-coded off in `paywallEnabled()`
    // for this release, so the Android key is intentionally absent from the
    // client side. The iOS key remains RC-driven.
    private const val PAYWALL_ENABLED_IOS = "paywall_enabled_ios"

    // Per-platform ads keys are RC-registered but the client always
    // short-circuits to `false` for this release — iOS has no ad bridge
    // yet and the Android SDK is wired against test unit IDs. Both keys
    // are intentionally absent client-side. Restore the RC read for both
    // platforms once the real unit IDs land.

    // Goals feature — full section on/off. If off, the entry point on Assets
    // stops showing and the deeplink returns null.
    private const val GOALS_ENABLED = "goals_enabled"

    // Free-tier limits, only enforced when paywall is enabled for the
    // current platform. Ints stored as strings so RC's default type works.
    private const val FREE_ACCOUNTS = "free_accounts"
    private const val FREE_CATEGORIES = "free_categories"

    // ---- accessors ----

    fun paywallEnabled(): Boolean {
        // First-release hard override — the Android paywall / billing wiring
        // isn't verified for this build, so we lock it OFF at the source and
        // ignore whatever the console eventually publishes. Once the Android
        // billing flow is validated, delete this early-return and let the
        // key drive it.
        if (!isIosPlatform) return false
        return RemoteConfig.getString(PAYWALL_ENABLED_IOS).toBooleanStrictOrNull()
            ?: DEFAULT_PAYWALL_ENABLED_IOS
    }

    @Suppress("FunctionOnlyReturningConstant")
    fun adsEnabled(): Boolean {
        // First-release hard override on BOTH platforms — the Android
        // `AdUnitIds` still reference Google test unit IDs (see
        // AdUnitIds.android.kt TODO) and we don't want iOS ads either
        // until real inventory is verified. Restore the platform-branched
        // `RemoteConfig.getString(...)` read once the real ad wiring lands
        // on both platforms.
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
    private const val DEFAULT_GOALS_ENABLED = true
    private const val DEFAULT_FREE_ACCOUNTS = 20
    private const val DEFAULT_FREE_CATEGORIES = 15
}

/** True on iOS builds, false on Android. Used to pick per-platform RC keys. */
expect val isIosPlatform: Boolean
