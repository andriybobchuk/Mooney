package com.andriybobchuk.mooney.mooney.domain.usecase.assets

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.andriybobchuk.mooney.core.data.database.AssetCategoryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ManageCategoryExpansionUseCase(
    private val dataStore: DataStore<Preferences>,
    private val assetCategoryDao: AssetCategoryDao
) {
    private val expandedCategoriesKey = stringPreferencesKey("expanded_asset_categories")

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun getExpandedCategories(): Flow<Set<String>> {
        return dataStore.data.map { preferences ->
            val expandedString = preferences[expandedCategoriesKey]
            if (expandedString != null) {
                try {
                    json.decodeFromString<List<String>>(expandedString).toSet()
                } catch (e: Exception) {
                    allCategoryIds()
                }
            } else {
                allCategoryIds()
            }
        }
    }

    private suspend fun allCategoryIds(): Set<String> {
        return try {
            assetCategoryDao.getAll().first().map { it.id }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    suspend fun saveExpandedCategories(expandedCategories: Set<String>) {
        dataStore.edit { preferences ->
            preferences[expandedCategoriesKey] = json.encodeToString(expandedCategories.toList())
        }
    }

    suspend fun toggleCategoryExpansion(category: String, currentExpanded: Set<String>) {
        val newExpanded = if (currentExpanded.contains(category)) {
            currentExpanded - category
        } else {
            currentExpanded + category
        }
        saveExpandedCategories(newExpanded)
    }
}