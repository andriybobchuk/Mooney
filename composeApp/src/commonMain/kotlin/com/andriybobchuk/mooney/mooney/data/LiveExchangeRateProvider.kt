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

    private fun mapToExchangeRates(response: FrankfurterLatestResponse, baseCurrency: Currency): ExchangeRates {
        val supportedCurrencyRates = mutableMapOf<Currency, Double>()
        supportedCurrencyRates[baseCurrency] = 1.0

        Currency.entries.forEach { currency ->
            if (currency != baseCurrency) {
                response.rates[currency.name]?.let { rate ->
                    supportedCurrencyRates[currency] = rate
                }
            }
        }

        return ExchangeRates(rates = supportedCurrencyRates)
    }
}
