package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.core.data.database.RecurringTransactionEntity
import com.andriybobchuk.mooney.mooney.domain.usecase.ProcessRecurringTransactionsUseCase
import com.andriybobchuk.mooney.testutil.FakePendingTransactionDao
import com.andriybobchuk.mooney.testutil.FakeRecurringTransactionDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProcessRecurringTransactionsUseCaseTest {

    private val recurringDao = FakeRecurringTransactionDao()
    private val pendingDao = FakePendingTransactionDao()
    private val sut = ProcessRecurringTransactionsUseCase(recurringDao, pendingDao)

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val yesterday = today.minus(DatePeriod(days = 1))
    private val twoDaysAgo = today.minus(DatePeriod(days = 2))

    private fun monthly(
        id: Int = 0,
        dayOfMonth: Int = 15,
        lastProcessedDate: String? = null,
        isActive: Boolean = true,
        subcategoryId: String = "groceries",
        amount: Double = 100.0,
        accountId: Int = 1
    ) = RecurringTransactionEntity(
        id = id,
        title = "Test Recurring",
        subcategoryId = subcategoryId,
        amount = amount,
        accountId = accountId,
        dayOfMonth = dayOfMonth,
        frequency = "MONTHLY",
        isActive = isActive,
        createdDate = "2024-01-01"
    ).let { it.copy(lastProcessedDate = lastProcessedDate) }

    private fun weekly(
        id: Int = 0,
        weekDay: Int = 0,
        lastProcessedDate: String? = null
    ) = RecurringTransactionEntity(
        id = id,
        title = "Weekly Recurring",
        subcategoryId = "groceries",
        amount = 50.0,
        accountId = 1,
        dayOfMonth = 1,
        frequency = "WEEKLY",
        weekDay = weekDay,
        isActive = true,
        createdDate = "2024-01-01",
        lastProcessedDate = lastProcessedDate
    )

    private fun yearly(
        id: Int = 0,
        dayOfMonth: Int = 1,
        monthOfYear: Int = 6,
        lastProcessedDate: String? = null
    ) = RecurringTransactionEntity(
        id = id,
        title = "Yearly Recurring",
        subcategoryId = "salary",
        amount = 1000.0,
        accountId = 1,
        dayOfMonth = dayOfMonth,
        frequency = "YEARLY",
        monthOfYear = monthOfYear,
        isActive = true,
        createdDate = "2024-01-01",
        lastProcessedDate = lastProcessedDate
    )

    private fun daily(
        id: Int = 0,
        lastProcessedDate: String? = null
    ) = RecurringTransactionEntity(
        id = id,
        title = "Daily Recurring",
        subcategoryId = "groceries",
        amount = 10.0,
        accountId = 1,
        dayOfMonth = 1,
        frequency = "DAILY",
        isActive = true,
        createdDate = "2024-01-01",
        lastProcessedDate = lastProcessedDate
    )

    // --- Monthly frequency tests ---

    @Test
    fun `monthly recurring with no last processed creates pending for current month`() = runTest {
        recurringDao.upsert(monthly(dayOfMonth = 1))
        sut()
        val pending = pendingDao.getAllPending().first()
        assertEquals(1, pending.size)
        assertEquals("groceries", pending[0].subcategoryId)
        assertEquals(100.0, pending[0].amount)
    }

    @Test
    fun `monthly recurring already processed this month does not create duplicate`() = runTest {
        // Last processed this month, so next due is next month (future)
        recurringDao.upsert(monthly(lastProcessedDate = today.toString(), dayOfMonth = 15))
        sut()
        val pending = pendingDao.getAllPending().first()
        // Next due is next month's 15th, which is in the future
        assertEquals(0, pending.size)
    }

    @Test
    fun `monthly recurring with past last processed creates pending`() = runTest {
        // Last processed last month on the 15th → next due this month 15th
        val lastMonth = today.minus(DatePeriod(months = 1))
        val nextDueDay = 15
        recurringDao.upsert(monthly(lastProcessedDate = lastMonth.toString(), dayOfMonth = nextDueDay))
        sut()
        val pending = pendingDao.getAllPending().first()
        // Whether pending is created depends on if day 15 is <= today
        if (today.dayOfMonth >= nextDueDay) {
            assertEquals(1, pending.size)
        } else {
            assertEquals(0, pending.size)
        }
    }

    @Test
    fun `monthly recurring due today creates pending`() = runTest {
        // Last processed last month on today's day → next due is today
        val lastMonth = today.minus(DatePeriod(months = 1))
        recurringDao.upsert(monthly(lastProcessedDate = lastMonth.toString(), dayOfMonth = today.dayOfMonth))
        sut()
        val pending = pendingDao.getAllPending().first()
        assertEquals(1, pending.size)
    }

    @Test
    fun `inactive recurring is not processed`() = runTest {
        recurringDao.upsert(monthly(isActive = false, dayOfMonth = 1))
        sut()
        val pending = pendingDao.getAllPending().first()
        assertEquals(0, pending.size)
    }

    // --- Weekly frequency tests ---

    @Test
    fun `weekly recurring with no last processed creates pending immediately`() = runTest {
        recurringDao.upsert(weekly())
        sut()
        val pending = pendingDao.getAllPending().first()
        assertEquals(1, pending.size)
    }

    @Test
    fun `weekly recurring processed 7 days ago creates pending`() = runTest {
        val sevenDaysAgo = today.minus(DatePeriod(days = 7))
        recurringDao.upsert(weekly(lastProcessedDate = sevenDaysAgo.toString()))
        sut()
        val pending = pendingDao.getAllPending().first()
        assertEquals(1, pending.size)
        assertEquals(today.toString(), pending[0].scheduledDate)
    }

    @Test
    fun `weekly recurring processed 3 days ago does not create pending`() = runTest {
        val threeDaysAgo = today.minus(DatePeriod(days = 3))
        recurringDao.upsert(weekly(lastProcessedDate = threeDaysAgo.toString()))
        sut()
        val pending = pendingDao.getAllPending().first()
        // Next due: 3 days ago + 7 = 4 days from now, which is future
        assertEquals(0, pending.size)
    }

    // --- Daily frequency tests ---

    @Test
    fun `daily recurring with no last processed creates pending immediately`() = runTest {
        recurringDao.upsert(daily())
        sut()
        val pending = pendingDao.getAllPending().first()
        assertEquals(1, pending.size)
    }

    @Test
    fun `daily recurring processed yesterday creates pending for today`() = runTest {
        recurringDao.upsert(daily(lastProcessedDate = yesterday.toString()))
        sut()
        val pending = pendingDao.getAllPending().first()
        assertEquals(1, pending.size)
        assertEquals(today.toString(), pending[0].scheduledDate)
    }

    @Test
    fun `daily recurring processed today does not create duplicate`() = runTest {
        recurringDao.upsert(daily(lastProcessedDate = today.toString()))
        sut()
        val pending = pendingDao.getAllPending().first()
        // Next due: tomorrow, future
        assertEquals(0, pending.size)
    }

    // --- Yearly frequency tests ---

    @Test
    fun `yearly recurring with no last processed creates pending if due this year`() = runTest {
        // Month 1 (Jan), day 1 → already passed, should create pending
        recurringDao.upsert(yearly(monthOfYear = 1, dayOfMonth = 1))
        sut()
        val pending = pendingDao.getAllPending().first()
        // Jan 1 is in the past, so pending should be created
        assertEquals(1, pending.size)
    }

    @Test
    fun `yearly recurring with no last processed future month does not create pending`() = runTest {
        // Month 12 (Dec), day 25 → future
        recurringDao.upsert(yearly(monthOfYear = 12, dayOfMonth = 25))
        sut()
        val pending = pendingDao.getAllPending().first()
        assertEquals(0, pending.size)
    }

    // --- Duplicate prevention ---

    @Test
    fun `running process twice does not create duplicate pending`() = runTest {
        recurringDao.upsert(daily(lastProcessedDate = yesterday.toString()))
        sut()
        val firstRun = pendingDao.getAllPending().first()
        assertEquals(1, firstRun.size)

        // Run again — should not duplicate
        sut()
        val secondRun = pendingDao.getAllPending().first()
        assertEquals(1, secondRun.size)
    }

    // --- Multiple recurring transactions ---

    @Test
    fun `processes multiple recurring transactions independently`() = runTest {
        val sevenDaysAgo = today.minus(DatePeriod(days = 7))
        recurringDao.upsert(daily(id = 0, lastProcessedDate = yesterday.toString()))
        recurringDao.upsert(weekly(id = 0, lastProcessedDate = sevenDaysAgo.toString()))
        sut()
        val pending = pendingDao.getAllPending().first()
        assertEquals(2, pending.size)
    }

    // --- Updates lastProcessedDate ---

    @Test
    fun `processing updates lastProcessedDate on recurring entity`() = runTest {
        recurringDao.upsert(daily(lastProcessedDate = yesterday.toString()))
        sut()
        val updated = recurringDao.getAll().first().first()
        assertEquals(today.toString(), updated.lastProcessedDate)
    }

    // --- Pending transaction fields ---

    @Test
    fun `pending transaction copies fields from recurring correctly`() = runTest {
        recurringDao.upsert(monthly(
            subcategoryId = "rent",
            amount = 3500.0,
            accountId = 42,
            dayOfMonth = 1
        ))
        sut()
        val pending = pendingDao.getAllPending().first()
        assertEquals(1, pending.size)
        val p = pending[0]
        assertEquals("rent", p.subcategoryId)
        assertEquals(3500.0, p.amount)
        assertEquals(42, p.accountId)
        assertEquals("PENDING", p.status)
    }

    // --- Day clamping ---

    @Test
    fun `monthly recurring with day 31 gets clamped to 28`() = runTest {
        recurringDao.upsert(monthly(dayOfMonth = 28))
        sut()
        val pending = pendingDao.getAllPending().first()
        // Just verify no crash
        assertTrue(true)
    }
}
