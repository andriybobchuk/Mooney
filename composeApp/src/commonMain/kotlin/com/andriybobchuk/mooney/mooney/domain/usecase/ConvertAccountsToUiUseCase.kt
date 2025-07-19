package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.presentation.account.UiAccount

class ConvertAccountsToUiUseCase(
    private val exchangeRates: ExchangeRates,
    private val baseCurrency: Currency
) {
    operator fun invoke(accounts: List<Account?>): List<UiAccount?> {
        return accounts.map { account ->
            if (account?.currency == baseCurrency) {
                UiAccount(
                    id = account.id,
                    title = account.title,
                    emoji = account.emoji,
                    originalAmount = account.amount,
                    originalCurrency = account.currency,
                    baseCurrencyAmount = account.amount,
                    exchangeRate = null
                )
            } else {
                account?.let {
                    val rate = exchangeRates.convert(1.0, account.currency, baseCurrency)
                    val converted = exchangeRates.convert(account.amount, account.currency, baseCurrency)
                    UiAccount(
                        id = account.id,
                        title = account.title,
                        emoji = account.emoji,
                        originalAmount = account.amount,
                        originalCurrency = account.currency,
                        baseCurrencyAmount = converted,
                        exchangeRate = rate
                    )
                }
            }
        }
    }
} 