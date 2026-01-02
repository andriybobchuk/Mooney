package com.andriybobchuk.mooney.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import java.io.File

actual class PreferencesDataStoreFactory(
    private val context: Context
) {
    actual fun create(): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                val file = File(context.filesDir, "mooney_preferences.preferences_pb")
                file.absolutePath.toPath()
            }
        )
    }
}