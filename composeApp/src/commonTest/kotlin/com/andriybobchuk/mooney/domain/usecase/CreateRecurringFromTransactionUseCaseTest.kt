package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.RecurringFrequency
import com.andriybobchuk.mooney.mooney.domain.RecurringSchedule
import com.andriybobchuk.mooney.mooney.domain.usecase.AddTransactionUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.CreateRecurringFromTransactionUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.SaveRecurringTransactionUseCase
import com.andriybobchuk.mooney.testutil.FakeCoreRepository
import com.andriybobchuk.mooney.testutil.FakePendingTransactionDao
import com.andriybobchuk.mooney.testutil.FakeRecurringTransactionDao
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateRecurringFromTransactionUseCaseTest {

    private lateinit var repository: FakeCoreRepository
    private lateinit var recurringDao: FakeRecurringTransactionDao
    private lateinit var pendingDao: FakePendingTransactionDao
    private lateinit var sut: CreateRecurringFromTransactionUseCase

    @BeforeTest
    fun setup() {
        repository = FakeCoreRepository()
        recurringDao = FakeRecurringTransactionDao()
        pendingDao = FakePendingTransactionDao()
        val addTransactionUseCase = AddTransactionUseCase(repository)
        val saveRecurringUseCase = SaveRecurringTransactionUseCase(recurringDao, pendingDao)
        sut = CreateRecurringFromTransactionUseCase(addTransactionUseCase, saveRecurringUseCase)
    }

    @Test
    fun `creates both transaction and recurring entry`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 5000.0)
        repository.upsertAccount(account)

        val transaction = TestFixtures.transaction(
            subcategory = TestFixtures.groceries,
            amount = 200.0,
            account = account
        )
        val schedule = RecurringSchedule(
            frequency = RecurringFrequency.MONTHLY,
            dayOfMonth = 15
        )

        sut(transaction, schedule)

        // Transaction was created
        val transactions = repository.getAllTransactions().first().filterNotNull()
        assertEquals(1, transactions.size)
        assertEquals(200.0, transactions[0].amount)

        // Recurring entry was created
        val recurring = recurringDao.getAll().first()
        assertEquals(1, recurring.size)
        assertEquals("Groceries & Household", recurring[0].title)
        assertEquals("groceries", recurring[0].subcategoryId)
        assertEquals(200.0, recurring[0].amount)
        assertEquals(1, recurring[0].accountId)
        assertEquals("MONTHLY", recurring[0].frequency)
        assertEquals(15, recurring[0].dayOfMonth)
        assertTrue(recurring[0].isActive)
    }

    @Test
    fun `creates weekly recurring with correct weekDay`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 5000.0)
        repository.upsertAccount(account)

        val transaction = TestFixtures.transaction(account = account)
        val schedule = RecurringSchedule(
            frequency = RecurringFrequency.WEEKLY,
            dayOfMonth = 1,
            weekDay = 3 // Thursday
        )

        sut(transaction, schedule)

        val recurring = recurringDao.getAll().first()
        assertEquals("WEEKLY", recurring[0].frequency)
        assertEquals(3, recurring[0].weekDay)
    }

    @Test
    fun `creates yearly recurring with correct monthOfYear`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 5000.0)
        repository.upsertAccount(account)

        val transaction = TestFixtures.transaction(account = account)
        val schedule = RecurringSchedule(
            frequency = RecurringFrequency.YEARLY,
            dayOfMonth = 25,
            monthOfYear = 12
        )

        sut(transaction, schedule)

        val recurring = recurringDao.getAll().first()
        assertEquals("YEARLY", recurring[0].frequency)
        assertEquals(12, recurring[0].monthOfYear)
        assertEquals(25, recurring[0].dayOfMonth)
    }

    @Test
    fun `transaction reduces account balance`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)

        val transaction = TestFixtures.transaction(
            subcategory = TestFixtures.groceries,
            amount = 300.0,
            account = account
        )
        val schedule = RecurringSchedule(frequency = RecurringFrequency.MONTHLY, dayOfMonth = 1)

        sut(transaction, schedule)

        val updatedAccount = repository.getAccountById(1)
        assertEquals(700.0, updatedAccount?.amount)
    }
}
