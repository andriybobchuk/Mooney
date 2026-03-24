package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateTaxesUseCase
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalculateTaxesUseCaseTest {

    private val currencyManager = TestFixtures.currencyManager()
    private val sut = CalculateTaxesUseCase()

    @BeforeTest
    fun setup() = runTest {
        currencyManager.refreshExchangeRates()
    }

    // --- isTaxTransaction ---

    @Test
    fun `isTaxTransaction returns true for ZUS subcategory`() {
        val transaction = TestFixtures.transaction(subcategory = TestFixtures.zus)
        assertTrue(CalculateTaxesUseCase.isTaxTransaction(transaction))
    }

    @Test
    fun `isTaxTransaction returns true for PIT subcategory`() {
        val transaction = TestFixtures.transaction(subcategory = TestFixtures.pit)
        assertTrue(CalculateTaxesUseCase.isTaxTransaction(transaction))
    }

    @Test
    fun `isTaxTransaction is case insensitive for lowercase zus`() {
        val lowercaseZus = TestFixtures.zus.copy(title = "zus")
        val transaction = TestFixtures.transaction(subcategory = lowercaseZus)
        assertTrue(CalculateTaxesUseCase.isTaxTransaction(transaction))
    }

    @Test
    fun `isTaxTransaction is case insensitive for mixed case Pit`() {
        val mixedPit = TestFixtures.pit.copy(title = "Pit")
        val transaction = TestFixtures.transaction(subcategory = mixedPit)
        assertTrue(CalculateTaxesUseCase.isTaxTransaction(transaction))
    }

    @Test
    fun `isTaxTransaction returns false for non-tax subcategory`() {
        val transaction = TestFixtures.transaction(subcategory = TestFixtures.groceries)
        assertFalse(CalculateTaxesUseCase.isTaxTransaction(transaction))
    }

    // --- invoke ---

    @Test
    fun `empty transaction list returns 0`() {
        val result = sut(emptyList(), Currency.PLN, TestFixtures.testRates)
        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun `ZUS transactions are summed correctly`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.zus, amount = 500.0),
            TestFixtures.transaction(subcategory = TestFixtures.zus, amount = 300.0)
        )

        val result = sut(transactions, Currency.PLN, TestFixtures.testRates)

        assertEquals(800.0, result, 0.001)
    }

    @Test
    fun `PIT transactions are summed correctly`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.pit, amount = 1000.0),
            TestFixtures.transaction(subcategory = TestFixtures.pit, amount = 200.0)
        )

        val result = sut(transactions, Currency.PLN, TestFixtures.testRates)

        assertEquals(1200.0, result, 0.001)
    }

    @Test
    fun `ZUS and PIT transactions are summed together`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.zus, amount = 500.0),
            TestFixtures.transaction(subcategory = TestFixtures.pit, amount = 300.0)
        )

        val result = sut(transactions, Currency.PLN, TestFixtures.testRates)

        assertEquals(800.0, result, 0.001)
    }

    @Test
    fun `non-tax transactions are excluded from sum`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.zus, amount = 500.0),
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 200.0),
            TestFixtures.transaction(subcategory = TestFixtures.rent, amount = 1000.0)
        )

        val result = sut(transactions, Currency.PLN, TestFixtures.testRates)

        assertEquals(500.0, result, 0.001)
    }

    @Test
    fun `only non-tax transactions returns 0`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 200.0),
            TestFixtures.transaction(subcategory = TestFixtures.salary, amount = 5000.0)
        )

        val result = sut(transactions, Currency.PLN, TestFixtures.testRates)

        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun `USD tax transaction is converted to PLN base currency`() {
        // 100 USD to PLN: 100 / 0.25 * 1.0 = 400 PLN
        val usdAccount = TestFixtures.account(currency = Currency.USD)
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.zus, amount = 100.0, account = usdAccount)
        )

        val result = sut(transactions, Currency.PLN, TestFixtures.testRates)

        assertEquals(400.0, result, 0.001)
    }

    @Test
    fun `mixed currency tax transactions are each converted to base currency`() {
        // 100 USD to PLN: 100 / 0.25 * 1.0 = 400 PLN
        // 220 UAH to PLN: 220 / 10.0 * 1.0 = 22 PLN
        // Total = 422 PLN
        val usdAccount = TestFixtures.account(id = 1, currency = Currency.USD)
        val uahAccount = TestFixtures.account(id = 2, currency = Currency.UAH)
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.zus, amount = 100.0, account = usdAccount),
            TestFixtures.transaction(subcategory = TestFixtures.pit, amount = 220.0, account = uahAccount)
        )

        val result = sut(transactions, Currency.PLN, TestFixtures.testRates)

        assertEquals(422.0, result, 0.001)
    }

    @Test
    fun `EUR tax transaction is converted to USD base currency`() {
        // 1.0 EUR to USD: 1.0 / 0.22 * 0.25 = ~1.136 USD
        val eurAccount = TestFixtures.account(currency = Currency.EUR)
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.zus, amount = 1.0, account = eurAccount)
        )

        val result = sut(transactions, Currency.USD, TestFixtures.testRates)

        assertEquals(1.0 / 0.22 * 0.25, result, 0.001)
    }
}
