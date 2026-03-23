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

class ManageAssetCategoryOrderUseCase(
    private val dataStore: DataStore<Preferences>
) {
    private val assetCategoryOrderKey = stringPreferencesKey("asset_category_order")
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true 
    }

    fun getCategoryOrder(): Flow<List<AssetCategory>> {
        return dataStore.data.map { preferences ->
            val orderString = preferences[assetCategoryOrderKey]
            if (orderString != null) {
                try {
                    val categoryNames = json.decodeFromString<List<String>>(orderString)
                    categoryNames.mapNotNull { name ->
                        try {
                            AssetCategory.valueOf(name)
                        } catch (e: Exception) {
                            null
                        }
                    }.ifEmpty { AssetCategory.entries }
                } catch (e: Exception) {
                    AssetCategory.entries
                }
            } else {
                // Default order - prioritize safer assets first
                listOf(
                    AssetCategory.CASH,
                    AssetCategory.BANK_ACCOUNT,
                    AssetCategory.REAL_ESTATE,
                    AssetCategory.PRECIOUS_METALS,
                    AssetCategory.BONDS,
                    AssetCategory.RETIREMENT,
                    AssetCategory.STOCKS,
                    AssetCategory.BUSINESS,
                    AssetCategory.COLLECTIBLES,
                    AssetCategory.CRYPTO,
                    AssetCategory.OTHER
                )
            }
        }
    }

    suspend fun saveCategoryOrder(categories: List<AssetCategory>) {
        dataStore.edit { preferences ->
            val categoryNames = categories.map { it.name }
            preferences[assetCategoryOrderKey] = json.encodeToString(categoryNames)
        }
    }

    suspend fun resetToDefault() {
        dataStore.edit { preferences ->
            preferences.remove(assetCategoryOrderKey)
        }
    }
}