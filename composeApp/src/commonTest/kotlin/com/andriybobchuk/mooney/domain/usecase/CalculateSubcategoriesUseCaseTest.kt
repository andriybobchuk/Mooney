package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateSubcategoriesUseCase
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalculateSubcategoriesUseCaseTest {

    // Rates: PLN=1.0, USD=0.25, EUR=0.22, UAH=10.0
    // convert(amount, from, to) = amount / fromRate * toRate
    // baseCurrency = PLN

    private val currencyManager = TestFixtures.currencyManager()
    private lateinit var sut: CalculateSubcategoriesUseCase

    @BeforeTest
    fun setup() = runTest {
        currencyManager.refreshExchangeRates()
        sut = CalculateSubcategoriesUseCase(currencyManager)
    }

    @Test
    fun `groups subcategories under parent correctly`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.rent, amount = 1200.0),
            TestFixtures.transaction(subcategory = TestFixtures.utilities, amount = 300.0)
        )

        val result = sut(TestFixtures.housing, transactions, Currency.PLN)

        assertEquals(2, result.size)
        val categories = result.map { it.category }
        assertTrue(categories.contains(TestFixtures.rent))
        assertTrue(categories.contains(TestFixtures.utilities))
    }

    @Test
    fun `parent category direct transactions are included`() {
        // A transaction directly assigned to housing (the parent), not a subcategory
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.housing, amount = 500.0),
            TestFixtures.transaction(subcategory = TestFixtures.rent, amount = 1000.0)
        )

        val result = sut(TestFixtures.housing, transactions, Currency.PLN)

        val categories = result.map { it.category }
        assertTrue(categories.contains(TestFixtures.housing), "Parent category should appear when it has direct transactions")
        assertTrue(categories.contains(TestFixtures.rent))
    }

    @Test
    fun `sorted by amount descending`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.utilities, amount = 200.0),
            TestFixtures.transaction(subcategory = TestFixtures.rent, amount = 1500.0)
        )

        val result = sut(TestFixtures.housing, transactions, Currency.PLN)

        assertEquals(2, result.size)
        assertEquals(TestFixtures.rent, result[0].category)
        assertEquals(TestFixtures.utilities, result[1].category)
    }

    @Test
    fun `trend percentage computed from previous month`() {
        val current = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.rent, amount = 1200.0)
        )
        val previous = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.rent, amount = 1000.0)
        )

        val result = sut(TestFixtures.housing, current, Currency.PLN, previous)

        // Trend = (1200 - 1000) / 1000 * 100 = 20%
        val rentSummary = result.first { it.category == TestFixtures.rent }
        assertEquals(20.0, rentSummary.trendPercentage, 0.001)
    }

    @Test
    fun `new subcategory this month gets 100 percent trend`() {
        val current = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.rent, amount = 800.0)
        )
        // No rent in previous month
        val previous = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.utilities, amount = 100.0)
        )

        val result = sut(TestFixtures.housing, current, Currency.PLN, previous)

        val rentSummary = result.first { it.category == TestFixtures.rent }
        assertEquals(100.0, rentSummary.trendPercentage, 0.001)
    }

    @Test
    fun `empty transactions returns empty list`() {
        val result = sut(TestFixtures.housing, emptyList(), Currency.PLN)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `transactions from other parent category are excluded`() {
        // Groceries belongs to expense parent, not housing
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 200.0),
            TestFixtures.transaction(subcategory = TestFixtures.rent, amount = 1200.0)
        )

        val result = sut(TestFixtures.housing, transactions, Currency.PLN)

        val categories = result.map { it.category }
        assertTrue(categories.contains(TestFixtures.rent))
        assertTrue(!categories.contains(TestFixtures.groceries))
    }

    @Test
    fun `amounts are summed per subcategory`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.rent, amount = 600.0),
            TestFixtures.transaction(subcategory = TestFixtures.rent, amount = 600.0)
        )

        val result = sut(TestFixtures.housing, transactions, Currency.PLN)

        assertEquals(1, result.size)
        assertEquals(1200.0, result[0].amount, 0.001)
    }

    @Test
    fun `foreign currency amounts are converted to base currency`() {
        // 100 USD to PLN: 100 / 0.25 * 1.0 = 400 PLN
        val usdAccount = TestFixtures.account(currency = Currency.USD)
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.rent, amount = 100.0, account = usdAccount)
        )

        val result = sut(TestFixtures.housing, transactions, Currency.PLN)

        assertEquals(1, result.size)
        assertEquals(400.0, result[0].amount, 0.001)
    }

    @Test
    fun `no previous month data gives zero trend for all`() {
        val current = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.rent, amount = 1000.0)
        )

        val result = sut(TestFixtures.housing, current, Currency.PLN, emptyList())

        val rentSummary = result.first { it.category == TestFixtures.rent }
        // New in this period → 100%
        assertEquals(100.0, rentSummary.trendPercentage, 0.001)
    }

    @Test
    fun `decreasing subcategory has negative trend`() {
        val current = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.utilities, amount = 100.0)
        )
        val previous = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.utilities, amount = 200.0)
        )

        val result = sut(TestFixtures.housing, current, Currency.PLN, previous)

        // Trend = (100 - 200) / 200 * 100 = -50%
        val summary = result.first { it.category == TestFixtures.utilities }
        assertEquals(-50.0, summary.trendPercentage, 0.001)
    }

    @Test
    fun `percent of category total is computed correctly`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.rent, amount = 800.0),
            TestFixtures.transaction(subcategory = TestFixtures.utilities, amount = 200.0)
        )

        val result = sut(TestFixtures.housing, transactions, Currency.PLN)

        val rentSummary = result.first { it.category == TestFixtures.rent }
        // 800 / 1000 * 100 = 80%
        assertEquals("80.00", rentSummary.percentOfRevenue)
    }
}
