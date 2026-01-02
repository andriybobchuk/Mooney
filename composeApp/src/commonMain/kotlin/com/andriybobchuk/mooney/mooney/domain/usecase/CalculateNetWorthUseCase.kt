package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.Account

class CalculateNetWorthUseCase(
    private val currencyManagerUseCase: CurrencyManagerUseCase
) {

    data class NetWorthResult(
        val totalNetWorth: Double,
        val currency: Currency
    )
    
    operator fun invoke(
        accounts: List<Account>,
        selectedCurrency: Currency,
        baseCurrency: Currency
    ): NetWorthResult {
        val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
        val totalBaseCurrency = accounts.sumOf { account ->
            if (account.currency != baseCurrency) {
                exchangeRates.convert(account.amount, account.currency, baseCurrency)
            } else {
                account.amount
            }
        }

        val converted = if (selectedCurrency != baseCurrency) {
            exchangeRates.convert(
                amount = totalBaseCurrency,
                from = baseCurrency,
                to = selectedCurrency
            )
        } else {
            totalBaseCurrency
        }

        return NetWorthResult(
            totalNetWorth = converted,
            currency = selectedCurrency
        )
    }
} 