package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.core.data.database.AccountDao
import com.andriybobchuk.mooney.core.data.database.AccountEntity
import com.andriybobchuk.mooney.core.data.database.GoalDao
import com.andriybobchuk.mooney.core.data.database.GoalEntity
import com.andriybobchuk.mooney.core.data.database.RecurringTransactionDao
import com.andriybobchuk.mooney.core.data.database.RecurringTransactionEntity
import com.andriybobchuk.mooney.core.data.database.TransactionDao
import com.andriybobchuk.mooney.core.data.database.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random

/**
 * Fills an empty app with 15 months of realistic demo data — accounts, goals,
 * recurring rules, and a spread of income + expense transactions. Used by the
 * marketing / screen-recording flow so App Store and Play screenshots show a
 * lived-in app instead of blank state.
 *
 * Writes go straight to DAOs (not through [AddTransactionUseCase]) so we can
 * seed hundreds of rows without triggering per-row balance side-effects; the
 * account balances are set explicitly at the end. Guarded by [isEmpty] so the
 * caller can only trigger it on a fresh install.
 */
class SeedDemoDataUseCase(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val goalDao: GoalDao,
    private val recurringTransactionDao: RecurringTransactionDao
) {

    suspend fun isEmpty(): Boolean {
        val txs = transactionDao.getAll().first()
        val accs = accountDao.getAll().first()
        return txs.isEmpty() && accs.isEmpty()
    }

    @Suppress("LongMethod", "MagicNumber", "CyclomaticComplexMethod", "LongParameterList")
    suspend operator fun invoke() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val rng = Random(seed = 1_234_567L)

        // Upsert then read back to discover the auto-assigned IDs. Room's
        // @Upsert can return Long but adding that overload would shadow the
        // production method, so we just refetch — cheap for six rows.
        accountDao.upsert(bankSeed())
        accountDao.upsert(cashSeed())
        accountDao.upsert(savingsSeed())
        accountDao.upsert(investmentsSeed())
        accountDao.upsert(flatSeed())
        accountDao.upsert(carSeed())

        val accounts = accountDao.getAll().first().associateBy { it.title }
        val bankId = requireAccountId(accounts, "Bank")
        val cashId = requireAccountId(accounts, "Cash")
        val savingsId = requireAccountId(accounts, "Savings")
        val investmentsId = requireAccountId(accounts, "Investments")

        val rentAmount = 2_800.0
        val netflixAmount = 55.0
        val spotifyAmount = 24.99
        val gymAmount = 149.0

        recurringTransactionDao.upsert(
            RecurringTransactionEntity(
                title = "Rent",
                subcategoryId = "rent",
                amount = rentAmount,
                accountId = bankId,
                dayOfMonth = 5,
                frequency = "MONTHLY",
                createdDate = today.toString(),
                description = "Monthly rent" // allow-hardcoded (demo seeder, English-only marketing data)
            )
        )
        recurringTransactionDao.upsert(
            RecurringTransactionEntity(
                title = "Netflix", // allow-hardcoded (demo seeder)
                subcategoryId = "subscriptions",
                amount = netflixAmount,
                accountId = bankId,
                dayOfMonth = 12,
                frequency = "MONTHLY",
                createdDate = today.toString(),
                description = "Netflix" // allow-hardcoded (demo seeder)
            )
        )
        recurringTransactionDao.upsert(
            RecurringTransactionEntity(
                title = "Spotify", // allow-hardcoded (demo seeder)
                subcategoryId = "subscriptions",
                amount = spotifyAmount,
                accountId = bankId,
                dayOfMonth = 18,
                frequency = "MONTHLY",
                createdDate = today.toString(),
                description = "Spotify" // allow-hardcoded (demo seeder)
            )
        )
        recurringTransactionDao.upsert(
            RecurringTransactionEntity(
                title = "Gym",
                subcategoryId = "subscriptions",
                amount = gymAmount,
                accountId = bankId,
                dayOfMonth = 3,
                frequency = "MONTHLY",
                createdDate = today.toString(),
                description = "Gym membership" // allow-hardcoded (demo seeder)
            )
        )

        // Assemble 15 months of transactions oldest → newest so the timeline
        // looks natural in charts. We write raw entities and set the final
        // account balances at the end, rather than running each tx through
        // AddTransactionUseCase — otherwise 500+ side-effect writes would turn
        // a one-tap seeder into a several-second freeze on device.
        val months = buildMonthList(today, 15)

        months.forEach { month ->
            val salary = 5_500.0 + rng.nextInt(0, 2_001) // 5500..7500
            insertTx(month.withDay(25), "salary", salary, bankId, "Monthly salary")

            if (rng.nextInt(0, 100) < 40) {
                val freelance = 800.0 + rng.nextInt(0, 1_701)
                insertTx(month.withDay(15), "freelance", freelance, bankId, "Freelance project")
            }
            if (rng.nextInt(0, 100) < 25) {
                val div = 120.0 + rng.nextInt(0, 300)
                insertTx(month.withDay(20), "dividends", div, investmentsId, "Quarterly dividend")
            }

            insertTx(month.withDay(5), "rent", rentAmount, bankId, "Rent")
            insertTx(month.withDay(12), "subscriptions", netflixAmount, bankId, "Netflix")
            insertTx(month.withDay(18), "subscriptions", spotifyAmount, bankId, "Spotify")
            insertTx(month.withDay(3), "subscriptions", gymAmount, bankId, "Gym membership")

            insertTx(month.withDay(10), "tax", salary * 0.19, bankId, "Income tax")

            insertTx(month.withDay(2), "electricity", 90.0 + rng.nextInt(0, 60), bankId, "Electricity")
            insertTx(month.withDay(7), "internet", 79.0, bankId, "Internet")
            insertTx(month.withDay(9), "insurance", 220.0, bankId, "Car insurance")

            repeat(6 + rng.nextInt(0, 4)) {
                val day = 1 + rng.nextInt(0, 28)
                val amount = 45.0 + rng.nextInt(0, 141)
                val acc = if (rng.nextInt(0, 100) < 20) cashId else bankId
                insertTx(month.withDay(day), "groceries", amount, acc, null)
            }
            repeat(3 + rng.nextInt(0, 5)) {
                val day = 1 + rng.nextInt(0, 28)
                val amount = 12.0 + rng.nextInt(0, 25)
                insertTx(month.withDay(day), "cafes_coffee", amount, cashId, null)
            }
            repeat(2 + rng.nextInt(0, 4)) {
                val day = 1 + rng.nextInt(0, 28)
                val amount = 55.0 + rng.nextInt(0, 90)
                insertTx(month.withDay(day), "eating_out", amount, bankId, null)
            }
            repeat(2 + rng.nextInt(0, 3)) {
                val day = 1 + rng.nextInt(0, 28)
                val amount = 200.0 + rng.nextInt(0, 200)
                insertTx(month.withDay(day), "car_fuel", amount, bankId, "Fuel")
            }
            repeat(rng.nextInt(0, 3)) {
                val day = 1 + rng.nextInt(0, 28)
                val amount = 30.0 + rng.nextInt(0, 60)
                insertTx(month.withDay(day), "transport", amount, cashId, null)
            }

            if (rng.nextInt(0, 100) < 60) {
                val day = 1 + rng.nextInt(0, 28)
                insertTx(month.withDay(day), "clothing", 150.0 + rng.nextInt(0, 300), bankId, null)
            }
            if (rng.nextInt(0, 100) < 40) {
                val day = 1 + rng.nextInt(0, 28)
                insertTx(month.withDay(day), "games_gaming", 40.0 + rng.nextInt(0, 90), bankId, null)
            }
            if (rng.nextInt(0, 100) < 25) {
                val day = 1 + rng.nextInt(0, 28)
                insertTx(month.withDay(day), "concerts", 120.0 + rng.nextInt(0, 200), bankId, null)
            }
            if (rng.nextInt(0, 100) < 30) {
                val day = 1 + rng.nextInt(0, 28)
                insertTx(month.withDay(day), "gifts", 80.0 + rng.nextInt(0, 200), bankId, "Gift")
            }
        }

        // Set the final balances the screenshots should show. These are picked
        // by hand to look realistic given the seeded income/expense mix — the
        // actual arithmetic drift is irrelevant since we bypassed the balance
        // side-effect path above.
        accountDao.upsert(bankSeed().copy(id = bankId, amount = 8_240.0))
        accountDao.upsert(cashSeed().copy(id = cashId, amount = 420.0))
        accountDao.upsert(savingsSeed().copy(id = savingsId, amount = 15_600.0))
        accountDao.upsert(investmentsSeed().copy(id = investmentsId, amount = 8_500.0))

        goalDao.upsert(
            GoalEntity(
                emoji = "🏝️",
                title = "Vacation to Japan", // allow-hardcoded (demo seeder)
                description = "Two weeks in spring", // allow-hardcoded (demo seeder)
                targetAmount = 12_000.0,
                currency = "PLN",
                createdDate = today.toString(),
                trackingType = "ACCOUNT",
                accountId = savingsId
            )
        )
        goalDao.upsert(
            GoalEntity(
                emoji = "🛡️",
                title = "Emergency Fund", // allow-hardcoded (demo seeder)
                description = "6 months of expenses",
                targetAmount = 30_000.0,
                currency = "PLN",
                createdDate = today.toString(),
                trackingType = "NET_WORTH"
            )
        )
        goalDao.upsert(
            GoalEntity(
                emoji = "🏡",
                title = "New Kitchen", // allow-hardcoded (demo seeder)
                description = "Renovation savings", // allow-hardcoded (demo seeder)
                targetAmount = 25_000.0,
                currency = "PLN",
                createdDate = today.toString(),
                trackingType = "NET_WORTH"
            )
        )
    }

    private fun bankSeed() = AccountEntity(
        title = "Bank",
        amount = 0.0,
        currency = "PLN",
        emoji = "🏦",
        assetCategory = "BANK_ACCOUNT",
        isPrimary = true,
        isPrimaryForExpenses = true,
        isPrimaryForIncome = true
    )

    private fun cashSeed() = AccountEntity(
        title = "Cash",
        amount = 0.0,
        currency = "PLN",
        emoji = "💵",
        assetCategory = "CASH"
    )

    private fun savingsSeed() = AccountEntity(
        title = "Savings", // allow-hardcoded (demo seeder)
        amount = 0.0,
        currency = "PLN",
        emoji = "💰",
        assetCategory = "BANK_ACCOUNT"
    )

    private fun investmentsSeed() = AccountEntity(
        title = "Investments", // allow-hardcoded (demo seeder)
        amount = 0.0,
        currency = "PLN",
        emoji = "📈",
        assetCategory = "STOCKS",
        currentMarketValue = 9_800.0
    )

    private fun flatSeed() = AccountEntity(
        title = "Flat",
        amount = 380_000.0,
        currency = "PLN",
        emoji = "🏠",
        assetCategory = "REAL_ESTATE",
        currentMarketValue = 420_000.0
    )

    private fun carSeed() = AccountEntity(
        title = "Car",
        amount = 45_000.0,
        currency = "PLN",
        emoji = "🚗",
        assetCategory = "VEHICLE",
        currentMarketValue = 38_500.0
    )

    private suspend fun insertTx(date: LocalDate, subcategoryId: String, amount: Double, accountId: Int, description: String?) {
        transactionDao.upsert(
            TransactionEntity(
                subcategoryId = subcategoryId,
                amount = amount,
                accountId = accountId,
                date = date.toString(),
                description = description
            )
        )
    }

    private fun requireAccountId(accounts: Map<String, AccountEntity>, title: String): Int {
        return accounts[title]?.id ?: error("Seed failed: expected account '$title' missing")
    }

    private fun buildMonthList(anchor: LocalDate, count: Int): List<LocalDate> {
        // First-of-month for `count` months ending at `anchor`, oldest first.
        val result = mutableListOf<LocalDate>()
        var year = anchor.year
        var month = anchor.monthNumber
        repeat(count) {
            result.add(LocalDate(year, month, 1))
            month -= 1
            if (month == 0) {
                month = 12
                year -= 1
            }
        }
        return result.reversed()
    }

    private fun LocalDate.withDay(day: Int): LocalDate {
        // Clamp to 28 so Feb never overflows. We're not relying on the exact
        // day matching a real calendar — the seed is illustrative.
        val safeDay = day.coerceAtMost(28)
        return LocalDate(this.year, this.monthNumber, safeDay)
    }
}
