package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.AssetCategory
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.usecase.ConvertAccountsToUiUseCase
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConvertAccountsToUiUseCaseTest {

    // baseCurrency = PLN (from GlobalConfig)
    // Rates: PLN=1.0, USD=0.25, EUR=0.22, UAH=10.0
    // convert(amount, from, to) = amount / fromRate * toRate

    private val currencyManager = TestFixtures.currencyManager()
    private val sut = ConvertAccountsToUiUseCase(currencyManager)

    @BeforeTest
    fun setup() = runTest {
        currencyManager.refreshExchangeRates()
    }

    @Test
    fun `base currency account has null exchangeRate`() {
        val plnAccount = TestFixtures.account(currency = Currency.PLN, amount = 1000.0)

        val result = sut(listOf(plnAccount))

        assertNotNull(result[0])
        assertNull(result[0]!!.exchangeRate)
    }

    @Test
    fun `base currency account has same baseCurrencyAmount as original`() {
        val plnAccount = TestFixtures.account(currency = Currency.PLN, amount = 1000.0)

        val result = sut(listOf(plnAccount))

        assertNotNull(result[0])
        assertEquals(1000.0, result[0]!!.baseCurrencyAmount, 0.001)
        assertEquals(1000.0, result[0]!!.originalAmount, 0.001)
    }

    @Test
    fun `foreign currency account has converted baseCurrencyAmount`() {
        // 100 USD to PLN: 100 / 0.25 * 1.0 = 400 PLN
        val usdAccount = TestFixtures.account(currency = Currency.USD, amount = 100.0)

        val result = sut(listOf(usdAccount))

        assertNotNull(result[0])
        assertEquals(400.0, result[0]!!.baseCurrencyAmount, 0.001)
    }

    @Test
    fun `EUR account has correct converted baseCurrencyAmount`() {
        // 110 EUR to PLN: 110 / 0.22 * 1.0 = 500 PLN
        val eurAccount = TestFixtures.account(currency = Currency.EUR, amount = 110.0)

        val result = sut(listOf(eurAccount))

        assertNotNull(result[0])
        assertEquals(500.0, result[0]!!.baseCurrencyAmount, 0.001)
    }

    @Test
    fun `UAH account has correct converted baseCurrencyAmount`() {
        // 100 UAH to PLN: 100 / 10.0 * 1.0 = 10 PLN
        val uahAccount = TestFixtures.account(currency = Currency.UAH, amount = 100.0)

        val result = sut(listOf(uahAccount))

        assertNotNull(result[0])
        assertEquals(10.0, result[0]!!.baseCurrencyAmount, 0.001)
    }

    @Test
    fun `foreign currency account has correct exchange rate for 1 unit to PLN`() {
        // 1 USD to PLN: 1 / 0.25 * 1.0 = 4.0
        val usdAccount = TestFixtures.account(currency = Currency.USD, amount = 100.0)

        val result = sut(listOf(usdAccount))

        assertNotNull(result[0])
        assertEquals(4.0, result[0]!!.exchangeRate!!, 0.001)
    }

    @Test
    fun `EUR account has correct exchange rate for 1 unit to PLN`() {
        // 1 EUR to PLN: 1 / 0.22 * 1.0 = ~4.545
        val eurAccount = TestFixtures.account(currency = Currency.EUR, amount = 50.0)

        val result = sut(listOf(eurAccount))

        assertNotNull(result[0])
        assertEquals(1.0 / 0.22 * 1.0, result[0]!!.exchangeRate!!, 0.001)
    }

    @Test
    fun `null account in list results in null in output`() {
        val result = sut(listOf(null))

        assertEquals(1, result.size)
        assertNull(result[0])
    }

    @Test
    fun `empty list returns empty list`() {
        val result = sut(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `mixed list preserves null positions`() {
        val plnAccount = TestFixtures.account(id = 1, currency = Currency.PLN, amount = 500.0)
        val usdAccount = TestFixtures.account(id = 2, currency = Currency.USD, amount = 100.0)

        val result = sut(listOf(plnAccount, null, usdAccount))

        assertEquals(3, result.size)
        assertNotNull(result[0])
        assertNull(result[1])
        assertNotNull(result[2])
    }

    @Test
    fun `preserves assetCategory for base currency account`() {
        val account = TestFixtures.account(
            currency = Currency.PLN,
            amount = 1000.0,
            assetCategory = AssetCategory.CASH
        )

        val result = sut(listOf(account))

        assertNotNull(result[0])
        assertEquals(AssetCategory.CASH, result[0]!!.assetCategory)
    }

    @Test
    fun `preserves assetCategory for foreign currency account`() {
        val account = TestFixtures.account(
            currency = Currency.USD,
            amount = 200.0,
            assetCategory = AssetCategory.STOCKS
        )

        val result = sut(listOf(account))

        assertNotNull(result[0])
        assertEquals(AssetCategory.STOCKS, result[0]!!.assetCategory)
    }

    @Test
    fun `preserves original amount and currency for foreign account`() {
        val usdAccount = TestFixtures.account(currency = Currency.USD, amount = 250.0)

        val result = sut(listOf(usdAccount))

        assertNotNull(result[0])
        assertEquals(250.0, result[0]!!.originalAmount, 0.001)
        assertEquals(Currency.USD, result[0]!!.originalCurrency)
    }

    @Test
    fun `multiple accounts each converted independently`() {
        // 100 USD to PLN: 100 / 0.25 = 400
        // 200 UAH to PLN: 200 / 10.0 = 20
        val usdAccount = TestFixtures.account(id = 1, currency = Currency.USD, amount = 100.0)
        val uahAccount = TestFixtures.account(id = 2, currency = Currency.UAH, amount = 200.0)

        val result = sut(listOf(usdAccount, uahAccount))

        assertEquals(2, result.size)
        assertEquals(400.0, result[0]!!.baseCurrencyAmount, 0.001)
        assertEquals(20.0, result[1]!!.baseCurrencyAmount, 0.001)
    }
}
