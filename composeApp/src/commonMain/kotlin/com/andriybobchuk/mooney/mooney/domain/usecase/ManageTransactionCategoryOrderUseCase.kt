package com.andriybobchuk.mooney.mooney.domain.usecase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists the user's manual ordering of top-level transaction categories
 * (e.g. Food, Transport, Salary) so they can pin most-used categories to the
 * top of the category picker.
 *
 * Storage: DataStore as a JSON-encoded `List<String>` of category IDs.
 * Categories not present in the saved order keep their schema-default position
 * (sorted to the end, preserving relative order).
 */
class ManageTransactionCategoryOrderUseCase(
    private val dataStore: DataStore<Preferences>
) {
    private val orderKey = stringPreferencesKey("transaction_category_order")

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun getCategoryOrder(): Flow<List<String>> {
        return dataStore.data.map { preferences ->
            val raw = preferences[orderKey] ?: return@map emptyList()
            try {
                json.decodeFromString<List<String>>(raw)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun saveCategoryOrder(categoryIds: List<String>) {
        dataStore.edit { preferences ->
            preferences[orderKey] = json.encodeToString(categoryIds)
        }
    }
}
