package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateTransactionTotalUseCase
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculateTransactionTotalUseCaseTest {

    // Rates: PLN=1.0, USD=0.25, EUR=0.22, UAH=10.0
    // convert(amount, from, to) = amount / fromRate * toRate
    // baseCurrency = PLN

    private val currencyManager = TestFixtures.currencyManager()
    private lateinit var sut: CalculateTransactionTotalUseCase

    @BeforeTest
    fun setup() = runTest {
        currencyManager.refreshExchangeRates()
        sut = CalculateTransactionTotalUseCase(currencyManager)
    }

    @Test
    fun `sums only expense non-tax transactions`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 200.0),
            TestFixtures.transaction(subcategory = TestFixtures.rent, amount = 1200.0)
        )

        val result = sut(transactions, Currency.PLN, Currency.PLN)

        assertEquals(1400.0, result.total, 0.001)
        assertEquals(Currency.PLN, result.currency)
    }

    @Test
    fun `excludes income transactions from total`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.salary, amount = 5000.0),
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 300.0)
        )

        val result = sut(transactions, Currency.PLN, Currency.PLN)

        // Only expense transaction counts
        assertEquals(300.0, result.total, 0.001)
    }

    @Test
    fun `excludes ZUS tax transactions`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 400.0),
            TestFixtures.transaction(subcategory = TestFixtures.zus, amount = 500.0)
        )

        val result = sut(transactions, Currency.PLN, Currency.PLN)

        // ZUS is excluded
        assertEquals(400.0, result.total, 0.001)
    }

    @Test
    fun `excludes PIT tax transactions`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.utilities, amount = 150.0),
            TestFixtures.transaction(subcategory = TestFixtures.pit, amount = 1000.0)
        )

        val result = sut(transactions, Currency.PLN, Currency.PLN)

        // PIT is excluded
        assertEquals(150.0, result.total, 0.001)
    }

    @Test
    fun `excludes both ZUS and PIT leaving only regular expenses`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 200.0),
            TestFixtures.transaction(subcategory = TestFixtures.zus, amount = 500.0),
            TestFixtures.transaction(subcategory = TestFixtures.pit, amount = 800.0)
        )

        val result = sut(transactions, Currency.PLN, Currency.PLN)

        assertEquals(200.0, result.total, 0.001)
    }

    @Test
    fun `converts total to selected currency when different from base`() {
        // 400 PLN expenses; convert to USD: 400 / 1.0 * 0.25 = 100 USD
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 400.0)
        )

        val result = sut(transactions, selectedCurrency = Currency.USD, baseCurrency = Currency.PLN)

        assertEquals(100.0, result.total, 0.001)
        assertEquals(Currency.USD, result.currency)
    }

    @Test
    fun `same selected and base currency performs no conversion`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.rent, amount = 1200.0)
        )

        val result = sut(transactions, selectedCurrency = Currency.PLN, baseCurrency = Currency.PLN)

        assertEquals(1200.0, result.total, 0.001)
    }

    @Test
    fun `empty list returns zero total`() {
        val result = sut(emptyList(), Currency.PLN, Currency.PLN)

        assertEquals(0.0, result.total, 0.001)
    }

    @Test
    fun `null transactions in list are filtered out`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 300.0),
            null
        )

        val result = sut(transactions, Currency.PLN, Currency.PLN)

        assertEquals(300.0, result.total, 0.001)
    }

    @Test
    fun `list of only nulls returns zero`() {
        val transactions = listOf<Nothing?>(null, null, null)

        val result = sut(transactions, Currency.PLN, Currency.PLN)

        assertEquals(0.0, result.total, 0.001)
    }

    @Test
    fun `USD expense transaction is converted to PLN base before summing`() {
        // 100 USD to PLN: 100 / 0.25 * 1.0 = 400 PLN
        val usdAccount = TestFixtures.account(currency = Currency.USD)
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 100.0, account = usdAccount)
        )

        val result = sut(transactions, selectedCurrency = Currency.PLN, baseCurrency = Currency.PLN)

        assertEquals(400.0, result.total, 0.001)
    }

    @Test
    fun `UAH expense transaction is converted to PLN correctly`() {
        // 100 UAH to PLN: 100 / 10.0 * 1.0 = 10 PLN
        val uahAccount = TestFixtures.account(currency = Currency.UAH)
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.utilities, amount = 100.0, account = uahAccount)
        )

        val result = sut(transactions, selectedCurrency = Currency.PLN, baseCurrency = Currency.PLN)

        assertEquals(10.0, result.total, 0.001)
    }

    @Test
    fun `mixed PLN and USD expenses are summed correctly after conversion`() {
        // 200 PLN + 100 USD (= 400 PLN) = 600 PLN total
        val plnAccount = TestFixtures.account(id = 1, currency = Currency.PLN)
        val usdAccount = TestFixtures.account(id = 2, currency = Currency.USD)
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 200.0, account = plnAccount),
            TestFixtures.transaction(subcategory = TestFixtures.rent, amount = 100.0, account = usdAccount)
        )

        val result = sut(transactions, selectedCurrency = Currency.PLN, baseCurrency = Currency.PLN)

        assertEquals(600.0, result.total, 0.001)
    }

    @Test
    fun `result currency matches selected currency`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 100.0)
        )

        val result = sut(transactions, selectedCurrency = Currency.EUR, baseCurrency = Currency.PLN)

        assertEquals(Currency.EUR, result.currency)
    }

    @Test
    fun `transfer transactions are excluded`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.internalTransfer, amount = 500.0),
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 100.0)
        )

        val result = sut(transactions, Currency.PLN, Currency.PLN)

        // Only expense counts
        assertEquals(100.0, result.total, 0.001)
    }
}
