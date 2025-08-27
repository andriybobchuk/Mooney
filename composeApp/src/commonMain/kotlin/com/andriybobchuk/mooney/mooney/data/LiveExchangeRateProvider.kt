package com.andriybobchuk.mooney.mooney.data

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

class LiveExchangeRateProvider(
    private val httpClient: HttpClient,
    private val apiKey: String
) : ExchangeRateProvider {
    
    companion object {
        private const val BASE_URL = "https://v6.exchangerate-api.com/v6"
    }
    
    override suspend fun getExchangeRates(baseCurrency: Currency): Result<ExchangeRates, DataError.Remote> {
        return safeCall<ExchangeRateApiResponse> {
            httpClient.get("$BASE_URL/$apiKey/latest/${baseCurrency.name}")
        }.map { response ->
            mapToExchangeRates(response, baseCurrency)
        }
    }
    
    private fun mapToExchangeRates(response: ExchangeRateApiResponse, baseCurrency: Currency): ExchangeRates {
        // Filter only the currencies we support
        val supportedCurrencyRates = mutableMapOf<Currency, Double>()
        
        // Always include base currency with rate 1.0
        supportedCurrencyRates[baseCurrency] = 1.0
        
        // Add other supported currencies from API response
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