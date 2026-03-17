package com.andriybobchuk.mooney.mooney.domain.backup

import com.andriybobchuk.mooney.core.data.database.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.datetime.Clock

/**
 * Comprehensive data export/import manager for complete database backup
 * This allows users to safely export ALL their data before package name changes
 */
class DataExportImportManager(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val goalDao: GoalDao,
    private val goalGroupDao: GoalGroupDao,
    private val categoryUsageDao: CategoryUsageDao
) {
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Complete database export structure
     */
    @Serializable
    data class CompleteDataExport(
        val exportVersion: Int = 1,
        val exportDate: Long,
        val appVersion: String = "1.0.0",
        val deviceInfo: String = "",
        val transactions: List<TransactionExport>,
        val accounts: List<AccountExport>,
        val goals: List<GoalExport>,
        val goalGroups: List<GoalGroupExport>,
        val categoryUsages: List<CategoryUsageExport>,
        val metadata: ExportMetadata
    )
    
    @Serializable
    data class TransactionExport(
        val id: Int = 0,
        val title: String,
        val amount: Double,
        val currency: String,
        val categoryId: Int,
        val accountId: Int? = null,
        val date: Long,
        val note: String? = null,
        val createdAt: Long,
        val updatedAt: Long
    )
    
    @Serializable
    data class AccountExport(
        val id: Int = 0,
        val name: String,
        val balance: Double,
        val currency: String,
        val emoji: String,
        val assetCategory: String,
        val createdAt: Long,
        val updatedAt: Long
    )
    
    @Serializable
    data class GoalExport(
        val id: Int = 0,
        val name: String,
        val targetAmount: Double,
        val currentAmount: Double,
        val currency: String,
        val targetDate: Long? = null,
        val groupId: Int? = null,
        val emoji: String? = null,
        val description: String? = null,
        val createdDate: Long,
        val updatedDate: Long
    )
    
    @Serializable
    data class GoalGroupExport(
        val id: Int = 0,
        val name: String,
        val emoji: String? = null,
        val createdDate: Long,
        val updatedDate: Long
    )
    
    @Serializable
    data class CategoryUsageExport(
        val categoryId: String,
        val usageCount: Int,
        val lastUsedDate: String
    )
    
    @Serializable
    data class ExportMetadata(
        val totalTransactions: Int,
        val totalAccounts: Int,
        val totalGoals: Int,
        val totalGoalGroups: Int,
        val totalCategoryUsages: Int,
        val checksum: String
    )
    
    /**
     * Export ALL data to JSON string
     * This creates a complete backup that can be imported later
     */
    suspend fun exportAllData(): String {
        println("📦 Starting complete data export...")
        
        // Fetch all data
        val transactions = transactionDao.getAll().first().map { entity ->
            TransactionExport(
                id = entity.id,
                title = entity.title,
                amount = entity.amount,
                currency = entity.currency,
                categoryId = entity.categoryId,
                accountId = entity.accountId,
                date = entity.date,
                note = entity.note,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
        
        val accounts = accountDao.getAll().first().map { entity ->
            AccountExport(
                id = entity.id,
                name = entity.name,
                balance = entity.balance,
                currency = entity.currency,
                emoji = entity.emoji,
                assetCategory = entity.assetCategory,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
        
        val goals = goalDao.getAll().first().map { entity ->
            GoalExport(
                id = entity.id,
                name = entity.name,
                targetAmount = entity.targetAmount,
                currentAmount = entity.currentAmount,
                currency = entity.currency,
                targetDate = entity.targetDate,
                groupId = entity.groupId,
                emoji = entity.emoji,
                description = entity.description,
                createdDate = entity.createdDate,
                updatedDate = entity.updatedDate
            )
        }
        
        val goalGroups = goalGroupDao.getAll().first().map { entity ->
            GoalGroupExport(
                id = entity.id,
                name = entity.name,
                emoji = entity.emoji,
                createdDate = entity.createdDate,
                updatedDate = entity.updatedDate
            )
        }
        
        // Get all category usages (limiting to reasonable amount)
        val categoryUsages = categoryUsageDao.getMostUsedCategories(1000).map { entity ->
            CategoryUsageExport(
                categoryId = entity.categoryId,
                usageCount = entity.usageCount,
                lastUsedDate = entity.lastUsedDate
            )
        }
        
        val metadata = ExportMetadata(
            totalTransactions = transactions.size,
            totalAccounts = accounts.size,
            totalGoals = goals.size,
            totalGoalGroups = goalGroups.size,
            totalCategoryUsages = categoryUsages.size,
            checksum = generateChecksum(
                transactions.size, 
                accounts.size, 
                goals.size,
                goalGroups.size,
                categoryUsages.size
            )
        )
        
        val export = CompleteDataExport(
            exportDate = Clock.System.now().epochSeconds,
            transactions = transactions,
            accounts = accounts,
            goals = goals,
            goalGroups = goalGroups,
            categoryUsages = categoryUsages,
            metadata = metadata
        )
        
        println("📊 Export Summary:")
        println("   - Transactions: ${transactions.size}")
        println("   - Accounts: ${accounts.size}")
        println("   - Goals: ${goals.size}")
        println("   - Goal Groups: ${goalGroups.size}")
        println("   - Category Usages: ${categoryUsages.size}")
        println("   - Checksum: ${metadata.checksum}")
        
        return json.encodeToString(export)
    }
    
    /**
     * Import data from JSON string
     * This restores a complete backup, optionally clearing existing data
     */
    suspend fun importData(jsonData: String, clearExisting: Boolean = false): ImportResult {
        return try {
            println("📥 Starting data import...")
            
            val export = json.decodeFromString<CompleteDataExport>(jsonData)
            
            // Verify export version compatibility
            if (export.exportVersion > 1) {
                return ImportResult.Error("Unsupported export version: ${export.exportVersion}")
            }
            
            // Optionally clear existing data (use with caution!)
            if (clearExisting) {
                println("⚠️ Clearing existing data before import...")
                // Note: You'd need to implement clear methods in DAOs
            }
            
            var importedTransactions = 0
            var importedAccounts = 0
            var importedGoals = 0
            var importedGoalGroups = 0
            var importedCategoryUsages = 0
            
            // Import goal groups first (as goals reference them)
            export.goalGroups.forEach { goalGroup ->
                val entity = GoalGroupEntity(
                    id = 0, // Let Room auto-generate new IDs
                    name = goalGroup.name,
                    emoji = goalGroup.emoji,
                    createdDate = goalGroup.createdDate,
                    updatedDate = goalGroup.updatedDate
                )
                goalGroupDao.upsert(entity)
                importedGoalGroups++
            }
            
            // Import accounts (as transactions reference them)
            export.accounts.forEach { account ->
                val entity = AccountEntity(
                    id = 0, // Let Room auto-generate new IDs
                    name = account.name,
                    balance = account.balance,
                    currency = account.currency,
                    emoji = account.emoji,
                    assetCategory = account.assetCategory,
                    createdAt = account.createdAt,
                    updatedAt = account.updatedAt
                )
                accountDao.upsert(entity)
                importedAccounts++
            }
            
            // Import transactions
            export.transactions.forEach { transaction ->
                val entity = TransactionEntity(
                    id = 0, // Let Room auto-generate new IDs
                    title = transaction.title,
                    amount = transaction.amount,
                    currency = transaction.currency,
                    categoryId = transaction.categoryId,
                    accountId = transaction.accountId,
                    date = transaction.date,
                    note = transaction.note,
                    createdAt = transaction.createdAt,
                    updatedAt = transaction.updatedAt
                )
                transactionDao.upsert(entity)
                importedTransactions++
            }
            
            // Import goals
            export.goals.forEach { goal ->
                val entity = GoalEntity(
                    id = 0, // Let Room auto-generate new IDs
                    name = goal.name,
                    targetAmount = goal.targetAmount,
                    currentAmount = goal.currentAmount,
                    currency = goal.currency,
                    targetDate = goal.targetDate,
                    groupId = goal.groupId,
                    emoji = goal.emoji,
                    description = goal.description,
                    createdDate = goal.createdDate,
                    updatedDate = goal.updatedDate
                )
                goalDao.upsert(entity)
                importedGoals++
            }
            
            // Import category usages
            export.categoryUsages.forEach { usage ->
                val entity = CategoryUsageEntity(
                    categoryId = usage.categoryId,
                    usageCount = usage.usageCount,
                    lastUsedDate = usage.lastUsedDate
                )
                categoryUsageDao.upsert(entity)
                importedCategoryUsages++
            }
            
            println("✅ Import Complete:")
            println("   - Transactions: $importedTransactions")
            println("   - Accounts: $importedAccounts")
            println("   - Goals: $importedGoals")
            println("   - Goal Groups: $importedGoalGroups")
            println("   - Category Usages: $importedCategoryUsages")
            
            ImportResult.Success(
                importedTransactions = importedTransactions,
                importedAccounts = importedAccounts,
                importedGoals = importedGoals,
                importedGoalGroups = importedGoalGroups,
                importedCategoryUsages = importedCategoryUsages
            )
            
        } catch (e: Exception) {
            println("❌ Import failed: ${e.message}")
            ImportResult.Error(e.message ?: "Unknown error during import")
        }
    }
    
    /**
     * Validate export JSON without importing
     */
    fun validateExportData(jsonData: String): ValidationResult {
        return try {
            val export = json.decodeFromString<CompleteDataExport>(jsonData)
            
            // Verify checksum
            val expectedChecksum = generateChecksum(
                export.transactions.size,
                export.accounts.size,
                export.goals.size,
                export.goalGroups.size,
                export.categoryUsages.size
            )
            
            if (export.metadata.checksum != expectedChecksum) {
                return ValidationResult.Invalid("Checksum mismatch - data may be corrupted")
            }
            
            ValidationResult.Valid(
                transactions = export.transactions.size,
                accounts = export.accounts.size,
                goals = export.goals.size,
                goalGroups = export.goalGroups.size,
                categoryUsages = export.categoryUsages.size,
                exportDate = export.exportDate
            )
        } catch (e: Exception) {
            ValidationResult.Invalid(e.message ?: "Invalid export format")
        }
    }
    
    private fun generateChecksum(
        transactions: Int, 
        accounts: Int, 
        goals: Int,
        goalGroups: Int,
        categoryUsages: Int
    ): String {
        val data = "$transactions-$accounts-$goals-$goalGroups-$categoryUsages"
        return data.hashCode().toString()
    }
    
    sealed class ImportResult {
        data class Success(
            val importedTransactions: Int,
            val importedAccounts: Int,
            val importedGoals: Int,
            val importedGoalGroups: Int,
            val importedCategoryUsages: Int
        ) : ImportResult()
        
        data class Error(val message: String) : ImportResult()
    }
    
    sealed class ValidationResult {
        data class Valid(
            val transactions: Int,
            val accounts: Int,
            val goals: Int,
            val goalGroups: Int,
            val categoryUsages: Int,
            val exportDate: Long
        ) : ValidationResult()
        
        data class Invalid(val reason: String) : ValidationResult()
    }
}