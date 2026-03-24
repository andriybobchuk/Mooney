package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Transaction
import kotlinx.datetime.LocalDate

class CalculateDailyTotalUseCase(
    private val currencyManagerUseCase: CurrencyManagerUseCase
) {
    private val baseCurrency = GlobalConfig.baseCurrency

    operator fun invoke(
        transactions: List<Transaction>,
        date: LocalDate
    ): Double {
        val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
        return transactions.filter { it.date == date }
            .filter {
                it.subcategory.type == CategoryType.EXPENSE &&
                    !CalculateTaxesUseCase.isTaxTransaction(it)
            }
            .sumOf {
                exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
            }
    }
}