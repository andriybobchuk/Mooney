package com.andriybobchuk.mooney.mooney.domain.usecase.assets

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.andriybobchuk.mooney.core.data.database.AssetCategoryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ManageAssetCategoryOrderUseCase(
    private val dataStore: DataStore<Preferences>,
    private val assetCategoryDao: AssetCategoryDao
) {
    private val assetCategoryOrderKey = stringPreferencesKey("asset_category_order")

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun getCategoryOrder(): Flow<List<String>> {
        return dataStore.data.map { preferences ->
            val orderString = preferences[assetCategoryOrderKey]
            if (orderString != null) {
                try {
                    json.decodeFromString<List<String>>(orderString)
                        .ifEmpty { defaultCategoryOrder() }
                } catch (e: Exception) {
                    defaultCategoryOrder()
                }
            } else {
                defaultCategoryOrder()
            }
        }
    }

    private fun defaultCategoryOrder(): List<String> = listOf(
        "CASH",
        "BANK_ACCOUNT",
        "REAL_ESTATE",
        "PRECIOUS_METALS",
        "BONDS",
        "RETIREMENT",
        "STOCKS",
        "BUSINESS",
        "COLLECTIBLES",
        "CRYPTO",
        "OTHER"
    )

    suspend fun saveCategoryOrder(categories: List<String>) {
        dataStore.edit { preferences ->
            preferences[assetCategoryOrderKey] = json.encodeToString(categories)
        }
    }

    suspend fun resetToDefault() {
        dataStore.edit { preferences ->
            preferences.remove(assetCategoryOrderKey)
        }
    }
}