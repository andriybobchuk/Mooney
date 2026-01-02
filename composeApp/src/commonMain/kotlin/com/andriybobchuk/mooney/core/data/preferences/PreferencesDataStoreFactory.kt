package com.andriybobchuk.mooney.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

expect class PreferencesDataStoreFactory {
    fun create(): DataStore<Preferences>
}