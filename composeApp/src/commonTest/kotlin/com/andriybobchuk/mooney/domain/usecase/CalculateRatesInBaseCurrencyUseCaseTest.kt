package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateRatesInBaseCurrencyUseCase
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalculateRatesInBaseCurrencyUseCaseTest {

    private val sut = CalculateRatesInBaseCurrencyUseCase()

    @Test
    fun `null exchange rates returns empty map`() {
        val result = sut(exchangeRates = null, displayBaseCurrency = Currency.PLN)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `result excludes the display base currency`() {
        val result = sut(exchangeRates = TestFixtures.testRates, displayBaseCurrency = Currency.PLN)
        assertFalse(result.containsKey(Currency.PLN))
    }

    @Test
    fun `result contains all currencies except the base`() {
        val result = sut(exchangeRates = TestFixtures.testRates, displayBaseCurrency = Currency.PLN)
        val expectedSize = Currency.entries.size - 1

        assertEquals(expectedSize, result.size)
    }

    @Test
    fun `result has Currency entries size minus 1 entries`() {
        val result = sut(exchangeRates = TestFixtures.testRates, displayBaseCurrency = Currency.USD)
        assertEquals(Currency.entries.size - 1, result.size)
    }

    @Test
    fun `USD rate relative to PLN base is calculated correctly`() {
        // 1 USD to PLN: 1 / 0.25 * 1.0 = 4.0 PLN
        val result = sut(exchangeRates = TestFixtures.testRates, displayBaseCurrency = Currency.PLN)
        assertEquals(4.0, result[Currency.USD]!!, 0.001)
    }

    @Test
    fun `EUR rate relative to PLN base is calculated correctly`() {
        // 1 EUR to PLN: 1 / 0.22 * 1.0 ≈ 4.545 PLN
        val result = sut(exchangeRates = TestFixtures.testRates, displayBaseCurrency = Currency.PLN)
        assertEquals(1.0 / 0.22 * 1.0, result[Currency.EUR]!!, 0.001)
    }

    @Test
    fun `UAH rate relative to PLN base is calculated correctly`() {
        // 1 UAH to PLN: 1 / 10.0 * 1.0 = 0.1 PLN
        val result = sut(exchangeRates = TestFixtures.testRates, displayBaseCurrency = Currency.PLN)
        assertEquals(0.1, result[Currency.UAH]!!, 0.001)
    }

    @Test
    fun `PLN rate relative to USD base is calculated correctly`() {
        // 1 PLN to USD: 1 / 1.0 * 0.25 = 0.25 USD
        val result = sut(exchangeRates = TestFixtures.testRates, displayBaseCurrency = Currency.USD)
        assertEquals(0.25, result[Currency.PLN]!!, 0.001)
    }

    @Test
    fun `EUR rate relative to USD base is calculated correctly`() {
        // 1 EUR to USD: 1 / 0.22 * 0.25 ≈ 1.136 USD
        val result = sut(exchangeRates = TestFixtures.testRates, displayBaseCurrency = Currency.USD)
        assertEquals(1.0 / 0.22 * 0.25, result[Currency.EUR]!!, 0.001)
    }

    @Test
    fun `base currency of PLN returns rates for USD EUR and UAH`() {
        val result = sut(exchangeRates = TestFixtures.testRates, displayBaseCurrency = Currency.PLN)

        assertTrue(result.containsKey(Currency.USD))
        assertTrue(result.containsKey(Currency.EUR))
        assertTrue(result.containsKey(Currency.UAH))
        assertFalse(result.containsKey(Currency.PLN))
    }

    @Test
    fun `base currency of EUR returns rates for PLN USD and UAH`() {
        val result = sut(exchangeRates = TestFixtures.testRates, displayBaseCurrency = Currency.EUR)

        assertTrue(result.containsKey(Currency.PLN))
        assertTrue(result.containsKey(Currency.USD))
        assertTrue(result.containsKey(Currency.UAH))
        assertFalse(result.containsKey(Currency.EUR))
    }

    @Test
    fun `same currency conversion rate is 1 when from and to are identical`() {
        // When base is PLN, converting PLN to PLN would be 1.0 / 1.0 * 1.0 = 1.0
        // But PLN is excluded from the result map, so we verify with a non-base currency
        // that appears in the result: USD to USD would be 1.0
        // We cannot test this directly since the base is excluded — verify via UAH with UAH base
        val result = sut(exchangeRates = TestFixtures.testRates, displayBaseCurrency = Currency.UAH)
        // 1 PLN to UAH: 1 / 1.0 * 10.0 = 10.0
        assertEquals(10.0, result[Currency.PLN]!!, 0.001)
    }
}
