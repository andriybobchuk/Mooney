package com.andriybobchuk.mooney.mooney.domain.backup

import com.andriybobchuk.mooney.core.data.database.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.datetime.Clock

@Suppress("TooManyFunctions")
class DataExportImportManager(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val goalDao: GoalDao,
    private val goalGroupDao: GoalGroupDao,
    private val categoryUsageDao: CategoryUsageDao,
    private val categoryDao: CategoryDao,
    private val userCurrencyDao: UserCurrencyDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val pendingTransactionDao: PendingTransactionDao
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // region Export models

    @Serializable
    data class CompleteDataExport(
        val exportVersion: Int = CURRENT_EXPORT_VERSION,
        val exportDate: Long,
        val appVersion: String = "1.0.0",
        val transactions: List<TransactionExport>,
        val accounts: List<AccountExport>,
        val goals: List<GoalExport>,
        val goalGroups: List<GoalGroupExport>,
        val categoryUsages: List<CategoryUsageExport>,
        val categories: List<CategoryExport> = emptyList(),
        val userCurrencies: List<UserCurrencyExport> = emptyList(),
        val recurringTransactions: List<RecurringTransactionExport> = emptyList(),
        val pendingTransactions: List<PendingTransactionExport> = emptyList(),
        val metadata: ExportMetadata
    )

    @Serializable
    data class TransactionExport(
        val id: Int = 0,
        val subcategoryId: String,
        val amount: Double,
        val accountId: Int,
        val date: String
    )

    @Serializable
    data class AccountExport(
        val id: Int = 0,
        val title: String,
        val amount: Double,
        val currency: String,
        val emoji: String,
        val assetCategory: String,
        val isPrimary: Boolean = false
    )

    @Serializable
    data class GoalExport(
        val id: Int = 0,
        val emoji: String,
        val title: String,
        val description: String,
        val targetAmount: Double,
        val currency: String,
        val createdDate: String,
        val groupName: String,
        val imagePath: String? = null
    )

    @Serializable
    data class GoalGroupExport(
        val id: Int = 0,
        val name: String,
        val emoji: String,
        val color: String,
        val createdDate: String
    )

    @Serializable
    data class CategoryUsageExport(
        val categoryId: String,
        val usageCount: Int,
        val lastUsedDate: String
    )

    @Serializable
    data class CategoryExport(
        val id: String,
        val title: String,
        val type: String,
        val emoji: String? = null,
        val parentId: String? = null
    )

    @Serializable
    data class UserCurrencyExport(
        val code: String,
        val sortOrder: Int
    )

    @Serializable
    data class RecurringTransactionExport(
        val id: Int = 0,
        val title: String,
        val subcategoryId: String,
        val amount: Double,
        val accountId: Int,
        val dayOfMonth: Int,
        val frequency: String,
        val weekDay: Int? = null,
        val monthOfYear: Int? = null,
        val isActive: Boolean = true,
        val createdDate: String,
        val lastProcessedDate: String? = null
    )

    @Serializable
    data class PendingTransactionExport(
        val id: Int = 0,
        val recurringTransactionId: Int,
        val subcategoryId: String,
        val amount: Double,
        val accountId: Int,
        val scheduledDate: String,
        val status: String = "PENDING",
        val createdDate: String
    )

    @Serializable
    data class ExportMetadata(
        val totalTransactions: Int,
        val totalAccounts: Int,
        val totalGoals: Int,
        val totalGoalGroups: Int,
        val totalCategoryUsages: Int,
        val totalCategories: Int = 0,
        val totalUserCurrencies: Int = 0,
        val totalRecurringTransactions: Int = 0,
        val totalPendingTransactions: Int = 0,
        val checksum: String
    )

    // endregion

    suspend fun exportAllData(): String {
        val transactions = transactionDao.getAll().first().map { it.toExport() }
        val accounts = accountDao.getAll().first().map { it.toExport() }
        val goals = goalDao.getAll().first().map { it.toExport() }
        val goalGroups = goalGroupDao.getAll().first().map { it.toExport() }
        val categoryUsages = categoryUsageDao.getMostUsedCategories(10000).map { it.toExport() }
        val categories = categoryDao.getAll().first().map { it.toExport() }
        val userCurrencies = userCurrencyDao.getAll().first().map { it.toExport() }
        val recurringTransactions = recurringTransactionDao.getAll().first().map { it.toExport() }
        val pendingTransactions = pendingTransactionDao.getAll().first().map { it.toExport() }

        val metadata = ExportMetadata(
            totalTransactions = transactions.size,
            totalAccounts = accounts.size,
            totalGoals = goals.size,
            totalGoalGroups = goalGroups.size,
            totalCategoryUsages = categoryUsages.size,
            totalCategories = categories.size,
            totalUserCurrencies = userCurrencies.size,
            totalRecurringTransactions = recurringTransactions.size,
            totalPendingTransactions = pendingTransactions.size,
            checksum = generateChecksum(
                transactions.size, accounts.size, goals.size,
                goalGroups.size, categoryUsages.size, categories.size,
                userCurrencies.size, recurringTransactions.size, pendingTransactions.size
            )
        )

        val export = CompleteDataExport(
            exportDate = Clock.System.now().epochSeconds,
            transactions = transactions,
            accounts = accounts,
            goals = goals,
            goalGroups = goalGroups,
            categoryUsages = categoryUsages,
            categories = categories,
            userCurrencies = userCurrencies,
            recurringTransactions = recurringTransactions,
            pendingTransactions = pendingTransactions,
            metadata = metadata
        )

        return json.encodeToString(export)
    }

    suspend fun importData(jsonData: String, clearExisting: Boolean = false): ImportResult {
        return try {
            val export = json.decodeFromString<CompleteDataExport>(jsonData)

            if (export.exportVersion > CURRENT_EXPORT_VERSION) {
                return ImportResult.Error("Unsupported export version: ${export.exportVersion}. Update the app.")
            }

            // Import order matters: parents before children
            var counts = ImportCounts()

            // 1. Categories first (no dependencies)
            export.categories.forEach { cat ->
                categoryDao.upsert(CategoryEntity(cat.id, cat.title, cat.type, cat.emoji, cat.parentId))
                counts = counts.copy(categories = counts.categories + 1)
            }

            // 2. User currencies (no dependencies)
            export.userCurrencies.forEach { uc ->
                userCurrencyDao.upsert(UserCurrencyEntity(uc.code, uc.sortOrder))
                counts = counts.copy(userCurrencies = counts.userCurrencies + 1)
            }

            // 3. Goal groups (goals reference them by name)
            export.goalGroups.forEach { gg ->
                goalGroupDao.upsert(GoalGroupEntity(0, gg.name, gg.emoji, gg.color, gg.createdDate))
                counts = counts.copy(goalGroups = counts.goalGroups + 1)
            }

            // 4. Accounts (transactions reference them)
            export.accounts.forEach { acc ->
                accountDao.upsert(
                    AccountEntity(0, acc.title, acc.amount, acc.currency, acc.emoji, acc.assetCategory, acc.isPrimary)
                )
                counts = counts.copy(accounts = counts.accounts + 1)
            }

            // 5. Transactions
            export.transactions.forEach { tx ->
                transactionDao.upsert(TransactionEntity(0, tx.subcategoryId, tx.amount, tx.accountId, tx.date))
                counts = counts.copy(transactions = counts.transactions + 1)
            }

            // 6. Goals
            export.goals.forEach { goal ->
                goalDao.upsert(
                    GoalEntity(0, goal.emoji, goal.title, goal.description, goal.targetAmount,
                        goal.currency, goal.createdDate, goal.groupName, goal.imagePath)
                )
                counts = counts.copy(goals = counts.goals + 1)
            }

            // 7. Category usages (upsert by PK = categoryId)
            export.categoryUsages.forEach { cu ->
                categoryUsageDao.upsert(CategoryUsageEntity(cu.categoryId, cu.usageCount, cu.lastUsedDate))
                counts = counts.copy(categoryUsages = counts.categoryUsages + 1)
            }

            // 8. Recurring transactions
            export.recurringTransactions.forEach { rt ->
                recurringTransactionDao.upsert(
                    RecurringTransactionEntity(0, rt.title, rt.subcategoryId, rt.amount, rt.accountId,
                        rt.dayOfMonth, rt.frequency, rt.weekDay, rt.monthOfYear, rt.isActive,
                        rt.createdDate, rt.lastProcessedDate)
                )
                counts = counts.copy(recurringTransactions = counts.recurringTransactions + 1)
            }

            // 9. Pending transactions
            export.pendingTransactions.forEach { pt ->
                pendingTransactionDao.upsert(
                    PendingTransactionEntity(0, pt.recurringTransactionId, pt.subcategoryId,
                        pt.amount, pt.accountId, pt.scheduledDate, pt.status, pt.createdDate)
                )
                counts = counts.copy(pendingTransactions = counts.pendingTransactions + 1)
            }

            ImportResult.Success(
                importedTransactions = counts.transactions,
                importedAccounts = counts.accounts,
                importedGoals = counts.goals,
                importedGoalGroups = counts.goalGroups,
                importedCategoryUsages = counts.categoryUsages,
                importedCategories = counts.categories,
                importedUserCurrencies = counts.userCurrencies,
                importedRecurringTransactions = counts.recurringTransactions,
                importedPendingTransactions = counts.pendingTransactions
            )
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "Unknown error during import")
        }
    }

    fun validateExportData(jsonData: String): ValidationResult {
        return try {
            val export = json.decodeFromString<CompleteDataExport>(jsonData)

            // For v1 exports, validate with old checksum format
            val expectedChecksum = if (export.exportVersion == 1) {
                generateChecksumV1(
                    export.transactions.size, export.accounts.size,
                    export.goals.size, export.goalGroups.size, export.categoryUsages.size
                )
            } else {
                generateChecksum(
                    export.transactions.size, export.accounts.size,
                    export.goals.size, export.goalGroups.size, export.categoryUsages.size,
                    export.categories.size, export.userCurrencies.size,
                    export.recurringTransactions.size, export.pendingTransactions.size
                )
            }

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

    private fun generateChecksum(vararg counts: Int): String {
        val data = counts.joinToString("-")
        return data.hashCode().toString()
    }

    /** Backward-compatible checksum for v1 exports */
    private fun generateChecksumV1(
        transactions: Int, accounts: Int, goals: Int, goalGroups: Int, categoryUsages: Int
    ): String {
        val data = "$transactions-$accounts-$goals-$goalGroups-$categoryUsages"
        return data.hashCode().toString()
    }

    // region Entity to Export mappers

    private fun TransactionEntity.toExport() = TransactionExport(id, subcategoryId, amount, accountId, date)

    private fun AccountEntity.toExport() = AccountExport(id, title, amount, currency, emoji, assetCategory, isPrimary)

    private fun GoalEntity.toExport() = GoalExport(id, emoji, title, description, targetAmount, currency, createdDate, groupName, imagePath)

    private fun GoalGroupEntity.toExport() = GoalGroupExport(id, name, emoji, color, createdDate)

    private fun CategoryUsageEntity.toExport() = CategoryUsageExport(categoryId, usageCount, lastUsedDate)

    private fun CategoryEntity.toExport() = CategoryExport(id, title, type, emoji, parentId)

    private fun UserCurrencyEntity.toExport() = UserCurrencyExport(code, sortOrder)

    private fun RecurringTransactionEntity.toExport() = RecurringTransactionExport(
        id, title, subcategoryId, amount, accountId, dayOfMonth, frequency,
        weekDay, monthOfYear, isActive, createdDate, lastProcessedDate
    )

    private fun PendingTransactionEntity.toExport() = PendingTransactionExport(
        id, recurringTransactionId, subcategoryId, amount, accountId, scheduledDate, status, createdDate
    )

    // endregion

    private data class ImportCounts(
        val transactions: Int = 0,
        val accounts: Int = 0,
        val goals: Int = 0,
        val goalGroups: Int = 0,
        val categoryUsages: Int = 0,
        val categories: Int = 0,
        val userCurrencies: Int = 0,
        val recurringTransactions: Int = 0,
        val pendingTransactions: Int = 0
    )

    sealed class ImportResult {
        data class Success(
            val importedTransactions: Int,
            val importedAccounts: Int,
            val importedGoals: Int,
            val importedGoalGroups: Int,
            val importedCategoryUsages: Int,
            val importedCategories: Int = 0,
            val importedUserCurrencies: Int = 0,
            val importedRecurringTransactions: Int = 0,
            val importedPendingTransactions: Int = 0
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

    companion object {
        const val CURRENT_EXPORT_VERSION = 2
    }
}
