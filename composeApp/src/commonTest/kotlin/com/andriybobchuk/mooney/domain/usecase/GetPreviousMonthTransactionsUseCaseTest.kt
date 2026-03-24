package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.usecase.GetPreviousMonthTransactionsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetTransactionsUseCase
import com.andriybobchuk.mooney.testutil.FakeCoreRepository
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetPreviousMonthTransactionsUseCaseTest {

    private lateinit var repository: FakeCoreRepository
    private lateinit var sut: GetPreviousMonthTransactionsUseCase

    @BeforeTest
    fun setup() {
        repository = FakeCoreRepository()
        sut = GetPreviousMonthTransactionsUseCase(GetTransactionsUseCase(repository))
    }

    @Test
    fun `returns only transactions from the previous month`() = runTest {
        val account = TestFixtures.account(id = 1)
        repository.upsertAccount(account)

        // Previous month: March 2024 (current month: April 2024)
        repository.upsertTransaction(TestFixtures.transaction(id = 0, account = account, date = LocalDate(2024, 3, 1)))
        repository.upsertTransaction(TestFixtures.transaction(id = 0, account = account, date = LocalDate(2024, 3, 15)))
        repository.upsertTransaction(TestFixtures.transaction(id = 0, account = account, date = LocalDate(2024, 3, 31)))

        val result = sut(MonthKey(2024, 4))

        assertEquals(3, result.size)
        result.forEach { tx ->
            assertEquals(2024, tx.date.year)
            assertEquals(3, tx.date.monthNumber)
        }
    }

    @Test
    fun `excludes transactions from the current month`() = runTest {
        val account = TestFixtures.account(id = 1)
        repository.upsertAccount(account)

        // Current month April — should be excluded
        repository.upsertTransaction(TestFixtures.transaction(id = 0, account = account, date = LocalDate(2024, 4, 5)))
        // Previous month March — should be included
        repository.upsertTransaction(TestFixtures.transaction(id = 0, account = account, date = LocalDate(2024, 3, 20)))

        val result = sut(MonthKey(2024, 4))

        assertEquals(1, result.size)
        assertEquals(3, result.first().date.monthNumber)
    }

    @Test
    fun `excludes transactions from two or more months ago`() = runTest {
        val account = TestFixtures.account(id = 1)
        repository.upsertAccount(account)

        // February — two months before April, should be excluded
        repository.upsertTransaction(TestFixtures.transaction(id = 0, account = account, date = LocalDate(2024, 2, 15)))
        // March — previous month, should be included
        repository.upsertTransaction(TestFixtures.transaction(id = 0, account = account, date = LocalDate(2024, 3, 10)))

        val result = sut(MonthKey(2024, 4))

        assertEquals(1, result.size)
        assertEquals(3, result.first().date.monthNumber)
    }

    @Test
    fun `year boundary - current January returns December transactions from previous year`() = runTest {
        val account = TestFixtures.account(id = 1)
        repository.upsertAccount(account)

        // December 2023 — previous month relative to January 2024
        repository.upsertTransaction(TestFixtures.transaction(id = 0, account = account, date = LocalDate(2023, 12, 1)))
        repository.upsertTransaction(TestFixtures.transaction(id = 0, account = account, date = LocalDate(2023, 12, 31)))

        // January 2024 — current month, should be excluded
        repository.upsertTransaction(TestFixtures.transaction(id = 0, account = account, date = LocalDate(2024, 1, 15)))

        val result = sut(MonthKey(2024, 1))

        assertEquals(2, result.size)
        result.forEach { tx ->
            assertEquals(2023, tx.date.year)
            assertEquals(12, tx.date.monthNumber)
        }
    }

    @Test
    fun `empty repository returns empty list`() = runTest {
        val result = sut(MonthKey(2024, 4))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `repository with no matching transactions returns empty list`() = runTest {
        val account = TestFixtures.account(id = 1)
        repository.upsertAccount(account)

        // Only January 2024 transactions — no match for April 2024 (previous = March)
        repository.upsertTransaction(TestFixtures.transaction(id = 0, account = account, date = LocalDate(2024, 1, 10)))

        val result = sut(MonthKey(2024, 4))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `first day of previous month is included`() = runTest {
        val account = TestFixtures.account(id = 1)
        repository.upsertAccount(account)

        // Exactly the first day of the previous month
        repository.upsertTransaction(TestFixtures.transaction(id = 0, account = account, date = LocalDate(2024, 3, 1)))

        val result = sut(MonthKey(2024, 4))

        assertEquals(1, result.size)
    }

    @Test
    fun `first day of current month is excluded`() = runTest {
        val account = TestFixtures.account(id = 1)
        repository.upsertAccount(account)

        // The boundary: April 1 is the start of the current month and must not appear
        repository.upsertTransaction(TestFixtures.transaction(id = 0, account = account, date = LocalDate(2024, 4, 1)))

        val result = sut(MonthKey(2024, 4))

        assertTrue(result.isEmpty())
    }
}
