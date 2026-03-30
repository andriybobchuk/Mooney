package com.andriybobchuk.mooney.mooney.data

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import kotlin.concurrent.Volatile

object GlobalConfig {
    @Volatile
    var baseCurrency = Currency.PLN

    val testExchangeRates = ExchangeRates(
        rates = mapOf(
            Currency.PLN to 1.0,
            Currency.USD to 0.27,
            Currency.EUR to 0.24,
            Currency.UAH to 11.08,
            Currency.GBP to 0.21,
            Currency.CHF to 0.23,
            Currency.CZK to 6.1,
            Currency.SEK to 2.7,
            Currency.NOK to 2.8,
            Currency.DKK to 1.8,
            Currency.JPY to 40.0,
            Currency.CAD to 0.36,
            Currency.AUD to 0.41,
            Currency.TRY to 9.5,
            Currency.BRL to 1.5,
            Currency.RUB to 22.0,
            Currency.AED to 0.98
        )
    )
}