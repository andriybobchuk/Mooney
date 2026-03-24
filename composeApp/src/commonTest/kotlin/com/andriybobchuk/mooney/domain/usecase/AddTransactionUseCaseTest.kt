package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.usecase.AddTransactionUseCase
import com.andriybobchuk.mooney.testutil.FakeCoreRepository
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AddTransactionUseCaseTest {

    private lateinit var repository: FakeCoreRepository
    private lateinit var sut: AddTransactionUseCase

    @BeforeTest
    fun setup() {
        repository = FakeCoreRepository()
        sut = AddTransactionUseCase(repository)
    }

    @Test
    fun `adding expense reduces account balance`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)

        val transaction = TestFixtures.transaction(
            id = 0,
            subcategory = TestFixtures.groceries,
            amount = 150.0,
            account = account
        )

        sut(transaction)

        val updatedAccount = repository.getAccountById(1)
        assertNotNull(updatedAccount)
        assertEquals(850.0, updatedAccount.amount, 0.001)
    }

    @Test
    fun `adding income increases account balance`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 500.0)
        repository.upsertAccount(account)

        val transaction = TestFixtures.transaction(
            id = 0,
            subcategory = TestFixtures.salary,
            amount = 3000.0,
            account = account
        )

        sut(transaction)

        val updatedAccount = repository.getAccountById(1)
        assertNotNull(updatedAccount)
        assertEquals(3500.0, updatedAccount.amount, 0.001)
    }

    @Test
    fun `transaction is saved in repository after invoke`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)

        val transaction = TestFixtures.transaction(
            id = 0,
            subcategory = TestFixtures.groceries,
            amount = 50.0,
            account = account
        )

        sut(transaction)

        val allTransactions = repository.getAllTransactions().first().filterNotNull()
        assertEquals(1, allTransactions.size)
        assertEquals(50.0, allTransactions.first().amount, 0.001)
    }

    @Test
    fun `updating existing expense reverses old effect and applies new one`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)

        // Add original expense of 100
        val original = TestFixtures.transaction(id = 0, subcategory = TestFixtures.groceries, amount = 100.0, account = account)
        sut(original)
        // Balance is now 900

        // Retrieve the saved transaction with its assigned id
        val saved = repository.getAllTransactions().first().filterNotNull().first()

        // Update expense amount to 200
        val updated = saved.copy(amount = 200.0)
        sut(updated)

        // Reverse 100 (back to 900), then apply 200 -> 700
        val finalAccount = repository.getAccountById(1)
        assertNotNull(finalAccount)
        assertEquals(800.0, finalAccount.amount, 0.001)
    }

    @Test
    fun `adding expense with zero amount does not change account balance`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)

        val transaction = TestFixtures.transaction(
            id = 0,
            subcategory = TestFixtures.groceries,
            amount = 0.0,
            account = account
        )

        sut(transaction)

        val updatedAccount = repository.getAccountById(1)
        assertNotNull(updatedAccount)
        assertEquals(1000.0, updatedAccount.amount, 0.001)
    }
}
