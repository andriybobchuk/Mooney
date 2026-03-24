package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateAnalyticsMetricsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateTaxesUseCase
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalculateAnalyticsMetricsUseCaseTest {

    private val calculateTaxesUseCase = CalculateTaxesUseCase()
    private val sut = CalculateAnalyticsMetricsUseCase(calculateTaxesUseCase)

    // No setup needed — sut is pure and has no coroutine dependencies

    @Test
    fun `returns exactly 4 metrics`() {
        val result = sut(
            currentRevenue = 5000.0, currentExpenses = 1000.0, currentTransactions = emptyList(),
            previousRevenue = 4000.0, previousExpenses = 800.0, previousTransactions = emptyList(),
            baseCurrency = Currency.PLN, exchangeRates = TestFixtures.testRates
        )

        assertEquals(4, result.size)
    }

    @Test
    fun `metrics are in correct order - Revenue Taxes Operating Costs Net Income`() {
        val result = sut(
            currentRevenue = 5000.0, currentExpenses = 1000.0, currentTransactions = emptyList(),
            previousRevenue = 4000.0, previousExpenses = 800.0, previousTransactions = emptyList(),
            baseCurrency = Currency.PLN, exchangeRates = TestFixtures.testRates
        )

        assertEquals("Revenue", result[0].title)
        assertEquals("Taxes", result[1].title)
        assertEquals("Operating Costs", result[2].title)
        assertEquals("Net Income", result[3].title)
    }

    @Test
    fun `revenue metric value contains current revenue`() {
        val result = sut(
            currentRevenue = 5000.0, currentExpenses = 1000.0, currentTransactions = emptyList(),
            previousRevenue = 4000.0, previousExpenses = 800.0, previousTransactions = emptyList(),
            baseCurrency = Currency.PLN, exchangeRates = TestFixtures.testRates
        )

        // value is formatted as "5,000.00 zł"
        assertTrue(result[0].value.contains("5,000.00"))
        assertTrue(result[0].value.contains("zł"))
    }

    @Test
    fun `taxes metric is computed from ZUS and PIT transactions`() {
        // 200 PLN ZUS + 300 PLN PIT = 500 PLN taxes
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.zus, amount = 200.0),
            TestFixtures.transaction(subcategory = TestFixtures.pit, amount = 300.0)
        )

        val result = sut(
            currentRevenue = 5000.0, currentExpenses = 1000.0, currentTransactions = transactions,
            previousRevenue = 4000.0, previousExpenses = 800.0, previousTransactions = emptyList(),
            baseCurrency = Currency.PLN, exchangeRates = TestFixtures.testRates
        )

        assertTrue(result[1].value.contains("500.00"))
    }

    @Test
    fun `taxes metric is 0 when no tax transactions`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 200.0)
        )

        val result = sut(
            currentRevenue = 5000.0, currentExpenses = 1000.0, currentTransactions = transactions,
            previousRevenue = 4000.0, previousExpenses = 800.0, previousTransactions = emptyList(),
            baseCurrency = Currency.PLN, exchangeRates = TestFixtures.testRates
        )

        assertTrue(result[1].value.contains("0.00"))
    }

    @Test
    fun `operating costs metric uses expenses parameter`() {
        val result = sut(
            currentRevenue = 5000.0, currentExpenses = 1500.0, currentTransactions = emptyList(),
            previousRevenue = 4000.0, previousExpenses = 800.0, previousTransactions = emptyList(),
            baseCurrency = Currency.PLN, exchangeRates = TestFixtures.testRates
        )

        assertTrue(result[2].value.contains("1,500.00"))
    }

    @Test
    fun `net income equals revenue minus taxes minus expenses`() {
        // Revenue = 5000, Taxes = 200 + 300 = 500, Expenses = 1000
        // Net Income = 5000 - 500 - 1000 = 3500
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.zus, amount = 200.0),
            TestFixtures.transaction(subcategory = TestFixtures.pit, amount = 300.0)
        )

        val result = sut(
            currentRevenue = 5000.0, currentExpenses = 1000.0, currentTransactions = transactions,
            previousRevenue = 4000.0, previousExpenses = 800.0, previousTransactions = emptyList(),
            baseCurrency = Currency.PLN, exchangeRates = TestFixtures.testRates
        )

        assertTrue(result[3].value.contains("3,500.00"))
    }

    @Test
    fun `trend percentage calculated correctly when previous is greater than zero`() {
        // Revenue: current=5000, previous=4000 → trend = (5000-4000)/4000 * 100 = 25.0%
        val result = sut(
            currentRevenue = 5000.0, currentExpenses = 1000.0, currentTransactions = emptyList(),
            previousRevenue = 4000.0, previousExpenses = 800.0, previousTransactions = emptyList(),
            baseCurrency = Currency.PLN, exchangeRates = TestFixtures.testRates
        )

        assertEquals(25.0, result[0].trendPercentage, 0.001)
    }

    @Test
    fun `trend percentage is negative when current is less than previous`() {
        // Revenue: current=3000, previous=4000 → trend = (3000-4000)/4000 * 100 = -25.0%
        val result = sut(
            currentRevenue = 3000.0, currentExpenses = 1000.0, currentTransactions = emptyList(),
            previousRevenue = 4000.0, previousExpenses = 800.0, previousTransactions = emptyList(),
            baseCurrency = Currency.PLN, exchangeRates = TestFixtures.testRates
        )

        assertEquals(-25.0, result[0].trendPercentage, 0.001)
    }

    @Test
    fun `trend percentage is 0 when previous is 0`() {
        val result = sut(
            currentRevenue = 5000.0, currentExpenses = 1000.0, currentTransactions = emptyList(),
            previousRevenue = 0.0, previousExpenses = 0.0, previousTransactions = emptyList(),
            baseCurrency = Currency.PLN, exchangeRates = TestFixtures.testRates
        )

        assertEquals(0.0, result[0].trendPercentage, 0.001)
    }

    @Test
    fun `all 4 metrics are clickable`() {
        val result = sut(
            currentRevenue = 5000.0, currentExpenses = 1000.0, currentTransactions = emptyList(),
            previousRevenue = 4000.0, previousExpenses = 800.0, previousTransactions = emptyList(),
            baseCurrency = Currency.PLN, exchangeRates = TestFixtures.testRates
        )

        result.forEach { metric ->
            assertTrue(metric.isClickable, "Expected metric '${metric.title}' to be clickable")
        }
    }

    @Test
    fun `operating costs trend uses expenses parameter for comparison`() {
        // Expenses: current=1200, previous=800 → trend = (1200-800)/800 * 100 = 50.0%
        val result = sut(
            currentRevenue = 5000.0, currentExpenses = 1200.0, currentTransactions = emptyList(),
            previousRevenue = 4000.0, previousExpenses = 800.0, previousTransactions = emptyList(),
            baseCurrency = Currency.PLN, exchangeRates = TestFixtures.testRates
        )

        assertEquals(50.0, result[2].trendPercentage, 0.001)
    }

    @Test
    fun `all metrics are zero when all inputs are zero`() {
        val result = sut(
            currentRevenue = 0.0, currentExpenses = 0.0, currentTransactions = emptyList(),
            previousRevenue = 0.0, previousExpenses = 0.0, previousTransactions = emptyList(),
            baseCurrency = Currency.PLN, exchangeRates = TestFixtures.testRates
        )

        assertEquals(4, result.size)
        result.forEach { metric ->
            assertEquals(0.0, metric.trendPercentage, 0.001,
                "Expected trendPercentage to be 0 for metric '${metric.title}'")
        }
    }
}
