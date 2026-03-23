package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.AssetCategory
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRateProvider
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateNetWorthUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.CurrencyManagerUseCase
import com.andriybobchuk.mooney.core.domain.DataError
import com.andriybobchuk.mooney.core.domain.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculateNetWorthUseCaseTest {

    private val testRates = ExchangeRates(
        rates = mapOf(
            Currency.USD to 1.0,
            Currency.EUR to 0.85,
            Currency.PLN to 4.0,
            Currency.UAH to 37.0
        )
    )

    private val fakeExchangeRateProvider = object : ExchangeRateProvider {
        override suspend fun getExchangeRates(baseCurrency: Currency): Result<ExchangeRates, DataError.Remote> {
            return Result.Success(testRates)
        }
    }

    private val currencyManager = CurrencyManagerUseCase(fakeExchangeRateProvider)
    private val sut = CalculateNetWorthUseCase(currencyManager)

    @BeforeTest
    fun setup() = runTest {
        currencyManager.refreshExchangeRates()
    }

    @Test
    fun `net worth with single account in base currency`() {
        val accounts = listOf(
            account(amount = 1000.0, currency = Currency.PLN)
        )

        val result = sut(accounts, selectedCurrency = Currency.PLN, baseCurrency = Currency.PLN)

        assertEquals(1000.0, result.totalNetWorth, 0.01)
        assertEquals(Currency.PLN, result.currency)
    }

    @Test
    fun `net worth with multiple accounts in same currency`() {
        val accounts = listOf(
            account(id = 1, amount = 500.0, currency = Currency.PLN),
            account(id = 2, amount = 300.0, currency = Currency.PLN)
        )

        val result = sut(accounts, selectedCurrency = Currency.PLN, baseCurrency = Currency.PLN)

        assertEquals(800.0, result.totalNetWorth, 0.01)
    }

    @Test
    fun `net worth converts foreign currency to base`() {
        val accounts = listOf(
            account(id = 1, amount = 100.0, currency = Currency.USD),
            account(id = 2, amount = 500.0, currency = Currency.PLN)
        )

        // 100 USD = 100 / 1.0 * 4.0 = 400 PLN
        // Total = 400 + 500 = 900 PLN
        val result = sut(accounts, selectedCurrency = Currency.PLN, baseCurrency = Currency.PLN)

        assertEquals(900.0, result.totalNetWorth, 0.01)
    }

    @Test
    fun `net worth displayed in different selected currency`() {
        val accounts = listOf(
            account(amount = 400.0, currency = Currency.PLN)
        )

        // 400 PLN = 400 / 4.0 * 1.0 = 100 USD
        val result = sut(accounts, selectedCurrency = Currency.USD, baseCurrency = Currency.PLN)

        assertEquals(100.0, result.totalNetWorth, 0.01)
        assertEquals(Currency.USD, result.currency)
    }

    @Test
    fun `net worth with empty accounts returns zero`() {
        val result = sut(emptyList(), selectedCurrency = Currency.PLN, baseCurrency = Currency.PLN)

        assertEquals(0.0, result.totalNetWorth, 0.01)
    }

    private fun account(
        id: Int = 1,
        amount: Double = 0.0,
        currency: Currency = Currency.PLN,
        title: String = "Test",
        emoji: String = "💰"
    ) = Account(
        id = id,
        title = title,
        amount = amount,
        currency = currency,
        emoji = emoji,
        assetCategory = AssetCategory.BANK_ACCOUNT
    )
}
