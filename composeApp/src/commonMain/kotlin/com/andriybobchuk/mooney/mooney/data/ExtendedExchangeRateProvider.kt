package com.andriybobchuk.mooney.mooney.data

import com.andriybobchuk.mooney.core.data.Secrets
import com.andriybobchuk.mooney.core.data.safeCall
import com.andriybobchuk.mooney.core.domain.DataError
import com.andriybobchuk.mooney.core.domain.Result
import com.andriybobchuk.mooney.core.domain.map
import com.andriybobchuk.mooney.mooney.data.network.ExchangeRateApiResponse
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRateProvider
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import io.ktor.client.HttpClient
import io.ktor.client.request.get

/**
 * exchangerate-api.com — covers ALL currencies the app supports (incl. UAH, RUB, AED) natively,
 * but does NOT provide historical data. Use when the user prioritizes accurate live rates for
 * every currency over historical charts.
 */
class ExtendedExchangeRateProvider(
    private val httpClient: HttpClient
) : ExchangeRateProvider {

    companion object {
        private const val BASE_URL = "https://v6.exchangerate-api.com/v6"
    }

    override suspend fun getExchangeRates(baseCurrency: Currency): Result<ExchangeRates, DataError.Remote> {
        return safeCall<ExchangeRateApiResponse> {
            httpClient.get("$BASE_URL/${Secrets.EXCHANGE_RATE_API_KEY}/latest/${baseCurrency.name}")
        }.map { response ->
            mapToExchangeRates(response, baseCurrency)
        }
    }

    private fun mapToExchangeRates(response: ExchangeRateApiResponse, baseCurrency: Currency): ExchangeRates {
        val supportedCurrencyRates = mutableMapOf<Currency, Double>()
        supportedCurrencyRates[baseCurrency] = 1.0

        Currency.entries.forEach { currency ->
            if (currency != baseCurrency) {
                response.conversionRates[currency.name]?.let { rate ->
                    supportedCurrencyRates[currency] = rate
                }
            }
        }

        return ExchangeRates(rates = supportedCurrencyRates)
    }
}
