package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.core.data.database.PendingTransactionEntity
import com.andriybobchuk.mooney.mooney.domain.usecase.AcceptPendingTransactionUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.AddTransactionUseCase
import com.andriybobchuk.mooney.testutil.FakeCoreRepository
import com.andriybobchuk.mooney.testutil.FakePendingTransactionDao
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AcceptPendingTransactionUseCaseTest {

    private lateinit var repository: FakeCoreRepository
    private lateinit var pendingDao: FakePendingTransactionDao
    private lateinit var sut: AcceptPendingTransactionUseCase

    @BeforeTest
    fun setup() {
        repository = FakeCoreRepository()
        pendingDao = FakePendingTransactionDao()
        val addTransactionUseCase = AddTransactionUseCase(repository)
        sut = AcceptPendingTransactionUseCase(pendingDao, addTransactionUseCase)
    }

    private fun pending(
        id: Int = 1,
        subcategoryId: String = "groceries",
        amount: Double = 150.0,
        accountId: Int = 1,
        scheduledDate: String = "2026-03-15"
    ) = PendingTransactionEntity(
        id = id,
        recurringTransactionId = 10,
        subcategoryId = subcategoryId,
        amount = amount,
        accountId = accountId,
        scheduledDate = scheduledDate,
        status = "PENDING",
        createdDate = "2026-03-01"
    )

    @Test
    fun `accepting pending creates real transaction`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)
        val p = pending()
        pendingDao.upsert(p)

        sut(p, account, TestFixtures.groceries)

        val transactions = repository.getAllTransactions().first().filterNotNull()
        assertEquals(1, transactions.size)
        assertEquals(150.0, transactions[0].amount)
        assertEquals("groceries", transactions[0].subcategory.id)
    }

    @Test
    fun `accepting pending uses scheduled date as transaction date`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)
        val p = pending(scheduledDate = "2026-02-20")
        pendingDao.upsert(p)

        sut(p, account, TestFixtures.groceries)

        val tx = repository.getAllTransactions().first().filterNotNull().first()
        assertEquals(2026, tx.date.year)
        assertEquals(2, tx.date.monthNumber)
        assertEquals(20, tx.date.dayOfMonth)
    }

    @Test
    fun `accepting pending marks status as ACCEPTED`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)
        val p = pending()
        pendingDao.upsert(p)

        sut(p, account, TestFixtures.groceries)

        val all = pendingDao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("ACCEPTED", all[0].status)
    }

    @Test
    fun `accepting pending reduces account balance for expense`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)
        val p = pending(amount = 200.0)
        pendingDao.upsert(p)

        sut(p, account, TestFixtures.groceries)

        val updatedAccount = repository.getAccountById(1)
        assertEquals(800.0, updatedAccount?.amount)
    }

    @Test
    fun `accepting pending increases account balance for income`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 500.0)
        repository.upsertAccount(account)
        val p = pending(subcategoryId = "salary", amount = 3000.0)
        pendingDao.upsert(p)

        sut(p, account, TestFixtures.salary)

        val updatedAccount = repository.getAccountById(1)
        assertEquals(3500.0, updatedAccount?.amount)
    }

    @Test
    fun `accepted pending no longer appears in getAllPending`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)
        val p = pending()
        pendingDao.upsert(p)

        sut(p, account, TestFixtures.groceries)

        val pendingList = pendingDao.getAllPending().first()
        assertEquals(0, pendingList.size)
    }
}
