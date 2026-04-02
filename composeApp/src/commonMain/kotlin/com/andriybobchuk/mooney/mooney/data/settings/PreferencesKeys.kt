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
}