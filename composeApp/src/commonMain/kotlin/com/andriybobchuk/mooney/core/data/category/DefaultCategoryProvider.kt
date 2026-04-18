package com.andriybobchuk.mooney.core.data.category

import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import mooney.composeapp.generated.resources.Res

interface DefaultCategoryProvider {
    suspend fun getTransactionCategories(): DefaultCategorySet
    suspend fun getAssetCategories(): DefaultAssetCategorySet
}

@OptIn(ExperimentalResourceApi::class)
class BundledCategoryProvider : DefaultCategoryProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getTransactionCategories(): DefaultCategorySet {
        val bytes = Res.readBytes("files/default_transaction_categories.json")
        return json.decodeFromString(bytes.decodeToString())
    }

    override suspend fun getAssetCategories(): DefaultAssetCategorySet {
        val bytes = Res.readBytes("files/default_asset_categories.json")
        return json.decodeFromString(bytes.decodeToString())
    }
}

class RemoteCategoryProvider(
    private val bundled: BundledCategoryProvider
) : DefaultCategoryProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getTransactionCategories(): DefaultCategorySet {
        return try {
            RemoteConfig.fetchAndActivate()
            val remoteJson = RemoteConfig.getString("default_transaction_categories")
            if (remoteJson.isNotBlank()) {
                val remote = json.decodeFromString<DefaultCategorySet>(remoteJson)
                val bundled = bundled.getTransactionCategories()
                if (remote.version > bundled.version) remote else bundled
            } else {
                bundled.getTransactionCategories()
            }
        } catch (_: Exception) {
            bundled.getTransactionCategories()
        }
    }

    override suspend fun getAssetCategories(): DefaultAssetCategorySet {
        return try {
            val remoteJson = RemoteConfig.getString("default_asset_categories")
            if (remoteJson.isNotBlank()) {
                val remote = json.decodeFromString<DefaultAssetCategorySet>(remoteJson)
                val bundled = bundled.getAssetCategories()
                if (remote.version > bundled.version) remote else bundled
            } else {
                bundled.getAssetCategories()
            }
        } catch (_: Exception) {
            bundled.getAssetCategories()
        }
    }
}
