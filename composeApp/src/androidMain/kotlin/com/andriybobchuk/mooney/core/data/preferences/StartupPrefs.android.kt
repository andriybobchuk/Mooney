package com.andriybobchuk.mooney.core.data.preferences

import android.content.Context

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class StartupPrefs(context: Context) {
    // commit() vs apply(): we use apply() here. The DataStore write happens
    // immediately after on the same coroutine, so the SharedPreferences write
    // doesn't have to be durable on its own — if the app crashes between, the
    // DataStore mirror is still correct.
    private val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    actual fun getOnboardingCompleted(): Boolean? =
        if (prefs.contains(KEY_ONBOARDING_COMPLETED)) {
            prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        } else null

    actual fun setOnboardingCompleted(value: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()
    }

    actual fun getDefaultCurrency(): String? =
        prefs.getString(KEY_DEFAULT_CURRENCY, null)

    actual fun setDefaultCurrency(value: String) {
        prefs.edit().putString(KEY_DEFAULT_CURRENCY, value).apply()
    }

    actual fun getThemeMode(): String? =
        prefs.getString(KEY_THEME_MODE, null)

    actual fun setThemeMode(value: String) {
        prefs.edit().putString(KEY_THEME_MODE, value).apply()
    }

    private companion object {
        const val NAME = "mooney_startup"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val KEY_DEFAULT_CURRENCY = "default_currency"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
