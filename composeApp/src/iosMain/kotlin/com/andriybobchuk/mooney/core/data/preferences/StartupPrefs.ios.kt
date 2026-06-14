package com.andriybobchuk.mooney.core.data.preferences

import platform.Foundation.NSUserDefaults

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class StartupPrefs {
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults

    actual fun getOnboardingCompleted(): Boolean? =
        if (defaults.objectForKey(KEY_ONBOARDING_COMPLETED) != null) {
            defaults.boolForKey(KEY_ONBOARDING_COMPLETED)
        } else null

    actual fun setOnboardingCompleted(value: Boolean) {
        defaults.setBool(value, KEY_ONBOARDING_COMPLETED)
    }

    actual fun getDefaultCurrency(): String? =
        defaults.stringForKey(KEY_DEFAULT_CURRENCY)

    actual fun setDefaultCurrency(value: String) {
        defaults.setObject(value, KEY_DEFAULT_CURRENCY)
    }

    actual fun getThemeMode(): String? =
        defaults.stringForKey(KEY_THEME_MODE)

    actual fun setThemeMode(value: String) {
        defaults.setObject(value, KEY_THEME_MODE)
    }

    private companion object {
        const val KEY_ONBOARDING_COMPLETED = "mooney_onboarding_completed"
        const val KEY_DEFAULT_CURRENCY = "mooney_default_currency"
        const val KEY_THEME_MODE = "mooney_theme_mode"
    }
}
