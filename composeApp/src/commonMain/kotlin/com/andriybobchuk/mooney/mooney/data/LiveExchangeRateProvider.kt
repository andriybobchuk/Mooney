package com.andriybobchuk.mooney.mooney.data

import com.andriybobchuk.mooney.core.data.safeCall
import com.andriybobchuk.mooney.core.domain.DataError
import com.andriybobchuk.mooney.core.domain.Result
import com.andriybobchuk.mooney.core.domain.map
import com.andriybobchuk.mooney.mooney.data.network.FrankfurterLatestResponse
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRateProvider
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import io.ktor.client.HttpClient
import io.ktor.client.request.get

class LiveExchangeRateProvider(
    private val httpClient: HttpClient
) : ExchangeRateProvider {

    companion object {
        private const val BASE_URL = "https://api.frankfurter.dev/v1"
    }

    override suspend fun getExchangeRates(baseCurrency: Currency): Result<ExchangeRates, DataError.Remote> {
        return safeCall<FrankfurterLatestResponse> {
            httpClient.get("$BASE_URL/latest?from=${baseCurrency.name}")
        }.map { response ->
            mapToExchangeRates(response, baseCurrency)
        }
    }

    // Approximate rates for currencies not covered by ECB/Frankfurter
    // Updated periodically — good enough for display, not for trading
    private val fallbackRatesInUsd = mapOf(
        "UAH" to 41.2,
        "RUB" to 96.5,
        "AED" to 3.67
    )

    private fun mapToExchangeRates(response: FrankfurterLatestResponse, baseCurrency: Currency): ExchangeRates {
        val supportedCurrencyRates = mutableMapOf<Currency, Double>()
        supportedCurrencyRates[baseCurrency] = 1.0

        Currency.entries.forEach { currency ->
            if (currency != baseCurrency) {
                val apiRate = response.rates[currency.name]
                if (apiRate != null) {
                    supportedCurrencyRates[currency] = apiRate
                } else {
                    // Fallback: cross-calculate via USD approximation.
                    //
                    // Frankfurter returns `rates[X]` = "X per 1 BASE". So when base is
                    // PLN, `response.rates["USD"]` = USD per 1 PLN (e.g. 0.24).
                    // `fallback` = target per 1 USD (e.g. 41.2 UAH per USD).
                    // To get target per 1 BASE we MULTIPLY: 0.24 USD/PLN * 41.2 UAH/USD
                    // = 9.9 UAH/PLN. The previous code divided, which produced rates
                    // ~170x too high when base was not USD — visible to anyone with
                    // UAH/RUB/AED accounts on a EUR/PLN base.
                    val fallback = fallbackRatesInUsd[currency.name]
                    if (fallback != null) {
                        val usdRate = if (baseCurrency == Currency.USD) 1.0
                            else response.rates["USD"] ?: return@forEach
                        supportedCurrencyRates[currency] = fallback * usdRate
                    }
                }
            }
        }

        return ExchangeRates(rates = supportedCurrencyRates)
    }
}
