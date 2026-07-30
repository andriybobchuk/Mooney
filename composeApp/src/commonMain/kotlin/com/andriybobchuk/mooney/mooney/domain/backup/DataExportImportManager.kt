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
    private val pendingTransactionDao: PendingTransactionDao,
    private val assetCategoryDao: AssetCategoryDao
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
        val assetCategories: List<AssetCategoryExport> = emptyList(),
        val metadata: ExportMetadata
    )

    @Serializable
    data class TransactionExport(
        val id: Int = 0,
        val subcategoryId: String,
        val amount: Double,
        val accountId: Int,
        val date: String,
        val destinationAmount: Double? = null,
        val description: String? = null
    )

    @Serializable
    data class AccountExport(
        val id: Int = 0,
        val title: String,
        val amount: Double,
        val currency: String,
        val emoji: String,
        val assetCategory: String,
        val isPrimary: Boolean = false,
        val isLiability: Boolean = false,
        // v4 additions — previously lost across export/import round-trips.
        // Every field has a safe default so restoring a v1-v3 export still
        // works (missing fields decode as their defaults).
        val currentMarketValue: Double? = null,
        val includeInNetWorth: Boolean = true,
        val isPrimaryForExpenses: Boolean = false,
        val isPrimaryForIncome: Boolean = false
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
        val imagePath: String? = null,
        // v4 additions — GoalEntity tracks these to compute progress against
        // account balance vs. net worth vs. gross assets. Missing them
        // silently downgraded every restored goal to a NET_WORTH goal.
        val trackingType: String = "NET_WORTH",
        val accountId: Int? = null
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
        val parentId: String? = null,
        // v4 addition — user-set monthly budget per category. Missing this
        // wiped every budget on restore, which silently broke the whole
        // Analytics → Budget-progress flow after import.
        val monthlyLimit: Double? = null
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
        val lastProcessedDate: String? = null,
        // v5 addition — carry the note attached to the recurring template
        // so restore preserves user context ("Landlord — May rent").
        val description: String? = null
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
        val createdDate: String,
        // v5 addition — same reason as RecurringTransactionExport.
        val description: String? = null
    )

    @Serializable
    data class AssetCategoryExport(
        val id: String,
        val title: String,
        val emoji: String,
        val description: String = "",
        val color: Long = 0xFF3562F6,
        val sortOrder: Int = 0,
        val isLiability: Boolean = false
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
        val totalAssetCategories: Int = 0,
        val checksum: String
    )

    // endregion

    // Dispatchers.Default so the 10 DAO reads + JSON encoding never touch
    // Main — on iPhones with hundreds of transactions the serialization step
    // alone was jank-freezing the button for 500-1500ms. Room's own reads
    // already dispatch internally, but json.encodeToString does not.
    suspend fun exportAllData(): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        val transactions = transactionDao.getAll().first().map { it.toExport() }
        val accounts = accountDao.getAll().first().map { it.toExport() }
        val goals = goalDao.getAll().first().map { it.toExport() }
        val goalGroups = goalGroupDao.getAll().first().map { it.toExport() }
        val categoryUsages = categoryUsageDao.getMostUsedCategories(10000).map { it.toExport() }
        val categories = categoryDao.getAll().first().map { it.toExport() }
        val userCurrencies = userCurrencyDao.getAll().first().map { it.toExport() }
        val recurringTransactions = recurringTransactionDao.getAll().first().map { it.toExport() }
        val pendingTransactions = pendingTransactionDao.getAll().first().map { it.toExport() }
        val assetCategories = assetCategoryDao.getAll().first().map { it.toExport() }

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
            totalAssetCategories = assetCategories.size,
            checksum = generateChecksum(
                transactions.size, accounts.size, goals.size,
                goalGroups.size, categoryUsages.size, categories.size,
                userCurrencies.size, recurringTransactions.size, pendingTransactions.size,
                assetCategories.size
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
            assetCategories = assetCategories,
            metadata = metadata
        )

        json.encodeToString(export)
    }

    // Suppressed because the length is inherently structural — 10 sequential
    // "insert entity type N" steps, each with explicit constructor mapping
    // so anyone reading this can grep for a single field and confirm it
    // survives round-trip. Extracting per-step helpers would hide exactly
    // the trace we want to keep visible.
    @Suppress("LongMethod")
    suspend fun importData(jsonData: String, clearExisting: Boolean = false): ImportResult {
        return try {
            val export = json.decodeFromString<CompleteDataExport>(jsonData)

            if (export.exportVersion > CURRENT_EXPORT_VERSION) {
                return ImportResult.Error("Unsupported export version: ${export.exportVersion}. Update the app.")
            }

            // Import order matters: parents before children
            var counts = ImportCounts()

            // 1. Categories first (no dependencies).
            //    monthlyLimit was added in export v4 — CategoryExport defaults
            //    it to null for older payloads so restoring a v1-v3 backup
            //    keeps the existing behavior (no budget).
            export.categories.forEach { cat ->
                categoryDao.upsert(
                    CategoryEntity(
                        id = cat.id,
                        title = cat.title,
                        type = cat.type,
                        emoji = cat.emoji,
                        parentId = cat.parentId,
                        monthlyLimit = cat.monthlyLimit
                    )
                )
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

            // 4. Accounts (transactions reference them).
            //    v4 fields (currentMarketValue, includeInNetWorth,
            //    isPrimaryForExpenses, isPrimaryForIncome) all have safe
            //    defaults so restoring older exports still keeps everything
            //    counted / no primary-role assignments.
            export.accounts.forEach { acc ->
                accountDao.upsert(
                    AccountEntity(
                        id = 0,
                        title = acc.title,
                        amount = acc.amount,
                        currency = acc.currency,
                        emoji = acc.emoji,
                        assetCategory = acc.assetCategory,
                        isPrimary = acc.isPrimary,
                        isLiability = acc.isLiability,
                        currentMarketValue = acc.currentMarketValue,
                        includeInNetWorth = acc.includeInNetWorth,
                        isPrimaryForExpenses = acc.isPrimaryForExpenses,
                        isPrimaryForIncome = acc.isPrimaryForIncome
                    )
                )
                counts = counts.copy(accounts = counts.accounts + 1)
            }

            // 5. Transactions
            export.transactions.forEach { tx ->
                transactionDao.upsert(TransactionEntity(0, tx.subcategoryId, tx.amount, tx.accountId, tx.date, tx.destinationAmount, tx.description))
                counts = counts.copy(transactions = counts.transactions + 1)
            }

            // 6. Goals — v4 restored `trackingType` + `accountId`. Missing
            //    them silently downgraded every restored goal to NET_WORTH,
            //    which produced wrong progress bars for ACCOUNT-tracked
            //    goals (e.g. "save $5k in checking").
            export.goals.forEach { goal ->
                goalDao.upsert(
                    GoalEntity(
                        id = 0,
                        emoji = goal.emoji,
                        title = goal.title,
                        description = goal.description,
                        targetAmount = goal.targetAmount,
                        currency = goal.currency,
                        createdDate = goal.createdDate,
                        groupName = goal.groupName,
                        imagePath = goal.imagePath,
                        trackingType = goal.trackingType,
                        accountId = goal.accountId
                    )
                )
                counts = counts.copy(goals = counts.goals + 1)
            }

            // 7. Category usages (upsert by PK = categoryId)
            export.categoryUsages.forEach { cu ->
                categoryUsageDao.upsert(CategoryUsageEntity(cu.categoryId, cu.usageCount, cu.lastUsedDate))
                counts = counts.copy(categoryUsages = counts.categoryUsages + 1)
            }

            // 8. Recurring transactions — v5 restored `description` so the
            //    note user typed at recurring-creation time survives export.
            export.recurringTransactions.forEach { rt ->
                recurringTransactionDao.upsert(
                    RecurringTransactionEntity(
                        id = 0,
                        title = rt.title,
                        subcategoryId = rt.subcategoryId,
                        amount = rt.amount,
                        accountId = rt.accountId,
                        dayOfMonth = rt.dayOfMonth,
                        frequency = rt.frequency,
                        weekDay = rt.weekDay,
                        monthOfYear = rt.monthOfYear,
                        isActive = rt.isActive,
                        createdDate = rt.createdDate,
                        lastProcessedDate = rt.lastProcessedDate,
                        description = rt.description
                    )
                )
                counts = counts.copy(recurringTransactions = counts.recurringTransactions + 1)
            }

            // 9. Pending transactions — same v5 rationale as recurring.
            export.pendingTransactions.forEach { pt ->
                pendingTransactionDao.upsert(
                    PendingTransactionEntity(
                        id = 0,
                        recurringTransactionId = pt.recurringTransactionId,
                        subcategoryId = pt.subcategoryId,
                        amount = pt.amount,
                        accountId = pt.accountId,
                        scheduledDate = pt.scheduledDate,
                        status = pt.status,
                        createdDate = pt.createdDate,
                        description = pt.description
                    )
                )
                counts = counts.copy(pendingTransactions = counts.pendingTransactions + 1)
            }

            // 10. Asset categories
            export.assetCategories.forEach { ac ->
                assetCategoryDao.upsert(
                    AssetCategoryEntity(ac.id, ac.title, ac.emoji, ac.description, ac.color, ac.sortOrder, ac.isLiability)
                )
                counts = counts.copy(assetCategories = counts.assetCategories + 1)
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
                importedPendingTransactions = counts.pendingTransactions,
                importedAssetCategories = counts.assetCategories
            )
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "Unknown error during import")
        }
    }

    fun validateExportData(jsonData: String): ValidationResult {
        return try {
            val export = json.decodeFromString<CompleteDataExport>(jsonData)

            // Validate with checksum format matching export version
            val expectedChecksum = when (export.exportVersion) {
                1 -> generateChecksumV1(
                    export.transactions.size, export.accounts.size,
                    export.goals.size, export.goalGroups.size, export.categoryUsages.size
                )
                2 -> generateChecksum(
                    export.transactions.size, export.accounts.size,
                    export.goals.size, export.goalGroups.size, export.categoryUsages.size,
                    export.categories.size, export.userCurrencies.size,
                    export.recurringTransactions.size, export.pendingTransactions.size
                )
                else -> generateChecksum(
                    export.transactions.size, export.accounts.size,
                    export.goals.size, export.goalGroups.size, export.categoryUsages.size,
                    export.categories.size, export.userCurrencies.size,
                    export.recurringTransactions.size, export.pendingTransactions.size,
                    export.assetCategories.size
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

    private fun TransactionEntity.toExport() = TransactionExport(id, subcategoryId, amount, accountId, date, destinationAmount, description)

    private fun AccountEntity.toExport() = AccountExport(
        id = id,
        title = title,
        amount = amount,
        currency = currency,
        emoji = emoji,
        assetCategory = assetCategory,
        isPrimary = isPrimary,
        isLiability = isLiability,
        // v4 fields — see AccountExport docs for why each matters.
        currentMarketValue = currentMarketValue,
        includeInNetWorth = includeInNetWorth,
        isPrimaryForExpenses = isPrimaryForExpenses,
        isPrimaryForIncome = isPrimaryForIncome
    )

    private fun GoalEntity.toExport() = GoalExport(
        id = id,
        emoji = emoji,
        title = title,
        description = description,
        targetAmount = targetAmount,
        currency = currency,
        createdDate = createdDate,
        groupName = groupName,
        imagePath = imagePath,
        trackingType = trackingType,
        accountId = accountId
    )

    private fun GoalGroupEntity.toExport() = GoalGroupExport(id, name, emoji, color, createdDate)

    private fun CategoryUsageEntity.toExport() = CategoryUsageExport(categoryId, usageCount, lastUsedDate)

    private fun CategoryEntity.toExport() = CategoryExport(
        id = id,
        title = title,
        type = type,
        emoji = emoji,
        parentId = parentId,
        monthlyLimit = monthlyLimit
    )

    private fun UserCurrencyEntity.toExport() = UserCurrencyExport(code, sortOrder)

    private fun RecurringTransactionEntity.toExport() = RecurringTransactionExport(
        id = id,
        title = title,
        subcategoryId = subcategoryId,
        amount = amount,
        accountId = accountId,
        dayOfMonth = dayOfMonth,
        frequency = frequency,
        weekDay = weekDay,
        monthOfYear = monthOfYear,
        isActive = isActive,
        createdDate = createdDate,
        lastProcessedDate = lastProcessedDate,
        description = description
    )

    private fun PendingTransactionEntity.toExport() = PendingTransactionExport(
        id = id,
        recurringTransactionId = recurringTransactionId,
        subcategoryId = subcategoryId,
        amount = amount,
        accountId = accountId,
        scheduledDate = scheduledDate,
        status = status,
        createdDate = createdDate,
        description = description
    )

    private fun AssetCategoryEntity.toExport() = AssetCategoryExport(
        id, title, emoji, description, color, sortOrder, isLiability
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
        val pendingTransactions: Int = 0,
        val assetCategories: Int = 0
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
            val importedPendingTransactions: Int = 0,
            val importedAssetCategories: Int = 0
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
        // Bump when adding new fields to any *Export data class OR when
        // changing the wire-format of an existing field. The importer +
        // checksum switch below keep older exports readable — but a mismatch
        // between the exporter and any consumer with a NEWER version-cap
        // (e.g. a beta test rejects a stable-app backup) surfaces the
        // "Unsupported export version" error to the user, which is the
        // correct behavior.
        const val CURRENT_EXPORT_VERSION = 5
    }
}
