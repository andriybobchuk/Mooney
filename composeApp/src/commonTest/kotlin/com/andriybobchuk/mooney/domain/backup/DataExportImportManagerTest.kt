package com.andriybobchuk.mooney.domain.backup

import com.andriybobchuk.mooney.core.data.database.*
import com.andriybobchuk.mooney.mooney.domain.backup.DataExportImportManager
import com.andriybobchuk.mooney.mooney.domain.backup.DataExportImportManager.Companion.CURRENT_EXPORT_VERSION
import com.andriybobchuk.mooney.testutil.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DataExportImportManagerTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun createManager(
        transactionDao: FakeTransactionDao = FakeTransactionDao(),
        accountDao: FakeAccountDao = FakeAccountDao(),
        goalDao: FakeGoalDao = FakeGoalDao(),
        goalGroupDao: FakeGoalGroupDao = FakeGoalGroupDao(),
        categoryUsageDao: FakeCategoryUsageDao = FakeCategoryUsageDao(),
        categoryDao: FakeCategoryDao = FakeCategoryDao(),
        userCurrencyDao: FakeUserCurrencyDao = FakeUserCurrencyDao(),
        recurringTransactionDao: FakeRecurringTransactionDao = FakeRecurringTransactionDao(),
        pendingTransactionDao: FakePendingTransactionDao = FakePendingTransactionDao()
    ) = DataExportImportManager(
        transactionDao, accountDao, goalDao, goalGroupDao,
        categoryUsageDao, categoryDao, userCurrencyDao,
        recurringTransactionDao, pendingTransactionDao
    )

    private fun parseExport(jsonStr: String): DataExportImportManager.CompleteDataExport =
        json.decodeFromString(jsonStr)

    // region Export Tests

    @Test
    fun `export empty database produces valid JSON`() = runTest {
        val manager = createManager()
        val jsonStr = manager.exportAllData()
        val result = manager.validateExportData(jsonStr)
        assertIs<DataExportImportManager.ValidationResult.Valid>(result)
        assertEquals(0, result.transactions)
        assertEquals(0, result.accounts)
    }

    @Test
    fun `export version is current`() = runTest {
        val manager = createManager()
        val export = parseExport(manager.exportAllData())
        assertEquals(CURRENT_EXPORT_VERSION, export.exportVersion)
    }

    @Test
    fun `export includes all account fields including isPrimary`() = runTest {
        val accountDao = FakeAccountDao()
        accountDao.upsert(AccountEntity(0, "Main", 1000.0, "PLN", "💰", "BANK_ACCOUNT", isPrimary = true))
        val manager = createManager(accountDao = accountDao)

        val export = parseExport(manager.exportAllData())
        assertEquals(1, export.accounts.size)
        val acc = export.accounts.first()
        assertEquals("Main", acc.title)
        assertEquals(1000.0, acc.amount)
        assertEquals("PLN", acc.currency)
        assertEquals("💰", acc.emoji)
        assertEquals("BANK_ACCOUNT", acc.assetCategory)
        assertEquals(true, acc.isPrimary)
    }

    @Test
    fun `export includes all transaction fields`() = runTest {
        val transactionDao = FakeTransactionDao()
        transactionDao.upsert(TransactionEntity(0, "groceries", 50.0, 1, "2024-03-15"))
        val manager = createManager(transactionDao = transactionDao)

        val export = parseExport(manager.exportAllData())
        val tx = export.transactions.first()
        assertEquals("groceries", tx.subcategoryId)
        assertEquals(50.0, tx.amount)
        assertEquals(1, tx.accountId)
        assertEquals("2024-03-15", tx.date)
    }

    @Test
    fun `export includes all goal fields including nullable imagePath`() = runTest {
        val goalDao = FakeGoalDao()
        goalDao.upsert(GoalEntity(0, "🎯", "Trip", "Japan", 15000.0, "PLN", "2024-01-01", "Travel", "/img.png"))
        goalDao.upsert(GoalEntity(0, "🚗", "Car", "Down payment", 40000.0, "PLN", "2024-02-01", "General", null))
        val manager = createManager(goalDao = goalDao)

        val export = parseExport(manager.exportAllData())
        assertEquals(2, export.goals.size)
        val withImage = export.goals.find { it.title == "Trip" }!!
        assertEquals("/img.png", withImage.imagePath)
        assertEquals("Travel", withImage.groupName)
        val withoutImage = export.goals.find { it.title == "Car" }!!
        assertNull(withoutImage.imagePath)
    }

    @Test
    fun `export includes categories`() = runTest {
        val categoryDao = FakeCategoryDao()
        categoryDao.upsert(CategoryEntity("groceries", "Groceries", "EXPENSE", "🛒", "expense"))
        val manager = createManager(categoryDao = categoryDao)

        val export = parseExport(manager.exportAllData())
        assertEquals(1, export.categories.size)
        val cat = export.categories.first()
        assertEquals("groceries", cat.id)
        assertEquals("Groceries", cat.title)
        assertEquals("EXPENSE", cat.type)
        assertEquals("🛒", cat.emoji)
        assertEquals("expense", cat.parentId)
    }

    @Test
    fun `export includes user currencies`() = runTest {
        val userCurrencyDao = FakeUserCurrencyDao()
        userCurrencyDao.upsert(UserCurrencyEntity("PLN", 0))
        userCurrencyDao.upsert(UserCurrencyEntity("USD", 1))
        val manager = createManager(userCurrencyDao = userCurrencyDao)

        val export = parseExport(manager.exportAllData())
        assertEquals(2, export.userCurrencies.size)
        assertTrue(export.userCurrencies.any { it.code == "PLN" && it.sortOrder == 0 })
        assertTrue(export.userCurrencies.any { it.code == "USD" && it.sortOrder == 1 })
    }

    @Test
    fun `export includes recurring transactions with all fields`() = runTest {
        val dao = FakeRecurringTransactionDao()
        dao.upsert(RecurringTransactionEntity(
            0, "Rent", "rent", 2800.0, 1, 1, "MONTHLY",
            weekDay = null, monthOfYear = null, isActive = true,
            createdDate = "2024-01-01", lastProcessedDate = "2024-03-01"
        ))
        val manager = createManager(recurringTransactionDao = dao)

        val export = parseExport(manager.exportAllData())
        assertEquals(1, export.recurringTransactions.size)
        val rt = export.recurringTransactions.first()
        assertEquals("Rent", rt.title)
        assertEquals("MONTHLY", rt.frequency)
        assertEquals(1, rt.dayOfMonth)
        assertNull(rt.weekDay)
        assertEquals(true, rt.isActive)
        assertEquals("2024-03-01", rt.lastProcessedDate)
    }

    @Test
    fun `export includes pending transactions`() = runTest {
        val dao = FakePendingTransactionDao()
        dao.upsert(PendingTransactionEntity(0, 1, "rent", 2800.0, 1, "2024-04-01", "PENDING", "2024-03-25"))
        val manager = createManager(pendingTransactionDao = dao)

        val export = parseExport(manager.exportAllData())
        assertEquals(1, export.pendingTransactions.size)
        val pt = export.pendingTransactions.first()
        assertEquals("PENDING", pt.status)
        assertEquals("2024-04-01", pt.scheduledDate)
    }

    @Test
    fun `export metadata counts match actual data`() = runTest {
        val accountDao = FakeAccountDao()
        accountDao.upsert(AccountEntity(0, "A1", 100.0, "PLN", "💰", "BANK_ACCOUNT"))
        accountDao.upsert(AccountEntity(0, "A2", 200.0, "USD", "🏦", "SAVINGS"))
        val transactionDao = FakeTransactionDao()
        transactionDao.upsert(TransactionEntity(0, "groceries", 50.0, 1, "2024-01-01"))
        val manager = createManager(transactionDao = transactionDao, accountDao = accountDao)

        val export = parseExport(manager.exportAllData())
        assertEquals(export.accounts.size, export.metadata.totalAccounts)
        assertEquals(export.transactions.size, export.metadata.totalTransactions)
    }

    // endregion

    // region Import Tests

    @Test
    fun `import restores accounts with isPrimary`() = runTest {
        val sourceAccountDao = FakeAccountDao()
        sourceAccountDao.upsert(AccountEntity(0, "Primary", 5000.0, "PLN", "💰", "BANK_ACCOUNT", isPrimary = true))
        sourceAccountDao.upsert(AccountEntity(0, "Savings", 10000.0, "USD", "🏦", "SAVINGS", isPrimary = false))
        val sourceManager = createManager(accountDao = sourceAccountDao)
        val jsonStr = sourceManager.exportAllData()

        val targetAccountDao = FakeAccountDao()
        val targetManager = createManager(accountDao = targetAccountDao)
        val result = targetManager.importData(jsonStr)

        assertIs<DataExportImportManager.ImportResult.Success>(result)
        assertEquals(2, result.importedAccounts)
        val imported = targetAccountDao.getAll().first()
        assertTrue(imported.any { it.title == "Primary" && it.isPrimary })
        assertTrue(imported.any { it.title == "Savings" && !it.isPrimary })
    }

    @Test
    fun `import restores categories`() = runTest {
        val sourceCatDao = FakeCategoryDao()
        sourceCatDao.upsert(CategoryEntity("expense", "Expense", "EXPENSE", "☺️", null))
        sourceCatDao.upsert(CategoryEntity("groceries", "Groceries", "EXPENSE", "🛒", "expense"))
        val sourceManager = createManager(categoryDao = sourceCatDao)
        val jsonStr = sourceManager.exportAllData()

        val targetCatDao = FakeCategoryDao()
        val targetManager = createManager(categoryDao = targetCatDao)
        val result = targetManager.importData(jsonStr)

        assertIs<DataExportImportManager.ImportResult.Success>(result)
        assertEquals(2, result.importedCategories)
        val imported = targetCatDao.getAll().first()
        assertEquals(2, imported.size)
        assertTrue(imported.any { it.id == "groceries" && it.parentId == "expense" })
    }

    @Test
    fun `import restores user currencies`() = runTest {
        val sourceDao = FakeUserCurrencyDao()
        sourceDao.upsert(UserCurrencyEntity("PLN", 0))
        sourceDao.upsert(UserCurrencyEntity("EUR", 1))
        val sourceManager = createManager(userCurrencyDao = sourceDao)
        val jsonStr = sourceManager.exportAllData()

        val targetDao = FakeUserCurrencyDao()
        val targetManager = createManager(userCurrencyDao = targetDao)
        val result = targetManager.importData(jsonStr)

        assertIs<DataExportImportManager.ImportResult.Success>(result)
        assertEquals(2, result.importedUserCurrencies)
    }

    @Test
    fun `import restores recurring transactions`() = runTest {
        val sourceDao = FakeRecurringTransactionDao()
        sourceDao.upsert(RecurringTransactionEntity(
            0, "Gym", "sport_gym", 200.0, 1, 2, "MONTHLY",
            null, null, true, "2024-01-01", null
        ))
        val sourceManager = createManager(recurringTransactionDao = sourceDao)
        val jsonStr = sourceManager.exportAllData()

        val targetDao = FakeRecurringTransactionDao()
        val targetManager = createManager(recurringTransactionDao = targetDao)
        val result = targetManager.importData(jsonStr)

        assertIs<DataExportImportManager.ImportResult.Success>(result)
        assertEquals(1, result.importedRecurringTransactions)
        val imported = targetDao.getAll().first()
        assertEquals("Gym", imported.first().title)
        assertEquals("MONTHLY", imported.first().frequency)
    }

    @Test
    fun `import restores pending transactions`() = runTest {
        val sourceDao = FakePendingTransactionDao()
        sourceDao.upsert(PendingTransactionEntity(0, 1, "rent", 2800.0, 1, "2024-04-01", "PENDING", "2024-03-25"))
        val sourceManager = createManager(pendingTransactionDao = sourceDao)
        val jsonStr = sourceManager.exportAllData()

        val targetDao = FakePendingTransactionDao()
        val targetManager = createManager(pendingTransactionDao = targetDao)
        val result = targetManager.importData(jsonStr)

        assertIs<DataExportImportManager.ImportResult.Success>(result)
        assertEquals(1, result.importedPendingTransactions)
    }

    @Test
    fun `full round-trip preserves all data across 9 tables`() = runTest {
        val srcAccounts = FakeAccountDao()
        val srcTransactions = FakeTransactionDao()
        val srcGoals = FakeGoalDao()
        val srcGoalGroups = FakeGoalGroupDao()
        val srcCategoryUsage = FakeCategoryUsageDao()
        val srcCategories = FakeCategoryDao()
        val srcCurrencies = FakeUserCurrencyDao()
        val srcRecurring = FakeRecurringTransactionDao()
        val srcPending = FakePendingTransactionDao()

        srcAccounts.upsert(AccountEntity(0, "Wallet", 500.0, "PLN", "👛", "CASH", isPrimary = true))
        srcTransactions.upsert(TransactionEntity(0, "groceries", 42.5, 1, "2024-06-15"))
        srcGoalGroups.upsert(GoalGroupEntity(0, "Travel", "✈️", "#FF0000", "2024-01-01"))
        srcGoals.upsert(GoalEntity(0, "🏖️", "Beach Trip", "Relax", 5000.0, "EUR", "2024-03-01", "Travel"))
        srcCategoryUsage.upsert(CategoryUsageEntity("groceries", 42, "2024-06-15"))
        srcCategories.upsert(CategoryEntity("custom_cat", "My Custom", "EXPENSE", "🎯", "expense"))
        srcCurrencies.upsert(UserCurrencyEntity("PLN", 0))
        srcCurrencies.upsert(UserCurrencyEntity("EUR", 1))
        srcRecurring.upsert(RecurringTransactionEntity(0, "Rent", "rent", 2800.0, 1, 1, "MONTHLY", null, null, true, "2024-01-01", null))
        srcPending.upsert(PendingTransactionEntity(0, 1, "rent", 2800.0, 1, "2024-04-01", "PENDING", "2024-03-25"))

        val sourceManager = createManager(srcTransactions, srcAccounts, srcGoals, srcGoalGroups, srcCategoryUsage, srcCategories, srcCurrencies, srcRecurring, srcPending)
        val jsonStr = sourceManager.exportAllData()

        // Import into fresh DAOs
        val tgtAccounts = FakeAccountDao()
        val tgtTransactions = FakeTransactionDao()
        val tgtGoals = FakeGoalDao()
        val tgtGoalGroups = FakeGoalGroupDao()
        val tgtCategoryUsage = FakeCategoryUsageDao()
        val tgtCategories = FakeCategoryDao()
        val tgtCurrencies = FakeUserCurrencyDao()
        val tgtRecurring = FakeRecurringTransactionDao()
        val tgtPending = FakePendingTransactionDao()

        val targetManager = createManager(tgtTransactions, tgtAccounts, tgtGoals, tgtGoalGroups, tgtCategoryUsage, tgtCategories, tgtCurrencies, tgtRecurring, tgtPending)
        val result = targetManager.importData(jsonStr)

        assertIs<DataExportImportManager.ImportResult.Success>(result)
        assertEquals(1, result.importedAccounts)
        assertEquals(1, result.importedTransactions)
        assertEquals(1, result.importedGoals)
        assertEquals(1, result.importedGoalGroups)
        assertEquals(1, result.importedCategoryUsages)
        assertEquals(1, result.importedCategories)
        assertEquals(2, result.importedUserCurrencies)
        assertEquals(1, result.importedRecurringTransactions)
        assertEquals(1, result.importedPendingTransactions)

        // Verify specific field values survived
        val acc = tgtAccounts.getAll().first().first()
        assertEquals("Wallet", acc.title)
        assertEquals(500.0, acc.amount)
        assertEquals("PLN", acc.currency)
        assertTrue(acc.isPrimary)
        assertEquals("CASH", acc.assetCategory)

        val tx = tgtTransactions.getAll().first().first()
        assertEquals("groceries", tx.subcategoryId)
        assertEquals(42.5, tx.amount)

        val goal = tgtGoals.getAll().first().first()
        assertEquals("Beach Trip", goal.title)
        assertEquals(5000.0, goal.targetAmount)
        assertEquals("Travel", goal.groupName)

        val cat = tgtCategories.getAll().first().first()
        assertEquals("custom_cat", cat.id)
        assertEquals("expense", cat.parentId)

        val recurring = tgtRecurring.getAll().first().first()
        assertEquals("Rent", recurring.title)
        assertEquals("MONTHLY", recurring.frequency)
    }

    // endregion

    // region Validation Tests

    @Test
    fun `validate rejects corrupted JSON`() {
        val manager = createManager()
        val result = manager.validateExportData("not json at all")
        assertIs<DataExportImportManager.ValidationResult.Invalid>(result)
    }

    @Test
    fun `validate rejects tampered checksum`() = runTest {
        val manager = createManager()
        val jsonStr = manager.exportAllData()
        val tampered = jsonStr.replace(Regex("\"checksum\"\\s*:\\s*\"[^\"]+\""), "\"checksum\": \"wrong\"")
        val result = manager.validateExportData(tampered)
        assertIs<DataExportImportManager.ValidationResult.Invalid>(result)
        assertTrue(result.reason.contains("Checksum"))
    }

    @Test
    fun `validate accepts valid export`() = runTest {
        val accountDao = FakeAccountDao()
        accountDao.upsert(AccountEntity(0, "Test", 100.0, "PLN", "💰", "BANK_ACCOUNT"))
        val manager = createManager(accountDao = accountDao)
        val jsonStr = manager.exportAllData()

        val result = manager.validateExportData(jsonStr)
        assertIs<DataExportImportManager.ValidationResult.Valid>(result)
        assertEquals(1, result.accounts)
    }

    @Test
    fun `import rejects future version`() = runTest {
        val manager = createManager()
        val jsonStr = manager.exportAllData()
        val futureVersion = jsonStr.replace(
            "\"exportVersion\": $CURRENT_EXPORT_VERSION",
            "\"exportVersion\": 99"
        )
        val result = manager.importData(futureVersion)
        assertIs<DataExportImportManager.ImportResult.Error>(result)
        assertTrue(result.message.contains("Unsupported"))
    }

    @Test
    fun `import handles malformed JSON`() = runTest {
        val manager = createManager()
        val result = manager.importData("{broken")
        assertIs<DataExportImportManager.ImportResult.Error>(result)
    }

    // endregion

    // region Backward Compatibility

    @Test
    fun `import handles v1 export without new fields gracefully`() = runTest {
        val v1Json = """
        {
            "exportVersion": 1,
            "exportDate": 1700000000,
            "appVersion": "1.0.0",
            "transactions": [],
            "accounts": [
                {
                    "id": 1,
                    "title": "Old Account",
                    "amount": 1000.0,
                    "currency": "PLN",
                    "emoji": "💰",
                    "assetCategory": "BANK_ACCOUNT"
                }
            ],
            "goals": [],
            "goalGroups": [],
            "categoryUsages": [],
            "metadata": {
                "totalTransactions": 0,
                "totalAccounts": 1,
                "totalGoals": 0,
                "totalGoalGroups": 0,
                "totalCategoryUsages": 0,
                "checksum": "${generateV1Checksum(0, 1, 0, 0, 0)}"
            }
        }
        """.trimIndent()

        val targetAccountDao = FakeAccountDao()
        val targetManager = createManager(accountDao = targetAccountDao)
        val result = targetManager.importData(v1Json)

        assertIs<DataExportImportManager.ImportResult.Success>(result)
        assertEquals(1, result.importedAccounts)
        assertEquals(0, result.importedCategories)

        val acc = targetAccountDao.getAll().first().first()
        assertEquals("Old Account", acc.title)
        assertEquals(false, acc.isPrimary) // default
    }

    @Test
    fun `validate accepts v1 export with v1 checksum`() {
        val v1Json = """
        {
            "exportVersion": 1,
            "exportDate": 1700000000,
            "appVersion": "1.0.0",
            "transactions": [],
            "accounts": [],
            "goals": [],
            "goalGroups": [],
            "categoryUsages": [],
            "metadata": {
                "totalTransactions": 0,
                "totalAccounts": 0,
                "totalGoals": 0,
                "totalGoalGroups": 0,
                "totalCategoryUsages": 0,
                "checksum": "${generateV1Checksum(0, 0, 0, 0, 0)}"
            }
        }
        """.trimIndent()

        val manager = createManager()
        val result = manager.validateExportData(v1Json)
        assertIs<DataExportImportManager.ValidationResult.Valid>(result)
    }

    // endregion

    // region Edge Cases

    @Test
    fun `export handles emoji in all fields`() = runTest {
        val accountDao = FakeAccountDao()
        accountDao.upsert(AccountEntity(0, "Зарплата 💰", 1000.0, "UAH", "🏦", "BANK_ACCOUNT"))
        val goalDao = FakeGoalDao()
        goalDao.upsert(GoalEntity(0, "🎯", "Поїздка 🏖️", "Опис", 5000.0, "PLN", "2024-01-01", "General"))
        val manager = createManager(accountDao = accountDao, goalDao = goalDao)

        val jsonStr = manager.exportAllData()
        val targetAccountDao = FakeAccountDao()
        val targetGoalDao = FakeGoalDao()
        val targetManager = createManager(accountDao = targetAccountDao, goalDao = targetGoalDao)
        targetManager.importData(jsonStr)

        assertEquals("Зарплата 💰", targetAccountDao.getAll().first().first().title)
        assertEquals("Поїздка 🏖️", targetGoalDao.getAll().first().first().title)
    }

    @Test
    fun `export handles special characters in strings`() = runTest {
        val accountDao = FakeAccountDao()
        accountDao.upsert(AccountEntity(0, "Test \"quotes\" & <html>", 0.0, "PLN", "💰", "BANK_ACCOUNT"))
        val manager = createManager(accountDao = accountDao)

        val jsonStr = manager.exportAllData()
        val targetAccountDao = FakeAccountDao()
        val targetManager = createManager(accountDao = targetAccountDao)
        targetManager.importData(jsonStr)

        assertEquals("Test \"quotes\" & <html>", targetAccountDao.getAll().first().first().title)
    }

    @Test
    fun `export handles zero and negative amounts`() = runTest {
        val accountDao = FakeAccountDao()
        accountDao.upsert(AccountEntity(0, "Empty", 0.0, "PLN", "💰", "BANK_ACCOUNT"))
        accountDao.upsert(AccountEntity(0, "Debt", -500.0, "PLN", "💸", "BANK_ACCOUNT"))
        val manager = createManager(accountDao = accountDao)

        val jsonStr = manager.exportAllData()
        val targetAccountDao = FakeAccountDao()
        val targetManager = createManager(accountDao = targetAccountDao)
        targetManager.importData(jsonStr)

        val imported = targetAccountDao.getAll().first()
        assertEquals(2, imported.size)
        assertTrue(imported.any { it.amount == 0.0 })
        assertTrue(imported.any { it.amount == -500.0 })
    }

    @Test
    fun `category with null emoji and null parentId round-trips`() = runTest {
        val categoryDao = FakeCategoryDao()
        categoryDao.upsert(CategoryEntity("expense", "Expense", "EXPENSE", null, null))
        val manager = createManager(categoryDao = categoryDao)

        val jsonStr = manager.exportAllData()
        val targetDao = FakeCategoryDao()
        val targetManager = createManager(categoryDao = targetDao)
        targetManager.importData(jsonStr)

        val cat = targetDao.getAll().first().first()
        assertNull(cat.emoji)
        assertNull(cat.parentId)
    }

    @Test
    fun `recurring transaction with null optional fields round-trips`() = runTest {
        val dao = FakeRecurringTransactionDao()
        dao.upsert(RecurringTransactionEntity(
            0, "Daily", "groceries", 50.0, 1, 1, "DAILY",
            weekDay = null, monthOfYear = null, isActive = true,
            createdDate = "2024-01-01", lastProcessedDate = null
        ))
        val manager = createManager(recurringTransactionDao = dao)

        val jsonStr = manager.exportAllData()
        val targetDao = FakeRecurringTransactionDao()
        val targetManager = createManager(recurringTransactionDao = targetDao)
        targetManager.importData(jsonStr)

        val rt = targetDao.getAll().first().first()
        assertNull(rt.weekDay)
        assertNull(rt.monthOfYear)
        assertNull(rt.lastProcessedDate)
    }

    // endregion

    private fun generateV1Checksum(t: Int, a: Int, g: Int, gg: Int, cu: Int): String {
        return "$t-$a-$g-$gg-$cu".hashCode().toString()
    }
}
