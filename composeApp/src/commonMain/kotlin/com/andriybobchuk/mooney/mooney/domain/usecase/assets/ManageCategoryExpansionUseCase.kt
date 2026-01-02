package com.andriybobchuk.mooney.mooney.domain.usecase.assets

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.andriybobchuk.mooney.mooney.domain.AssetCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ManageCategoryExpansionUseCase(
    private val dataStore: DataStore<Preferences>
) {
    private val EXPANDED_CATEGORIES_KEY = stringPreferencesKey("expanded_asset_categories")
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true 
    }

    fun getExpandedCategories(): Flow<Set<AssetCategory>> {
        return dataStore.data.map { preferences ->
            val expandedString = preferences[EXPANDED_CATEGORIES_KEY]
            if (expandedString != null) {
                try {
                    val categoryNames = json.decodeFromString<List<String>>(expandedString)
                    categoryNames.mapNotNull { name ->
                        try {
                            AssetCategory.valueOf(name)
                        } catch (e: Exception) {
                            null
                        }
                    }.toSet()
                } catch (e: Exception) {
                    // Default: all categories expanded
                    AssetCategory.entries.toSet()
                }
            } else {
                // Default: all categories expanded
                AssetCategory.entries.toSet()
            }
        }
    }

    suspend fun saveExpandedCategories(expandedCategories: Set<AssetCategory>) {
        dataStore.edit { preferences ->
            val categoryNames = expandedCategories.map { it.name }
            preferences[EXPANDED_CATEGORIES_KEY] = json.encodeToString(categoryNames)
        }
    }

    suspend fun toggleCategoryExpansion(category: AssetCategory, currentExpanded: Set<AssetCategory>) {
        val newExpanded = if (currentExpanded.contains(category)) {
            currentExpanded - category
        } else {
            currentExpanded + category
        }
        saveExpandedCategories(newExpanded)
    }
}