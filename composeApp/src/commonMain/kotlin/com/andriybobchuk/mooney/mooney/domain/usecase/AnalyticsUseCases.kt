package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.presentation.analytics.TopCategorySummary
import com.andriybobchuk.mooney.mooney.presentation.formatWithCommas
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

class CalculateMonthlyAnalyticsUseCase(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val currencyManagerUseCase: CurrencyManagerUseCase
) {

    suspend operator fun invoke(
        startDate: LocalDate,
        endDate: LocalDate,
        baseCurrency: Currency
    ): MonthlyAnalyticsResult {
        val allTransactions = getTransactionsUseCase().first()
        val monthlyTransactions = allTransactions.filterNotNull().filter { 
            it.date >= startDate && it.date < endDate 
        }
        
        val totalRevenue = calculateTotalRevenue(monthlyTransactions, baseCurrency)
        val totalExpenses = calculateTotalExpenses(monthlyTransactions, baseCurrency)
        val topCategories = calculateTopCategories(monthlyTransactions, totalExpenses, baseCurrency)
        
        return MonthlyAnalyticsResult(
            totalRevenue = totalRevenue,
            totalExpenses = totalExpenses,
            topCategories = topCategories,
            transactions = monthlyTransactions
        )
    }
    
    private fun calculateTotalRevenue(
        transactions: List<Transaction>,
        baseCurrency: Currency
    ): Double {
        val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
        return transactions
            .filter { it.subcategory.type == CategoryType.INCOME }
            .sumOf { 
                exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
            }
    }
    
    private fun calculateTotalExpenses(
        transactions: List<Transaction>,
        baseCurrency: Currency
    ): Double {
        val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
        return transactions
            .filter { 
                it.subcategory.type == CategoryType.EXPENSE && 
                !it.subcategory.title.contains("ZUS") && 
                !it.subcategory.title.contains("PIT")
            }
            .sumOf { 
                exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
            }
    }
    
    private fun calculateTopCategories(
        transactions: List<Transaction>,
        totalExpenses: Double,
        baseCurrency: Currency
    ): List<TopCategorySummary> {
        val categorySums = transactions
            .filter { it.subcategory.type == CategoryType.EXPENSE }
            .groupBy { transaction ->
                when {
                    transaction.subcategory.isGeneralCategory() -> transaction.subcategory
                    transaction.subcategory.isTypeCategory() -> transaction.subcategory
                    else -> transaction.subcategory.parent!!
                }
            }
            .mapValues { (_, transactions) ->
                val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
                transactions.sumOf { 
                    exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
                }
            }
        
        return categorySums
            .entries
            .sortedByDescending { it.value }
            .map { (category, amount) ->
                TopCategorySummary(
                    category = category,
                    amount = amount,
                    formatted = formatAmount(amount, baseCurrency),
                    percentOfRevenue = calculatePercentage(amount, totalExpenses)
                )
            }
    }
    
    private fun formatAmount(amount: Double, currency: Currency): String {
        return "${amount.formatWithCommas()} ${currency.symbol}"
    }
    
    private fun calculatePercentage(part: Double, total: Double): String {
        return if (total == 0.0) "–" else (part / total * 100).formatWithCommas()
    }
}

data class MonthlyAnalyticsResult(
    val totalRevenue: Double,
    val totalExpenses: Double,
    val topCategories: List<TopCategorySummary>,
    val transactions: List<Transaction>
)

 