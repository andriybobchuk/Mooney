package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.Transaction

class CalculateTransactionTotalUseCase(
    private val exchangeRates: ExchangeRates
) {
    data class TotalResult(
        val total: Double,
        val currency: Currency
    )
    
    operator fun invoke(
        transactions: List<Transaction?>,
        selectedCurrency: Currency,
        baseCurrency: Currency
    ): TotalResult {
        val totalPln = transactions.filterNotNull().filter {
            it.subcategory.type == CategoryType.EXPENSE && 
            !it.subcategory.title.contains("ZUS") && 
            !it.subcategory.title.contains("PIT")
        }.sumOf {
            exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
        }

        val converted = if (selectedCurrency != baseCurrency) {
            exchangeRates.convert(
                totalPln,
                from = baseCurrency,
                to = selectedCurrency
            )
        } else totalPln

        return TotalResult(
            total = converted,
            currency = selectedCurrency
        )
    }
} 