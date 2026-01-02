package com.andriybobchuk.mooney.mooney.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.andriybobchuk.mooney.core.presentation.theme.AppTheme
import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository
import com.andriybobchuk.mooney.mooney.domain.settings.ThemeMode
import com.andriybobchuk.mooney.mooney.domain.settings.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class DataStorePreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    override fun getUserPreferences(): Flow<UserPreferences> {
        return dataStore.data.map { preferences ->
            UserPreferences(
                pinnedCategories = preferences[PreferencesKeys.PINNED_CATEGORIES]?.toList() ?: emptyList(),
                defaultCurrency = preferences[PreferencesKeys.DEFAULT_CURRENCY] ?: "PLN",
                themeMode = ThemeMode.valueOf(
                    preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
                ),
                appTheme = try {
                    AppTheme.valueOf(
                        preferences[PreferencesKeys.APP_THEME] ?: AppTheme.PURPLE.name
                    )
                } catch (e: IllegalArgumentException) {
                    AppTheme.PURPLE
                },
                notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                version = preferences[PreferencesKeys.PREFERENCES_VERSION] ?: 1
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
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    override suspend fun updateAppTheme(appTheme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_THEME] = appTheme.name
        }
    }

    override suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    override suspend fun clearPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    override suspend fun getCurrentPreferences(): UserPreferences {
        return getUserPreferences().firstOrNull() ?: UserPreferences()
    }
}