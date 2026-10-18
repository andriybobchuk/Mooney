package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.core.data.database.NetWorthSnapshotDao
import com.andriybobchuk.mooney.core.data.database.NetWorthSnapshotEntity
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRateProvider
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.usecase.CurrencyManagerUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.networth.CatchUpNetWorthSnapshotsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.networth.WriteNetWorthSnapshotUseCase
import com.andriybobchuk.mooney.testutil.FakeCoreRepository
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * In-memory DAO so we can assert what the catch-up writes.
 */
private class InMemorySnapshotDao : NetWorthSnapshotDao {
    val stored = mutableListOf<NetWorthSnapshotEntity>()
    private val flow = MutableStateFlow<List<NetWorthSnapshotEntity>>(emptyList())

    override suspend fun upsertAll(snapshots: List<NetWorthSnapshotEntity>) {
        snapshots.forEach { s ->
            // Emulate unique (accountId, date) constraint: replace if exists.
            val existing = stored.indexOfFirst {
                it.accountId == s.accountId && it.date == s.date
            }
            if (existing >= 0) stored[existing] = s else stored += s
        }
        flow.value = stored.toList()
    }

    override fun observeAll(): Flow<List<NetWorthSnapshotEntity>> = flow
    override suspend fun getByDate(date: String): List<NetWorthSnapshotEntity> =
        stored.filter { it.date == date }
    override suspend fun getLatestSnapshotDate(): String? =
        stored.maxOfOrNull { it.date }
    override suspend fun deleteByAccount(accountId: Int) {
        stored.removeAll { it.accountId == accountId }
    }
}

/**
 * Fixed rates: 1 PLN = 0.25 USD. Return via Result.Success per the
 * provider contract.
 */
private class FixedRateProvider : ExchangeRateProvider {
    override suspend fun getExchangeRates(
        baseCurrency: Currency
    ): com.andriybobchuk.mooney.core.domain.Result<ExchangeRates, com.andriybobchuk.mooney.core.domain.DataError.Remote> {
        return com.andriybobchuk.mooney.core.domain.Result.Success(
            ExchangeRates(mapOf(Currency.PLN to 1.0, Currency.USD to 0.25))
        )
    }
}

class CatchUpNetWorthSnapshotsUseCaseTest {

    private val repo = FakeCoreRepository()
    private val dao = InMemorySnapshotDao()
    private val currencyManager = CurrencyManagerUseCase(FixedRateProvider())
    private val writeSnapshot = WriteNetWorthSnapshotUseCase(dao, currencyManager)
    private val sut = CatchUpNetWorthSnapshotsUseCase(repo, dao, writeSnapshot)

    init {
        // Fix base currency across tests so WriteNetWorthSnapshot's conversion
        // path is deterministic. GlobalConfig is a singleton — resetting it
        // on construction keeps test order from leaking state.
        GlobalConfig.baseCurrency = Currency.PLN
    }

    private val plnAccount = Account(
        id = 1, title = "Checking", amount = 1000.0,
        currency = Currency.PLN, emoji = "🏦"
    )

    @Test
    fun `empty account list writes nothing`() = runTest {
        sut()
        assertEquals(0, dao.stored.size)
    }

    @Test
    fun `single account first run writes 12 historical month-ends`() = runTest {
        repo.upsertAccount(plnAccount)
        sut()
        // 12 months × 1 account = 12 snapshot rows.
        assertEquals(12, dao.stored.size)
        // Every row is for accountId=1, baseCurrency=PLN.
        assertTrue(dao.stored.all { it.accountId == 1 })
        assertTrue(dao.stored.all { it.baseCurrency == "PLN" })
        // All reconstructed (first run has no live snapshots to beat).
        assertTrue(dao.stored.all { it.isReconstructed })
    }

    @Test
    fun `catch-up with no transactions historical balance equals current`() = runTest {
        repo.upsertAccount(plnAccount)
        sut()
        // No transactions ever → every snapshot's amount = current amount.
        assertTrue(dao.stored.all { it.originalAmount == 1000.0 })
        assertTrue(dao.stored.all { it.baseAmount == 1000.0 })
    }

    @Test
    fun `expense since snapshot date undoes current to higher historical`() = runTest {
        repo.upsertAccount(plnAccount)
        // Insert a -200 PLN expense dated AFTER the oldest month-end.
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        repo.upsertTransaction(
            Transaction(
                id = 0,
                subcategory = TestFixtures.groceries,
                amount = 200.0,
                account = plnAccount,
                date = today
            )
        )
        sut()
        // Current = 1000, expense of 200 happened TODAY (after every
        // backfilled month-end), so historical balance for every snapshot
        // should be 1000 + 200 = 1200.
        assertTrue(
            dao.stored.all { it.originalAmount == 1200.0 },
            "got amounts: ${dao.stored.map { it.originalAmount }}"
        )
    }

    @Test
    fun `second run is idempotent and does not duplicate rows`() = runTest {
        repo.upsertAccount(plnAccount)
        sut()
        val firstRunCount = dao.stored.size
        sut()
        // Second run: because latestExisting watermark = oldest month-end,
        // AND we only process month-ends STRICTLY GREATER than the
        // watermark, nothing new gets written. The first run already
        // covered all target month-ends.
        // The exact count = 12 from first run, unchanged.
        assertEquals(firstRunCount, dao.stored.size)
    }
}
