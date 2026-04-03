package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.AccountWithConversion
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates

class ConvertAccountsToUiUseCase(
    private val currencyManagerUseCase: CurrencyManagerUseCase
) {
    operator fun invoke(accounts: List<Account?>): List<AccountWithConversion?> {
        val baseCurrency = GlobalConfig.baseCurrency
        val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
        return accounts.map { account ->
            if (account?.currency == baseCurrency) {
                AccountWithConversion(
                    id = account.id,
                    title = account.title,
                    emoji = account.emoji,
                    originalAmount = account.amount,
                    originalCurrency = account.currency,
                    baseCurrencyAmount = account.amount,
                    exchangeRate = null,
                    assetCategory = account.assetCategory,
                    assetCategoryId = account.assetCategoryId,
                    isPrimary = account.isPrimary,
                    isLiability = account.isLiability
                )
            } else {
                account?.let {
                    val rate = exchangeRates.convert(1.0, account.currency, baseCurrency)
                    val converted = exchangeRates.convert(account.amount, account.currency, baseCurrency)
                    AccountWithConversion(
                        id = account.id,
                        title = account.title,
                        emoji = account.emoji,
                        originalAmount = account.amount,
                        originalCurrency = account.currency,
                        baseCurrencyAmount = converted,
                        exchangeRate = rate,
                        assetCategory = account.assetCategory,
                        assetCategoryId = account.assetCategoryId,
                        isPrimary = account.isPrimary,
                        isLiability = account.isLiability
                    )
                }
            }
        }
    }
}
