package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.usecase.CreateReconciliationUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.ReconciliationDifference
import com.andriybobchuk.mooney.testutil.FakeCoreRepository
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateReconciliationUseCaseTest {

    private val repository = FakeCoreRepository()
    private val sut = CreateReconciliationUseCase(repository)

    @Test
    fun `detectReconciliationDifference returns null for new account with id zero`() = runTest {
        val result = sut.detectReconciliationDifference(accountId = 0, newAmount = 500.0)

        assertNull(result)
    }

    @Test
    fun `detectReconciliationDifference returns null when account not found in repository`() = runTest {
        // No accounts seeded — id 99 does not exist
        val result = sut.detectReconciliationDifference(accountId = 99, newAmount = 500.0)

        assertNull(result)
    }

    @Test
    fun `detectReconciliationDifference returns null when difference is below threshold`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)

        // Difference = 1000.005 - 1000.0 = 0.005, below the 0.01 threshold
        val result = sut.detectReconciliationDifference(accountId = 1, newAmount = 1000.005)

        assertNull(result)
    }

    @Test
    fun `detectReconciliationDifference returns null when difference is exactly zero`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)

        val result = sut.detectReconciliationDifference(accountId = 1, newAmount = 1000.0)

        assertNull(result)
    }

    @Test
    fun `detectReconciliationDifference returns positive difference correctly`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)

        val result = sut.detectReconciliationDifference(accountId = 1, newAmount = 1200.0)

        assertNotNull(result)
        assertEquals(1000.0, result.oldAmount, 0.001)
        assertEquals(1200.0, result.newAmount, 0.001)
        assertEquals(200.0, result.difference, 0.001)
        assertTrue(result.isGain)
    }

    @Test
    fun `detectReconciliationDifference returns negative difference correctly`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)

        val result = sut.detectReconciliationDifference(accountId = 1, newAmount = 750.0)

        assertNotNull(result)
        assertEquals(1000.0, result.oldAmount, 0.001)
        assertEquals(750.0, result.newAmount, 0.001)
        assertEquals(-250.0, result.difference, 0.001)
        assertFalse(result.isGain)
    }

    @Test
    fun `shouldShowDialog is true for a meaningful difference`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 500.0)
        repository.upsertAccount(account)

        val result = sut.detectReconciliationDifference(accountId = 1, newAmount = 600.0)

        assertNotNull(result)
        assertTrue(result.shouldShowDialog)
    }

    @Test
    fun `shouldShowDialog computed correctly at minimum threshold boundary`() {
        val diff = ReconciliationDifference(
            account = TestFixtures.account(),
            oldAmount = 100.0,
            newAmount = 100.01,
            difference = 0.01,
            isGain = true
        )

        assertTrue(diff.shouldShowDialog)
    }

    @Test
    fun `shouldShowDialog is false below minimum threshold`() {
        val diff = ReconciliationDifference(
            account = TestFixtures.account(),
            oldAmount = 100.0,
            newAmount = 100.009,
            difference = 0.009,
            isGain = true
        )

        assertFalse(diff.shouldShowDialog)
    }

    @Test
    fun `createReconciliationTransaction saves transaction in repository for a gain`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 800.0)
        repository.upsertAccount(account)

        val diff = ReconciliationDifference(
            account = account,
            oldAmount = 800.0,
            newAmount = 1000.0,
            difference = 200.0,
            isGain = true
        )

        sut.createReconciliationTransaction(diff)

        val list = repository.getAllTransactions().first().filterNotNull()
        assertEquals(1, list.size)
        val tx = list.first()
        assertEquals(200.0, tx.amount, 0.001)
        assertEquals("positive_reconciliation", tx.subcategory.id)
        assertEquals(1000.0, tx.account.amount, 0.001)
    }

    @Test
    fun `createReconciliationTransaction saves transaction in repository for a loss`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)

        val diff = ReconciliationDifference(
            account = account,
            oldAmount = 1000.0,
            newAmount = 700.0,
            difference = -300.0,
            isGain = false
        )

        sut.createReconciliationTransaction(diff)

        val list = repository.getAllTransactions().first().filterNotNull()
        assertEquals(1, list.size)
        val tx = list.first()
        assertEquals(300.0, tx.amount, 0.001)
        assertEquals("reconciliation", tx.subcategory.id)
        assertEquals(700.0, tx.account.amount, 0.001)
    }
}
