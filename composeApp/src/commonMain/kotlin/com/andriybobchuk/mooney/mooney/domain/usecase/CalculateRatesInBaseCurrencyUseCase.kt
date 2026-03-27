package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates

class CalculateRatesInBaseCurrencyUseCase {

    operator fun invoke(
        exchangeRates: ExchangeRates?,
        displayBaseCurrency: Currency
    ): Map<Currency, Double> {
        if (exchangeRates == null) return emptyMap()

        val rates = mutableMapOf<Currency, Double>()
        exchangeRates.rates.keys.forEach { currency ->
            if (currency != displayBaseCurrency) {
                val rate = exchangeRates.convert(1.0, currency, displayBaseCurrency)
                rates[currency] = rate
            }
        }
        return rates
    }
}
