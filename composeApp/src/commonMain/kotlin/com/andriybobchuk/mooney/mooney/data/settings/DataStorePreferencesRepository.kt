package com.andriybobchuk.mooney.mooney.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.andriybobchuk.mooney.core.data.preferences.StartupPrefs
import com.andriybobchuk.mooney.core.presentation.theme.AppTheme
import com.andriybobchuk.mooney.mooney.domain.settings.ExchangeRateSource
import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository
import com.andriybobchuk.mooney.mooney.domain.settings.ThemeMode
import com.andriybobchuk.mooney.mooney.domain.settings.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class DataStorePreferencesRepository(
    private val dataStore: DataStore<Preferences>,
    private val startupPrefs: StartupPrefs
) : PreferencesRepository {

    override fun getUserPreferences(): Flow<UserPreferences> {
        return dataStore.data.map { preferences ->
            UserPreferences(
                pinnedCategories = preferences[PreferencesKeys.PINNED_CATEGORIES]?.toList() ?: emptyList(),
                defaultCurrency = preferences[PreferencesKeys.DEFAULT_CURRENCY] ?: "USD",
                themeMode = ThemeMode.valueOf(
                    preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
                ),
                appTheme = try {
                    AppTheme.valueOf(
                        preferences[PreferencesKeys.APP_THEME] ?: AppTheme.BLUE.name
                    )
                } catch (e: IllegalArgumentException) {
                    AppTheme.BLUE
                },
                notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                version = preferences[PreferencesKeys.PREFERENCES_VERSION] ?: 1,
                appLanguage = preferences[PreferencesKeys.APP_LANGUAGE] ?: "system",
                onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
                defaultExpenseCategory = preferences[PreferencesKeys.DEFAULT_EXPENSE_CATEGORY] ?: "groceries",
                defaultIncomeCategory = preferences[PreferencesKeys.DEFAULT_INCOME_CATEGORY] ?: "salary",
                excludeTaxesFromTotals = preferences[PreferencesKeys.EXCLUDE_TAXES_FROM_TOTALS] ?: true,
                exchangeRateSource = try {
                    ExchangeRateSource.valueOf(
                        preferences[PreferencesKeys.EXCHANGE_RATE_SOURCE] ?: ExchangeRateSource.EXTENDED.name
                    )
                } catch (e: IllegalArgumentException) {
                    ExchangeRateSource.EXTENDED
                }
            )
        }
    }

    override suspend fun updatePinnedCategories(categoryIds: List<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PINNED_CATEGORIES] = categoryIds.toSet()
        }
    }

    override suspend fun updateDefaultCurrency(currency: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_CURRENCY] = currency
        }
        // Mirror so the next cold-start composition reads the right currency
        // synchronously instead of falling back to USD until DataStore lands.
        startupPrefs.setDefaultCurrency(currency)
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
        // Mirror so cold-start renders the user's chosen theme on frame 1.
        startupPrefs.setThemeMode(themeMode.name)
    }

    override suspend fun updateAppTheme(appTheme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_THEME] = appTheme.name
        }
    }

    override suspend fun updateAppLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = language
        }
    }

    override suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    override suspend fun updateDefaultExpenseCategory(categoryId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_EXPENSE_CATEGORY] = categoryId
        }
    }

    override suspend fun updateDefaultIncomeCategory(categoryId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_INCOME_CATEGORY] = categoryId
        }
    }

    override suspend fun updateExcludeTaxesFromTotals(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXCLUDE_TAXES_FROM_TOTALS] = enabled
        }
    }

    override suspend fun updateExchangeRateSource(source: ExchangeRateSource) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXCHANGE_RATE_SOURCE] = source.name
        }
    }

    override suspend fun markOnboardingCompleted() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = true
        }
        // Once this flips, every subsequent cold-start can pick the start
        // destination without waiting for DataStore — eliminating the blank
        // "Box-and-return" frame on returning launches.
        startupPrefs.setOnboardingCompleted(true)
    }

    override suspend fun clearPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
        // Reset the mirror too so a "wipe data" doesn't leave a stale onboarding
        // flag that would skip the new-install flow.
        startupPrefs.setOnboardingCompleted(false)
    }

    override suspend fun getCurrentPreferences(): UserPreferences {
        val prefs = getUserPreferences().firstOrNull() ?: UserPreferences()
        // Opportunistic backfill: callers that hit this (NavigationHost on
        // boot) are usually the same ones that wanted the synchronous mirror.
        // Writing through here means upgrading installs auto-populate the
        // mirror on first launch with the new code, without a dedicated
        // migration step.
        startupPrefs.setOnboardingCompleted(prefs.onboardingCompleted)
        startupPrefs.setDefaultCurrency(prefs.defaultCurrency)
        startupPrefs.setThemeMode(prefs.themeMode.name)
        return prefs
    }
}