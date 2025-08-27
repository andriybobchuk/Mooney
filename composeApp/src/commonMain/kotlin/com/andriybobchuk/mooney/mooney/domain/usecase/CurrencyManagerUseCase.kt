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
    val baseCurrency = GlobalConfig.baseCurrency
    
    // Cached exchange rates
    private var cachedExchangeRates: ExchangeRates = GlobalConfig.testExchangeRates
    private var selectedCurrencyIndex = 0
    private var selectedCurrency = baseCurrency

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

    fun getAvailableCurrencies(): List<Currency> = cachedExchangeRates.rates.keys.toList()

    fun cycleToNextCurrency(): Currency {
        val availableCurrencies = getAvailableCurrencies()
        selectedCurrencyIndex = (selectedCurrencyIndex + 1) % availableCurrencies.size
        selectedCurrency = availableCurrencies[selectedCurrencyIndex]
        return selectedCurrency
    }

    fun resetToBaseCurrency() {
        val availableCurrencies = getAvailableCurrencies()
        selectedCurrencyIndex = availableCurrencies.indexOf(baseCurrency)
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