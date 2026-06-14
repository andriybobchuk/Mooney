package com.andriybobchuk.mooney.core.data.preferences

/**
 * Tiny synchronous mirror of the three preferences `NavigationHost` and `App()`
 * need on the FIRST composition pass — before DataStore's async read returns.
 *
 * - `onboardingCompleted`: lets NavigationHost pick a start destination on
 *   frame 1 instead of blocking on DataStore (the "blank Box" boot stall).
 * - `defaultCurrency`: seeds `GlobalConfig.baseCurrency` synchronously.
 * - `themeMode`: seeds the initial value of `collectAsState` so users don't
 *   see a one-frame wrong-theme flash.
 *
 * DataStore remains the source of truth for writes; this is a write-through
 * cache backed by SharedPreferences (Android) / NSUserDefaults (iOS), both of
 * which expose synchronous reads.
 *
 * Returns `null` when the value has never been written, which is the signal
 * for callers to fall back to the existing async DataStore path (one slow
 * boot on first install; instant from then on).
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class StartupPrefs {
    fun getOnboardingCompleted(): Boolean?
    fun setOnboardingCompleted(value: Boolean)

    fun getDefaultCurrency(): String?
    fun setDefaultCurrency(value: String)

    fun getThemeMode(): String?
    fun setThemeMode(value: String)
}
