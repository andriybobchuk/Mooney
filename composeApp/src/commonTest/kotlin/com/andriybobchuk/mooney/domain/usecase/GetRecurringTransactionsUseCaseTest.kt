package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.core.data.database.RecurringTransactionEntity
import com.andriybobchuk.mooney.mooney.domain.RecurringFrequency
import com.andriybobchuk.mooney.mooney.domain.usecase.GetAccountsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetCategoriesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetRecurringTransactionsUseCase
import com.andriybobchuk.mooney.testutil.FakeCoreRepository
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

class GetRecurringTransactionsUseCaseTest {

    private lateinit var repository: FakeCoreRepository
    private lateinit var recurringDao: FakeRecurringTransactionDao
    private lateinit var sut: GetRecurringTransactionsUseCase

    @BeforeTest
    fun setup() {
        repository = FakeCoreRepository()
        recurringDao = FakeRecurringTransactionDao()
        val getAccountsUseCase = GetAccountsUseCase(repository)
        val getCategoriesUseCase = GetCategoriesUseCase(repository)
        sut = GetRecurringTransactionsUseCase(recurringDao, getAccountsUseCase, getCategoriesUseCase)
    }

    private fun entity(
        id: Int = 0,
        subcategoryId: String = "groceries",
        accountId: Int = 1,
        frequency: String = "MONTHLY",
        dayOfMonth: Int = 15,
        weekDay: Int? = null,
        monthOfYear: Int? = null
    ) = RecurringTransactionEntity(
        id = id,
        title = "Test Recurring",
        subcategoryId = subcategoryId,
        amount = 200.0,
        accountId = accountId,
        dayOfMonth = dayOfMonth,
        frequency = frequency,
        weekDay = weekDay,
        monthOfYear = monthOfYear,
        isActive = true,
        createdDate = "2025-01-01"
    )

    @Test
    fun `returns empty list when no recurring transactions`() = runTest {
        val result = sut().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `maps entity to domain model with account and category`() = runTest {
        val account = TestFixtures.account(id = 1, title = "Main")
        repository.upsertAccount(account)
        recurringDao.upsert(entity(subcategoryId = "groceries", accountId = 1))

        val result = sut().first()
        assertEquals(1, result.size)

        val recurring = result[0]
        assertEquals("Test Recurring", recurring.title)
        assertEquals(200.0, recurring.amount)
        assertNotNull(recurring.account)
        assertEquals("Main", recurring.account?.title)
        assertNotNull(recurring.subcategory)
        assertEquals("groceries", recurring.subcategory?.id)
    }

    @Test
    fun `maps monthly frequency correctly`() = runTest {
        repository.upsertAccount(TestFixtures.account(id = 1))
        recurringDao.upsert(entity(frequency = "MONTHLY", dayOfMonth = 20))

        val result = sut().first()
        assertEquals(RecurringFrequency.MONTHLY, result[0].schedule.frequency)
        assertEquals(20, result[0].schedule.dayOfMonth)
    }

    @Test
    fun `maps weekly frequency with weekDay`() = runTest {
        repository.upsertAccount(TestFixtures.account(id = 1))
        recurringDao.upsert(entity(frequency = "WEEKLY", weekDay = 3))

        val result = sut().first()
        assertEquals(RecurringFrequency.WEEKLY, result[0].schedule.frequency)
        assertEquals(3, result[0].schedule.weekDay)
    }

    @Test
    fun `maps yearly frequency with monthOfYear`() = runTest {
        repository.upsertAccount(TestFixtures.account(id = 1))
        recurringDao.upsert(entity(frequency = "YEARLY", dayOfMonth = 25, monthOfYear = 12))

        val result = sut().first()
        assertEquals(RecurringFrequency.YEARLY, result[0].schedule.frequency)
        assertEquals(25, result[0].schedule.dayOfMonth)
        assertEquals(12, result[0].schedule.monthOfYear)
    }

    @Test
    fun `unknown frequency defaults to MONTHLY`() = runTest {
        repository.upsertAccount(TestFixtures.account(id = 1))
        recurringDao.upsert(entity(frequency = "BIWEEKLY"))

        val result = sut().first()
        assertEquals(RecurringFrequency.MONTHLY, result[0].schedule.frequency)
    }

    @Test
    fun `missing account returns null account in domain model`() = runTest {
        // accountId = 99 does not exist in repository
        recurringDao.upsert(entity(accountId = 99))

        val result = sut().first()
        assertEquals(1, result.size)
        assertNull(result[0].account)
    }

    @Test
    fun `missing category returns null subcategory`() = runTest {
        repository.upsertAccount(TestFixtures.account(id = 1))
        recurringDao.upsert(entity(subcategoryId = "nonexistent_category"))

        val result = sut().first()
        assertEquals(1, result.size)
        assertNull(result[0].subcategory)
    }

    @Test
    fun `returns multiple recurring transactions`() = runTest {
        repository.upsertAccount(TestFixtures.account(id = 1))
        repository.upsertAccount(TestFixtures.account(id = 2, title = "Savings"))
        recurringDao.upsert(entity(accountId = 1))
        recurringDao.upsert(entity(accountId = 2))

        val result = sut().first()
        assertEquals(2, result.size)
    }
}
