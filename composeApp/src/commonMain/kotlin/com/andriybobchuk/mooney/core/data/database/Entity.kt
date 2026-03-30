package com.andriybobchuk.mooney.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val currency: String,
    val emoji: String,
    val assetCategory: String = "BANK_ACCOUNT",
    val isPrimary: Boolean = false,
    val isLiability: Boolean = false
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val emoji: String? = null,
    val parentId: String? = null
)

@Entity(tableName = "user_currencies")
data class UserCurrencyEntity(
    @PrimaryKey val code: String,
    val sortOrder: Int
)

@Entity
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subcategoryId: String,
    val amount: Double,
    val accountId: Int,
    val date: String
)

@Entity(tableName = "category_usage")
data class CategoryUsageEntity(
    @PrimaryKey val categoryId: String, // The category/subcategory ID
    val usageCount: Int, // Number of times this category was used
    val lastUsedDate: String // ISO date string of last usage
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val emoji: String,
    val title: String,
    val description: String,
    val targetAmount: Double,
    val currency: String,
    val createdDate: String, // ISO date string
    val groupName: String = "General",
    val imagePath: String? = null,
    val trackingType: String = "NET_WORTH",  // "ACCOUNT", "NET_WORTH", "TOTAL_ASSETS"
    val accountId: Int? = null               // only used when trackingType = "ACCOUNT"
)

@Entity(tableName = "goal_groups")
data class GoalGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val emoji: String,
    val color: String,
    val createdDate: String // ISO date string
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subcategoryId: String,
    val amount: Double,
    val accountId: Int,
    val dayOfMonth: Int, // 1-31
    val frequency: String, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val weekDay: Int? = null, // 0-6 for weekly, null otherwise
    val monthOfYear: Int? = null, // 1-12 for yearly, null otherwise
    val isActive: Boolean = true,
    val createdDate: String, // ISO date string
    val lastProcessedDate: String? = null // Track last time it was processed
)

@Entity(tableName = "pending_transactions")
data class PendingTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val recurringTransactionId: Int,
    val subcategoryId: String,
    val amount: Double,
    val accountId: Int,
    val scheduledDate: String, // ISO date string - when it should be added
    val status: String = "PENDING", // "PENDING", "ACCEPTED", "REJECTED", "SKIPPED"
    val createdDate: String // ISO date string - when the pending entry was created
)

@Entity(tableName = "asset_categories")
data class AssetCategoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val emoji: String,
    val description: String = "",
    val color: Long = 0xFF3562F6,
    val sortOrder: Int = 0,
    val isLiability: Boolean = false
)
