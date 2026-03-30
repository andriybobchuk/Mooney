package com.andriybobchuk.mooney.core.premium

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class PremiumManager(
    private val dataStore: DataStore<Preferences>
) {
    val isPremium: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.IS_PREMIUM] ?: false
    }

    suspend fun getIsPremium(): Boolean {
        return dataStore.data.firstOrNull()?.get(PreferencesKeys.IS_PREMIUM) ?: false
    }

    suspend fun setPremium(premium: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_PREMIUM] = premium
        }
    }
}
