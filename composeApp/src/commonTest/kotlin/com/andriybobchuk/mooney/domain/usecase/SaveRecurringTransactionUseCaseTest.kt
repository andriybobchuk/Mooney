package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.core.data.database.RecurringTransactionEntity
import com.andriybobchuk.mooney.mooney.domain.RecurringFrequency
import com.andriybobchuk.mooney.mooney.domain.RecurringSchedule
import com.andriybobchuk.mooney.mooney.domain.RecurringTransaction
import com.andriybobchuk.mooney.mooney.domain.usecase.SaveRecurringTransactionUseCase
import com.andriybobchuk.mooney.testutil.FakeRecurringTransactionDao
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveRecurringTransactionUseCaseTest {

    private lateinit var recurringDao: FakeRecurringTransactionDao
    private lateinit var sut: SaveRecurringTransactionUseCase

    @BeforeTest
    fun setup() {
        recurringDao = FakeRecurringTransactionDao()
        sut = SaveRecurringTransactionUseCase(recurringDao)
    }

    private fun recurringTransaction(
        id: Int = 0,
        title: String = "Groceries",
        frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
        dayOfMonth: Int = 15,
        weekDay: Int? = null,
        monthOfYear: Int? = null
    ) = RecurringTransaction(
        id = id,
        title = title,
        subcategory = TestFixtures.groceries,
        amount = 200.0,
        account = TestFixtures.account(),
        schedule = RecurringSchedule(
            frequency = frequency,
            dayOfMonth = dayOfMonth,
            weekDay = weekDay,
            monthOfYear = monthOfYear
        ),
        isActive = true
    )

    @Test
    fun `new recurring gets auto-generated id`() = runTest {
        sut(recurringTransaction(id = 0))
        val saved = recurringDao.getAll().first()
        assertEquals(1, saved.size)
        assertTrue(saved[0].id > 0)
    }

    @Test
    fun `new recurring sets createdDate to today`() = runTest {
        sut(recurringTransaction(id = 0))
        val saved = recurringDao.getAll().first().first()
        assertNotNull(saved.createdDate)
        // Just verify it's a valid date string (not empty)
        assertTrue(saved.createdDate.isNotEmpty())
    }

    @Test
    fun `new recurring has null lastProcessedDate`() = runTest {
        sut(recurringTransaction(id = 0))
        val saved = recurringDao.getAll().first().first()
        assertNull(saved.lastProcessedDate)
    }

    @Test
    fun `editing preserves original createdDate`() = runTest {
        // Insert original with known createdDate
        recurringDao.upsert(
            RecurringTransactionEntity(
                id = 5,
                title = "Original",
                subcategoryId = "groceries",
                amount = 100.0,
                accountId = 1,
                dayOfMonth = 10,
                frequency = "MONTHLY",
                isActive = true,
                createdDate = "2025-01-01",
                lastProcessedDate = "2025-06-10"
            )
        )

        // Edit it
        sut(recurringTransaction(id = 5, title = "Updated"))

        val updated = recurringDao.getById(5)
        assertNotNull(updated)
        assertEquals("Updated", updated.title)
        assertEquals("2025-01-01", updated.createdDate) // preserved
        assertEquals("2025-06-10", updated.lastProcessedDate) // preserved
    }

    @Test
    fun `saves all schedule fields correctly`() = runTest {
        sut(recurringTransaction(
            frequency = RecurringFrequency.YEARLY,
            dayOfMonth = 25,
            monthOfYear = 12
        ))
        val saved = recurringDao.getAll().first().first()
        assertEquals("YEARLY", saved.frequency)
        assertEquals(25, saved.dayOfMonth)
        assertEquals(12, saved.monthOfYear)
    }

    @Test
    fun `weekly saves weekDay`() = runTest {
        sut(recurringTransaction(
            frequency = RecurringFrequency.WEEKLY,
            weekDay = 4 // Friday
        ))
        val saved = recurringDao.getAll().first().first()
        assertEquals("WEEKLY", saved.frequency)
        assertEquals(4, saved.weekDay)
    }

    @Test
    fun `saves subcategory id and account id`() = runTest {
        sut(recurringTransaction())
        val saved = recurringDao.getAll().first().first()
        assertEquals("groceries", saved.subcategoryId)
        assertEquals(1, saved.accountId)
    }

    @Test
    fun `null subcategory saves empty string`() = runTest {
        val recurring = RecurringTransaction(
            id = 0,
            title = "No Category",
            subcategory = null,
            amount = 50.0,
            account = TestFixtures.account(),
            schedule = RecurringSchedule(frequency = RecurringFrequency.MONTHLY, dayOfMonth = 1),
            isActive = true
        )
        sut(recurring)
        val saved = recurringDao.getAll().first().first()
        assertEquals("", saved.subcategoryId)
    }

    @Test
    fun `null account saves id 0`() = runTest {
        val recurring = RecurringTransaction(
            id = 0,
            title = "No Account",
            subcategory = TestFixtures.groceries,
            amount = 50.0,
            account = null,
            schedule = RecurringSchedule(frequency = RecurringFrequency.MONTHLY, dayOfMonth = 1),
            isActive = true
        )
        sut(recurring)
        val saved = recurringDao.getAll().first().first()
        assertEquals(0, saved.accountId)
    }
}
