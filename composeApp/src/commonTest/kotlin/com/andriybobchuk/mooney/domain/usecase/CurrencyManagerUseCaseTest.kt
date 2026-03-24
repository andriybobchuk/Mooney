package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.core.domain.DataError
import com.andriybobchuk.mooney.core.domain.Result
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRateProvider
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.usecase.CurrencyManagerUseCase
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CurrencyManagerUseCaseTest {

    private val sut = TestFixtures.currencyManager()

    // PLN: 1.0, USD: 0.25, EUR: 0.22, UAH: 10.0
    private val testRates = TestFixtures.testRates

    @BeforeTest
    fun setup() = runTest {
        sut.refreshExchangeRates()
    }

    @Test
    fun `starts with base currency PLN`() {
        assertEquals(Currency.PLN, sut.getCurrentCurrency())
    }

    @Test
    fun `getCurrentBaseCurrency returns PLN`() {
        assertEquals(Currency.PLN, sut.getCurrentBaseCurrency())
    }

    @Test
    fun `refreshExchangeRates updates cached rates on success`() = runTest {
        val rates = sut.getCurrentExchangeRates()

        assertEquals(testRates.rates[Currency.PLN], rates.rates[Currency.PLN])
        assertEquals(testRates.rates[Currency.USD], rates.rates[Currency.USD])
        assertEquals(testRates.rates[Currency.EUR], rates.rates[Currency.EUR])
        assertEquals(testRates.rates[Currency.UAH], rates.rates[Currency.UAH])
    }

    @Test
    fun `refreshExchangeRates returns error and keeps old cached rates when provider fails`() = runTest {
        val failingProvider = object : ExchangeRateProvider {
            override suspend fun getExchangeRates(baseCurrency: Currency): Result<ExchangeRates, DataError.Remote> {
                return Result.Error(DataError.Remote.UNKNOWN)
            }
        }
        val sutWithFailingProvider = CurrencyManagerUseCase(failingProvider)
        // Seed with known good rates first via a successful refresh
        sutWithFailingProvider.refreshExchangeRates() // uses GlobalConfig.testExchangeRates as initial cache

        val ratesBefore = sutWithFailingProvider.getCurrentExchangeRates()

        val refreshResult = sutWithFailingProvider.refreshExchangeRates()

        assertTrue(refreshResult is Result.Error)
        // Rates must be unchanged after a failed refresh
        assertEquals(ratesBefore.rates, sutWithFailingProvider.getCurrentExchangeRates().rates)
    }

    @Test
    fun `cycleToNextCurrency advances to the next available currency`() = runTest {
        val initialCurrency = sut.getCurrentCurrency()
        val availableCurrencies = sut.getAvailableCurrencies()

        val nextCurrency = sut.cycleToNextCurrency()

        val expectedIndex = (availableCurrencies.indexOf(initialCurrency) + 1) % availableCurrencies.size
        assertEquals(availableCurrencies[expectedIndex], nextCurrency)
        assertEquals(nextCurrency, sut.getCurrentCurrency())
    }

    @Test
    fun `cycleToNextCurrency wraps around after last currency`() = runTest {
        val availableCurrencies = sut.getAvailableCurrencies()
        // Cycle through all currencies to reach the last one, then wrap
        repeat(availableCurrencies.size - 1) { sut.cycleToNextCurrency() }
        val lastCurrency = sut.getCurrentCurrency()

        val wrapped = sut.cycleToNextCurrency()

        val expectedFirst = availableCurrencies.first()
        // After wrapping, the index returns to the beginning
        assertEquals(availableCurrencies[(availableCurrencies.indexOf(lastCurrency) + 1) % availableCurrencies.size], wrapped)
    }

    @Test
    fun `resetToBaseCurrency resets selected currency back to PLN`() = runTest {
        sut.cycleToNextCurrency()
        sut.cycleToNextCurrency()

        sut.resetToBaseCurrency()

        assertEquals(Currency.PLN, sut.getCurrentCurrency())
    }

    @Test
    fun `convertToBaseCurrency converts USD to PLN correctly`() = runTest {
        // 100 USD / 0.25 * 1.0 = 400 PLN
        val result = sut.convertToBaseCurrency(100.0, Currency.USD)

        assertEquals(400.0, result, 0.001)
    }

    @Test
    fun `convertToBaseCurrency converts EUR to PLN correctly`() = runTest {
        // 44 EUR / 0.22 * 1.0 = 200 PLN
        val result = sut.convertToBaseCurrency(44.0, Currency.EUR)

        assertEquals(200.0, result, 0.001)
    }

    @Test
    fun `convertToBaseCurrency converts UAH to PLN correctly`() = runTest {
        // 100 UAH / 10.0 * 1.0 = 10 PLN
        val result = sut.convertToBaseCurrency(100.0, Currency.UAH)

        assertEquals(10.0, result, 0.001)
    }

    @Test
    fun `convertToBaseCurrency is identity for PLN to PLN`() = runTest {
        val result = sut.convertToBaseCurrency(250.0, Currency.PLN)

        assertEquals(250.0, result, 0.001)
    }

    @Test
    fun `getAvailableCurrencies returns all currencies from exchange rates`() = runTest {
        val available = sut.getAvailableCurrencies()

        assertTrue(available.contains(Currency.PLN))
        assertTrue(available.contains(Currency.USD))
        assertTrue(available.contains(Currency.EUR))
        assertTrue(available.contains(Currency.UAH))
    }
}
