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
    val assetCategory: String = "BANK_ACCOUNT"
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
    val imagePath: String? = null
)

@Entity(tableName = "goal_groups")
data class GoalGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val emoji: String,
    val color: String,
    val createdDate: String // ISO date string
)

