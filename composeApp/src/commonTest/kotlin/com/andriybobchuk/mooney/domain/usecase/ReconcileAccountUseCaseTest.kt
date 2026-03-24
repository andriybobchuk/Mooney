package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.usecase.AddAccountUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.CreateReconciliationUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.ReconcileAccountUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.ReconciliationDifference
import com.andriybobchuk.mooney.testutil.FakeCoreRepository
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReconcileAccountUseCaseTest {

    private lateinit var repository: FakeCoreRepository
    private lateinit var createReconciliationUseCase: CreateReconciliationUseCase
    private lateinit var addAccountUseCase: AddAccountUseCase
    private lateinit var sut: ReconcileAccountUseCase

    @BeforeTest
    fun setup() {
        repository = FakeCoreRepository()
        createReconciliationUseCase = CreateReconciliationUseCase(repository)
        addAccountUseCase = AddAccountUseCase(repository)
        sut = ReconcileAccountUseCase(repository, createReconciliationUseCase, addAccountUseCase)
    }

    @Test
    fun `reconcile creates transaction and saves account`() = runTest {
        val originalAccount = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(originalAccount)

        val targetAccount = originalAccount.copy(amount = 1200.0)
        val diff = ReconciliationDifference(
            account = originalAccount,
            oldAmount = 1000.0,
            newAmount = 1200.0,
            difference = 200.0,
            isGain = true
        )

        sut(diff, targetAccount)

        val transactions = repository.getAllTransactions().first().filterNotNull()
        assertTrue(transactions.isNotEmpty(), "Expected a reconciliation transaction to be created")

        val savedAccount = repository.getAccountById(1)
        assertNotNull(savedAccount)
        assertEquals(1200.0, savedAccount.amount, 0.001)
    }

    @Test
    fun `account amount matches target after reconciliation`() = runTest {
        val originalAccount = TestFixtures.account(id = 5, amount = 500.0)
        repository.upsertAccount(originalAccount)

        val targetAmount = 750.0
        val targetAccount = originalAccount.copy(amount = targetAmount)
        val diff = ReconciliationDifference(
            account = originalAccount,
            oldAmount = 500.0,
            newAmount = targetAmount,
            difference = 250.0,
            isGain = true
        )

        sut(diff, targetAccount)

        val savedAccount = repository.getAccountById(5)
        assertNotNull(savedAccount)
        assertEquals(targetAmount, savedAccount.amount, 0.001)
    }

    @Test
    fun `positive difference creates gain reconciliation transaction`() = runTest {
        val originalAccount = TestFixtures.account(id = 2, amount = 300.0)
        repository.upsertAccount(originalAccount)

        val targetAccount = originalAccount.copy(amount = 400.0)
        val diff = ReconciliationDifference(
            account = originalAccount,
            oldAmount = 300.0,
            newAmount = 400.0,
            difference = 100.0,
            isGain = true
        )

        sut(diff, targetAccount)

        val transactions = repository.getAllTransactions().first().filterNotNull()
        val reconciliationTx = transactions.firstOrNull()
        assertNotNull(reconciliationTx)
        assertEquals(100.0, reconciliationTx.amount, 0.001)
        assertEquals("positive_reconciliation", reconciliationTx.subcategory.id)
    }

    @Test
    fun `negative difference creates loss reconciliation transaction`() = runTest {
        val originalAccount = TestFixtures.account(id = 3, amount = 600.0)
        repository.upsertAccount(originalAccount)

        val targetAccount = originalAccount.copy(amount = 500.0)
        val diff = ReconciliationDifference(
            account = originalAccount,
            oldAmount = 600.0,
            newAmount = 500.0,
            difference = -100.0,
            isGain = false
        )

        sut(diff, targetAccount)

        val transactions = repository.getAllTransactions().first().filterNotNull()
        val reconciliationTx = transactions.firstOrNull()
        assertNotNull(reconciliationTx)
        assertEquals(100.0, reconciliationTx.amount, 0.001)
        assertEquals("reconciliation", reconciliationTx.subcategory.id)
    }

    @Test
    fun `reconcile updates account title and currency too`() = runTest {
        val originalAccount = TestFixtures.account(id = 7, title = "Old Name", currency = Currency.PLN)
        repository.upsertAccount(originalAccount)

        val targetAccount = originalAccount.copy(title = "New Name", currency = Currency.USD, amount = 1000.0)
        val diff = ReconciliationDifference(
            account = originalAccount,
            oldAmount = originalAccount.amount,
            newAmount = 1000.0,
            difference = 1000.0 - originalAccount.amount,
            isGain = true
        )

        sut(diff, targetAccount)

        val savedAccount = repository.getAccountById(7)
        assertNotNull(savedAccount)
        assertEquals("New Name", savedAccount.title)
        assertEquals(Currency.USD, savedAccount.currency)
    }
}
