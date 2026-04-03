package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRateProvider
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.core.domain.Result
import kotlinx.coroutines.flow.first

class CurrencyManagerUseCase(
    private val exchangeRateProvider: ExchangeRateProvider
) {
    // Always read live from GlobalConfig — never cache
    val baseCurrency: Currency get() = GlobalConfig.baseCurrency

    // Cached exchange rates
    private var cachedExchangeRates: ExchangeRates = GlobalConfig.testExchangeRates
    private var selectedCurrencyIndex = 0
    private var selectedCurrency = baseCurrency

    // User-selected currencies for cycling (defaults to all supported)
    private var userCurrencies: List<Currency> = listOf(baseCurrency)

    fun setUserCurrencies(codes: List<String>) {
        userCurrencies = codes.mapNotNull { code ->
            Currency.entries.find { it.name == code }
        }.ifEmpty { listOf(baseCurrency) }
        // Reset selection if current currency is no longer in the list
        if (selectedCurrency !in userCurrencies) {
            selectedCurrencyIndex = 0
            selectedCurrency = userCurrencies.first()
        }
    }

    suspend fun refreshExchangeRates(): Result<ExchangeRates, *> {
        return when (val result = exchangeRateProvider.getExchangeRates(baseCurrency)) {
            is Result.Success -> {
                cachedExchangeRates = result.data
                result
            }
            is Result.Error -> {
                // Keep using cached rates on error
                result
            }
        }
    }

    fun getCurrentExchangeRates(): ExchangeRates = cachedExchangeRates

    fun getCurrentCurrency(): Currency = selectedCurrency

    fun getAvailableCurrencies(): List<Currency> = userCurrencies

    fun cycleToNextCurrency(): Currency {
        val available = userCurrencies
        if (available.size <= 1) return selectedCurrency
        selectedCurrencyIndex = (selectedCurrencyIndex + 1) % available.size
        selectedCurrency = available[selectedCurrencyIndex]
        return selectedCurrency
    }

    fun resetToBaseCurrency() {
        selectedCurrencyIndex = userCurrencies.indexOf(baseCurrency).coerceAtLeast(0)
        selectedCurrency = baseCurrency
    }

    fun convertToBaseCurrency(amount: Double, fromCurrency: Currency): Double {
        return cachedExchangeRates.convert(amount, fromCurrency, baseCurrency)
    }

    fun getCurrentBaseCurrency(): Currency = baseCurrency

    suspend fun convertAllAccountsToBaseCurrency(accountsFlow: kotlinx.coroutines.flow.Flow<List<com.andriybobchuk.mooney.mooney.domain.Account?>>): Double {
        val accounts = accountsFlow.first()
        return accounts.filterNotNull().sumOf { account ->
            convertToBaseCurrency(account.amount, account.currency)
        }
    }
}
