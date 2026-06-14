package com.andriybobchuk.mooney.core.ads

/**
 * Where in the UI an ad can appear. The rule is: anything not listed here is
 * NOT an ad surface — keep the main Transactions feed sacred.
 *
 * Adding a new placement is a deliberate, reviewed decision (don't add lightly):
 * each one is a contract with users that they'll see ads there and nowhere
 * else.
 */
enum class AdPlacement {
    /** Bottom of Settings screen. Quiet, always-on banner. */
    SETTINGS_BANNER,

    /** Bottom of Analytics breakdown deep-views (Revenue / Costs / Taxes / Net Income). */
    ANALYTICS_BREAKDOWN_BANNER,

    /** Bottom of Categories management screen. */
    CATEGORIES_BANNER,

    /** Bottom of Assets screen — mirrors the Settings banner footprint. */
    ASSETS_BANNER,

    /** Single sponsored row at index ~25 inside the Transactions list. */
    TRANSACTIONS_NATIVE_ROW,

    /**
     * Fires on entering the Analytics tab — a deliberate destination, more
     * tolerable for a brief ad break than returning to the Transactions
     * "home". Name kept for migration compatibility (DataStore key references).
     */
    INTERSTITIAL_RETURN_TO_TRANSACTIONS,

    /** User-initiated. "Watch ad to unlock X for 24h." */
    REWARDED_FEATURE_UNLOCK
}
