package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.Transaction

class CalculateTaxesUseCase {

    operator fun invoke(
        transactions: List<Transaction>,
        baseCurrency: Currency,
        exchangeRates: ExchangeRates
    ): Double {
        return transactions
            .filter { isTaxTransaction(it) }
            .sumOf { exchangeRates.convert(it.amount, it.account.currency, baseCurrency) }
    }

    companion object {
        fun isTaxTransaction(transaction: Transaction): Boolean {
            return transaction.subcategory.title.contains("ZUS", ignoreCase = true) ||
                transaction.subcategory.title.contains("PIT", ignoreCase = true)
        }
    }
}
