package com.andriybobchuk.mooney.mooney.domain.devtools

import com.andriybobchuk.mooney.core.data.database.PendingTransactionDao
import com.andriybobchuk.mooney.core.data.database.RecurringTransactionDao
import com.andriybobchuk.mooney.core.data.database.RecurringTransactionEntity
import com.andriybobchuk.mooney.mooney.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class DevToolsManager(
    private val repository: CoreRepository,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val pendingTransactionDao: PendingTransactionDao
) {
    suspend fun getStats(): DevStats {
        val accounts = repository.getAllAccounts().first().filterNotNull()
        val transactions = repository.getAllTransactions().first().filterNotNull()
        val goals = repository.getAllGoals().first()
        val recurring = recurringTransactionDao.getAll().first()
        return DevStats(
            accountCount = accounts.size,
            transactionCount = transactions.size,
            goalCount = goals.size,
            recurringCount = recurring.size
        )
    }

    suspend fun populateMockAccounts() {
        val accounts = listOf(
            Account(0, "Primary PLN", 12450.0, Currency.PLN, "🏦", AssetCategory.BANK_ACCOUNT),
            Account(0, "USD Savings", 3200.0, Currency.USD, "💵", AssetCategory.BANK_ACCOUNT),
            Account(0, "EUR Travel", 850.0, Currency.EUR, "✈️", AssetCategory.CASH),
            Account(0, "Crypto", 1500.0, Currency.USD, "₿", AssetCategory.CRYPTO),
            Account(0, "Emergency Fund", 5000.0, Currency.PLN, "🛡️", AssetCategory.BANK_ACCOUNT),
        )
        accounts.forEach { repository.upsertAccount(it) }
    }

    suspend fun populateMockTransactions() {
        val accounts = repository.getAllAccounts().first().filterNotNull()
        if (accounts.isEmpty()) return

        val primaryAccount = accounts.first()
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val categories = repository.getAllCategories()

        val salary = categories.find { it.id == "salary" }
        val groceries = categories.find { it.id == "groceries" }
        val rent = categories.find { it.id == "rent" }
        val zus = categories.find { it.id == "zus" }
        val pit = categories.find { it.id == "pit" }
        val transport = categories.find { it.id == "transport" }
        val sport = categories.find { it.id == "sport" }
        val subscriptions = categories.find { it.id == "subscriptions" }
        val beverages = categories.find { it.id == "beverages" }

        // Generate 6 months of transactions
        for (monthOffset in 0..5) {
            val baseDate = now.minus(monthOffset, DateTimeUnit.MONTH)
            val monthDate = LocalDate(baseDate.year, baseDate.monthNumber, 1)

            // Income
            salary?.let {
                repository.upsertTransaction(Transaction(0, it, 8500.0 + (monthOffset * 100.0), primaryAccount, monthDate.withDay(10)))
            }

            // Expenses
            groceries?.let {
                repository.upsertTransaction(Transaction(0, it, 650.0 + (monthOffset * 20.0), primaryAccount, monthDate.withDay(3)))
                repository.upsertTransaction(Transaction(0, it, 420.0, primaryAccount, monthDate.withDay(17)))
            }
            rent?.let {
                repository.upsertTransaction(Transaction(0, it, 2800.0, primaryAccount, monthDate.withDay(1)))
            }
            zus?.let {
                repository.upsertTransaction(Transaction(0, it, 1400.0, primaryAccount, monthDate.withDay(15)))
            }
            pit?.let {
                repository.upsertTransaction(Transaction(0, it, 850.0, primaryAccount, monthDate.withDay(20)))
            }
            transport?.let {
                repository.upsertTransaction(Transaction(0, it, 180.0, primaryAccount, monthDate.withDay(5)))
            }
            sport?.let {
                repository.upsertTransaction(Transaction(0, it, 200.0, primaryAccount, monthDate.withDay(2)))
            }
            subscriptions?.let {
                repository.upsertTransaction(Transaction(0, it, 95.0, primaryAccount, monthDate.withDay(8)))
            }
            beverages?.let {
                repository.upsertTransaction(Transaction(0, it, 320.0, primaryAccount, monthDate.withDay(12)))
                repository.upsertTransaction(Transaction(0, it, 150.0, primaryAccount, monthDate.withDay(22)))
            }
        }
    }

    suspend fun populateMockGoals() {
        val goals = listOf(
            Goal(0, "🏖️", "Vacation Fund", "Trip to Japan", 15000.0, Currency.PLN),
            Goal(0, "🚗", "New Car", "Down payment", 40000.0, Currency.PLN),
            Goal(0, "🏠", "Apartment", "Savings for deposit", 100000.0, Currency.PLN),
        )
        goals.forEach { repository.upsertGoal(it) }
    }

    suspend fun clearAllAccounts() {
        val accounts = repository.getAllAccounts().first().filterNotNull()
        accounts.forEach { repository.deleteAccount(it.id) }
    }

    suspend fun clearAllTransactions() {
        val transactions = repository.getAllTransactions().first().filterNotNull()
        transactions.forEach { repository.deleteTransaction(it.id) }
    }

    suspend fun clearAllGoals() {
        val goals = repository.getAllGoals().first()
        goals.forEach { repository.deleteGoal(it.id) }
    }

    suspend fun clearRecurringTransactions() {
        recurringTransactionDao.getAll().first().forEach { recurringTransactionDao.delete(it.id) }
        pendingTransactionDao.getAll().first().forEach { pendingTransactionDao.delete(it.id) }
    }

    suspend fun populateMockRecurringTransactions() {
        val accounts = repository.getAllAccounts().first().filterNotNull()
        val firstAccountId = accounts.firstOrNull()?.id ?: return

        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

        val recurring = listOf(
            RecurringTransactionEntity(
                title = "Rent",
                subcategoryId = "rent",
                amount = 3500.0,
                accountId = firstAccountId,
                dayOfMonth = 1,
                frequency = "MONTHLY",
                createdDate = today.toString(),
                lastProcessedDate = today.minus(1, DateTimeUnit.MONTH).toString()
            ),
            RecurringTransactionEntity(
                title = "Netflix",
                subcategoryId = "subscriptions",
                amount = 49.99,
                accountId = firstAccountId,
                dayOfMonth = 15,
                frequency = "MONTHLY",
                createdDate = today.toString(),
                lastProcessedDate = today.minus(1, DateTimeUnit.MONTH).toString()
            ),
            RecurringTransactionEntity(
                title = "Gym",
                subcategoryId = "sport",
                amount = 150.0,
                accountId = firstAccountId,
                dayOfMonth = 5,
                frequency = "MONTHLY",
                createdDate = today.toString(),
                lastProcessedDate = today.minus(1, DateTimeUnit.MONTH).toString()
            )
        )

        recurring.forEach { recurringTransactionDao.upsert(it) }
    }

    suspend fun clearEverything() {
        clearAllTransactions()
        clearAllAccounts()
        clearAllGoals()
        clearRecurringTransactions()
    }

    suspend fun populateEverything() {
        populateMockAccounts()
        populateMockTransactions()
        populateMockGoals()
        populateMockRecurringTransactions()
    }

    private fun LocalDate.withDay(day: Int): LocalDate {
        val maxDay = when (monthNumber) {
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return LocalDate(year, monthNumber, day.coerceAtMost(maxDay))
    }
}

data class DevStats(
    val accountCount: Int,
    val transactionCount: Int,
    val goalCount: Int,
    val recurringCount: Int = 0
)
