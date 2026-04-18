package com.andriybobchuk.mooney.core.data.category

import kotlinx.serialization.Serializable

@Serializable
data class DefaultCategorySet(
    val version: Int,
    val categories: List<DefaultTransactionCategory>
)

@Serializable
data class DefaultTransactionCategory(
    val id: String,
    val title: String,
    val type: String,
    val emoji: String? = null,
    val parentId: String? = null
)

@Serializable
data class DefaultAssetCategorySet(
    val version: Int,
    val categories: List<DefaultAssetCategory>
)

@Serializable
data class DefaultAssetCategory(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String = "",
    val color: Long = 0xFF3562F6,
    val sortOrder: Int = 0,
    val isLiability: Boolean = false
)
