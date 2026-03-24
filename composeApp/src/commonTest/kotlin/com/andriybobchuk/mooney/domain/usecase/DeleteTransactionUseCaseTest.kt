package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.usecase.AddTransactionUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.DeleteTransactionUseCase
import com.andriybobchuk.mooney.testutil.FakeCoreRepository
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeleteTransactionUseCaseTest {

    private lateinit var repository: FakeCoreRepository
    private lateinit var addUseCase: AddTransactionUseCase
    private lateinit var sut: DeleteTransactionUseCase

    @BeforeTest
    fun setup() {
        repository = FakeCoreRepository()
        addUseCase = AddTransactionUseCase(repository)
        sut = DeleteTransactionUseCase(repository)
    }

    @Test
    fun `deleting expense restores account balance`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)

        val transaction = TestFixtures.transaction(
            id = 0,
            subcategory = TestFixtures.groceries,
            amount = 200.0,
            account = account
        )
        addUseCase(transaction)
        // Balance is now 800

        val savedTx = repository.getAllTransactions().first().filterNotNull().first()
        sut(savedTx.id)

        val updatedAccount = repository.getAccountById(1)
        assertNotNull(updatedAccount)
        assertEquals(1000.0, updatedAccount.amount, 0.001)
    }

    @Test
    fun `deleting income reduces account balance`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 500.0)
        repository.upsertAccount(account)

        val transaction = TestFixtures.transaction(
            id = 0,
            subcategory = TestFixtures.salary,
            amount = 3000.0,
            account = account
        )
        addUseCase(transaction)
        // Balance is now 3500

        val savedTx = repository.getAllTransactions().first().filterNotNull().first()
        sut(savedTx.id)

        val updatedAccount = repository.getAccountById(1)
        assertNotNull(updatedAccount)
        assertEquals(500.0, updatedAccount.amount, 0.001)
    }

    @Test
    fun `deleted transaction is removed from repository`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)

        val transaction = TestFixtures.transaction(
            id = 0,
            subcategory = TestFixtures.groceries,
            amount = 100.0,
            account = account
        )
        addUseCase(transaction)

        val savedTx = repository.getAllTransactions().first().filterNotNull().first()
        sut(savedTx.id)

        val allTransactions = repository.getAllTransactions().first().filterNotNull()
        assertTrue(allTransactions.isEmpty())
    }

    @Test
    fun `deleting non-existent transaction id does nothing`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)

        sut(999)

        val updatedAccount = repository.getAccountById(1)
        assertNotNull(updatedAccount)
        assertEquals(1000.0, updatedAccount.amount, 0.001)

        val allTransactions = repository.getAllTransactions().first().filterNotNull()
        assertTrue(allTransactions.isEmpty())
    }

    @Test
    fun `deleting one of multiple transactions only affects the target`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)

        addUseCase(TestFixtures.transaction(id = 0, subcategory = TestFixtures.groceries, amount = 100.0, account = account))
        // Balance: 900
        val currentAccount = repository.getAccountById(1)!!
        addUseCase(TestFixtures.transaction(id = 0, subcategory = TestFixtures.groceries, amount = 50.0, account = currentAccount))
        // Balance: 850

        val transactions = repository.getAllTransactions().first().filterNotNull()
        assertEquals(2, transactions.size)

        sut(transactions.first().id)

        val remainingTransactions = repository.getAllTransactions().first().filterNotNull()
        assertEquals(1, remainingTransactions.size)
    }
}
