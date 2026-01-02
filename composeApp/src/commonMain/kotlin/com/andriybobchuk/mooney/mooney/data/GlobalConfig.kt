package com.andriybobchuk.mooney.mooney.data

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates

object GlobalConfig {
    val baseCurrency = Currency.PLN

    val testExchangeRates = ExchangeRates(
        rates = mapOf(
            Currency.PLN to 1.0,     // base
            Currency.USD to 0.27,    // 1 PLN = 0.27 USD
            Currency.EUR to 0.24,     // 1 PLN = 0.24 EUR
            Currency.UAH to 11.08     // 1 PLN = 11.08 UAH
        )
    )
}