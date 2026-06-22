package com.andriybobchuk.mooney.mooney.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object PreferencesKeys {
    val PINNED_CATEGORIES = stringSetPreferencesKey("pinned_categories")
    val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val APP_THEME = stringPreferencesKey("app_theme")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val PREFERENCES_VERSION = intPreferencesKey("preferences_version")
    val APP_LANGUAGE = stringPreferencesKey("app_language")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val IS_PREMIUM = booleanPreferencesKey("is_premium")
    val CUSTOM_CATEGORY_COUNT = intPreferencesKey("custom_category_count")
    val DEFAULT_EXPENSE_CATEGORY = stringPreferencesKey("default_expense_category")
    val DEFAULT_INCOME_CATEGORY = stringPreferencesKey("default_income_category")
    val EXCLUDE_TAXES_FROM_TOTALS = booleanPreferencesKey("exclude_taxes_from_totals")
    val CURRENCY_INSIGHTS_ENABLED = booleanPreferencesKey("currency_insights_enabled")
    val DEFAULTS_VERSION = intPreferencesKey("defaults_version")
    val LAST_USAGE_REPORT = stringPreferencesKey("last_category_usage_report")
    val EXCHANGE_RATE_SOURCE = stringPreferencesKey("exchange_rate_source")
    val DEVELOPER_OPTIONS_ENABLED = booleanPreferencesKey("developer_options_enabled")
    // Review prompt gating
    val INSTALL_TIMESTAMP = androidx.datastore.preferences.core.longPreferencesKey("install_timestamp")
    val APP_OPEN_COUNT = intPreferencesKey("app_open_count")
    val LAST_REVIEW_PROMPT_TIMESTAMP = androidx.datastore.preferences.core.longPreferencesKey("last_review_prompt_timestamp")
    val LAST_WIDGET_PAGE = intPreferencesKey("last_widget_page")
    /**
     * Off by default — when off, the Transactions screen renders only the
     * spending-calendar widget (no pager, no dots indicator). Turning it on
     * (via Developer Options) restores the multi-page widget pager with
     * spending trends, currency rates and the suggest-a-widget card.
     */
    val WIDGET_PAGER_ENABLED = booleanPreferencesKey("widget_pager_enabled")
    // Analytics — first-time event flags. Each guards a one-shot event so we
    // can distinguish activation moments (first transaction, first account)
    // from ongoing engagement in Firebase funnels.
    val ANALYTICS_FIRST_ACCOUNT_FIRED = booleanPreferencesKey("analytics_first_account_fired")
    val ANALYTICS_FIRST_TRANSACTION_FIRED = booleanPreferencesKey("analytics_first_transaction_fired")
    // Ads — frequency-capping counters used by AdEligibilityUseCase. All are
    // best-effort; if a write loses a race, worst case we show one extra ad
    // (capped at the next check anyway). See core/ads/Ads.kt.
    val ADS_LAST_INTERSTITIAL_TIMESTAMP = androidx.datastore.preferences.core.longPreferencesKey("ads_last_interstitial_timestamp")
    val ADS_LAST_APP_OPEN_DAY = intPreferencesKey("ads_last_app_open_day")
    /** Last-shown timestamp per banner placement — used to cap banner frequency. */
    val ADS_LAST_BANNER_ANALYTICS = androidx.datastore.preferences.core.longPreferencesKey("ads_last_banner_analytics")
    val ADS_LAST_BANNER_SETTINGS = androidx.datastore.preferences.core.longPreferencesKey("ads_last_banner_settings")
    val ADS_LAST_BANNER_CATEGORIES = androidx.datastore.preferences.core.longPreferencesKey("ads_last_banner_categories")
    val ADS_LAST_BANNER_ASSETS = androidx.datastore.preferences.core.longPreferencesKey("ads_last_banner_assets")
    /** PIN hash for App Lock — null/missing means lock is disabled. */
    val APP_LOCK_PIN_HASH = androidx.datastore.preferences.core.stringPreferencesKey("app_lock_pin_hash")
    /** Developer kill-switch for all ads — read by AdEligibilityUseCase. */
    val ADS_DISABLED_DEV = booleanPreferencesKey("ads_disabled_dev")
    /**
     * Developer force-show: bypass new-user grace + per-placement cooldown so
     * every eligible surface fills immediately. Mirrors
     * FeatureFlags.adsAlwaysShow but is toggleable at runtime, used when
     * verifying ad placements on a freshly-installed build that's still inside
     * the 3-session grace window.
     */
    val ADS_FORCE_SHOW_DEV = booleanPreferencesKey("ads_force_show_dev")
    /**
     * When true, the Assets top-bar shows the gross-assets total instead of
     * net worth (assets − liabilities). Only takes visible effect when the
     * user actually has liabilities; otherwise the two are equal.
     */
    val ASSETS_ONLY_IN_TOP_BAR = booleanPreferencesKey("assets_only_in_top_bar")
    // Reminder notifications — opt-in. Mode is stored as the
    // ReminderMode enum name ("OFF" / "DAILY" / "WEEKLY"). Hour/minute apply
    // to both daily and weekly; weekday only applies when mode == WEEKLY
    // and uses ISO numbering (1 = Mon … 7 = Sun).
    val REMINDER_MODE = stringPreferencesKey("reminder_mode")
    val REMINDER_HOUR = intPreferencesKey("reminder_hour")
    val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    val REMINDER_WEEKDAY = intPreferencesKey("reminder_weekday")
}