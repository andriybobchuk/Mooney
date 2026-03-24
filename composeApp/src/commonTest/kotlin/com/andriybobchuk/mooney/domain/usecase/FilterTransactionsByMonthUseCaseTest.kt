package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.usecase.FilterTransactionsByMonthUseCase
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilterTransactionsByMonthUseCaseTest {

    private val sut = FilterTransactionsByMonthUseCase()
    private val march2024 = MonthKey(2024, 3)

    @Test
    fun `empty list returns empty result`() {
        val result = sut(emptyList(), march2024)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `transactions within the month are included`() {
        val transactions = listOf(
            TestFixtures.transaction(date = LocalDate(2024, 3, 1)),
            TestFixtures.transaction(date = LocalDate(2024, 3, 15)),
            TestFixtures.transaction(date = LocalDate(2024, 3, 31))
        )

        val result = sut(transactions, march2024)

        assertEquals(3, result.size)
    }

    @Test
    fun `transactions outside the month are excluded`() {
        val transactions = listOf(
            TestFixtures.transaction(date = LocalDate(2024, 2, 28)),
            TestFixtures.transaction(date = LocalDate(2024, 4, 1))
        )

        val result = sut(transactions, march2024)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `transactions from previous and next month mixed with current month`() {
        val transactions = listOf(
            TestFixtures.transaction(date = LocalDate(2024, 2, 28)),  // excluded: before
            TestFixtures.transaction(date = LocalDate(2024, 3, 10)),  // included
            TestFixtures.transaction(date = LocalDate(2024, 3, 20)),  // included
            TestFixtures.transaction(date = LocalDate(2024, 4, 1))    // excluded: after
        )

        val result = sut(transactions, march2024)

        assertEquals(2, result.size)
        assertTrue(result.all { it.date.monthNumber == 3 && it.date.year == 2024 })
    }

    @Test
    fun `boundary - first day of month is included`() {
        val transactions = listOf(
            TestFixtures.transaction(date = LocalDate(2024, 3, 1))
        )

        val result = sut(transactions, march2024)

        assertEquals(1, result.size)
    }

    @Test
    fun `boundary - last day of month is included`() {
        val transactions = listOf(
            TestFixtures.transaction(date = LocalDate(2024, 3, 31))
        )

        val result = sut(transactions, march2024)

        assertEquals(1, result.size)
    }

    @Test
    fun `boundary - first day of next month is excluded`() {
        val transactions = listOf(
            TestFixtures.transaction(date = LocalDate(2024, 4, 1))
        )

        val result = sut(transactions, march2024)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `boundary - day before first day of month is excluded`() {
        val transactions = listOf(
            TestFixtures.transaction(date = LocalDate(2024, 2, 29))
        )

        val result = sut(transactions, march2024)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `null transactions in list are silently skipped`() {
        val transactions = listOf(
            TestFixtures.transaction(date = LocalDate(2024, 3, 10)),
            null,
            TestFixtures.transaction(date = LocalDate(2024, 3, 20)),
            null
        )

        val result = sut(transactions, march2024)

        assertEquals(2, result.size)
    }

    @Test
    fun `list containing only null transactions returns empty result`() {
        val transactions = listOf(null, null, null)

        val result = sut(transactions, march2024)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `year boundary - December to January`() {
        val december2023 = MonthKey(2023, 12)
        val transactions = listOf(
            TestFixtures.transaction(date = LocalDate(2023, 11, 30)),  // excluded: November
            TestFixtures.transaction(date = LocalDate(2023, 12, 1)),   // included: Dec 1
            TestFixtures.transaction(date = LocalDate(2023, 12, 31)),  // included: Dec 31
            TestFixtures.transaction(date = LocalDate(2024, 1, 1))     // excluded: next year Jan 1
        )

        val result = sut(transactions, december2023)

        assertEquals(2, result.size)
        assertTrue(result.all { it.date.monthNumber == 12 && it.date.year == 2023 })
    }

    @Test
    fun `year boundary - January first day of next month is excluded`() {
        val january2024 = MonthKey(2024, 1)
        val transactions = listOf(
            TestFixtures.transaction(date = LocalDate(2024, 1, 31)),  // included: last day of Jan
            TestFixtures.transaction(date = LocalDate(2024, 2, 1))    // excluded: first day of Feb
        )

        val result = sut(transactions, january2024)

        assertEquals(1, result.size)
        assertEquals(LocalDate(2024, 1, 31), result[0].date)
    }

    @Test
    fun `single transaction exactly matching month is returned`() {
        val transactions = listOf(
            TestFixtures.transaction(date = LocalDate(2024, 3, 15))
        )

        val result = sut(transactions, march2024)

        assertEquals(1, result.size)
        assertEquals(LocalDate(2024, 3, 15), result[0].date)
    }
}
