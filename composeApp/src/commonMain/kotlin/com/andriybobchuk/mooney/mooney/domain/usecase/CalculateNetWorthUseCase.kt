package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.presentation.account.UiAccount

class CalculateNetWorthUseCase {
    val exchangeRates = GlobalConfig.testExchangeRates

    data class NetWorthResult(
        val totalNetWorth: Double,
        val currency: Currency
    )
    
    operator fun invoke(
        accounts: List<UiAccount?>,
        selectedCurrency: Currency,
        baseCurrency: Currency
    ): NetWorthResult {
        val totalPln = accounts.filterNotNull().sumOf { it.baseCurrencyAmount }

        val converted = if (selectedCurrency != baseCurrency) {
            exchangeRates.convert(
                amount = totalPln,
                from = baseCurrency,
                to = selectedCurrency
            )
        } else {
            totalPln
        }

        return NetWorthResult(
            totalNetWorth = converted,
            currency = selectedCurrency
        )
    }
} 