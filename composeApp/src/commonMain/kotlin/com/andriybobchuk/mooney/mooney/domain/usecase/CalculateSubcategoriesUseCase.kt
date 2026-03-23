package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.TopCategorySummary
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas

class CalculateSubcategoriesUseCase(
    private val currencyManagerUseCase: CurrencyManagerUseCase
) {
    
    operator fun invoke(
        parentCategory: Category,
        transactions: List<Transaction>,
        baseCurrency: Currency,
        previousTransactions: List<Transaction> = emptyList()
    ): List<TopCategorySummary> {
        // Filter transactions for this parent category
        val categoryTransactions = transactions.filter { transaction ->
            when {
                // If transaction is directly in the parent category
                transaction.subcategory == parentCategory -> true
                // If transaction is in a subcategory of this parent
                transaction.subcategory.parent == parentCategory -> true
                else -> false
            }
        }
        
        // Calculate total for this category to get percentages
        val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
        val categoryTotal = categoryTransactions.sumOf { 
            exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
        }
        
        // Group by subcategories
        val subcategorySums = categoryTransactions
            .filter { it.subcategory.isSubCategory() && it.subcategory.parent == parentCategory }
            .groupBy { it.subcategory }
            .mapValues { (_, transactions) ->
                transactions.sumOf { 
                    exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
                }
            }
        
        // Add the parent category itself if there are transactions directly assigned to it
        val parentCategorySum = categoryTransactions
            .filter { it.subcategory == parentCategory }
            .sumOf { 
                exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
            }
        
        val allSums = mutableMapOf<Category, Double>()
        allSums.putAll(subcategorySums)
        if (parentCategorySum > 0) {
            allSums[parentCategory] = parentCategorySum
        }
        
        // Calculate previous month data for trends
        val previousCategoryTransactions = previousTransactions.filter { transaction ->
            when {
                transaction.subcategory == parentCategory -> true
                transaction.subcategory.parent == parentCategory -> true
                else -> false
            }
        }
        
        val previousSubcategorySums = previousCategoryTransactions
            .filter { it.subcategory.isSubCategory() && it.subcategory.parent == parentCategory }
            .groupBy { it.subcategory }
            .mapValues { (_, transactions) ->
                transactions.sumOf { 
                    exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
                }
            }
        
        val previousParentCategorySum = previousCategoryTransactions
            .filter { it.subcategory == parentCategory }
            .sumOf { 
                exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
            }
        
        val allPreviousSums = mutableMapOf<Category, Double>()
        allPreviousSums.putAll(previousSubcategorySums)
        if (previousParentCategorySum > 0) {
            allPreviousSums[parentCategory] = previousParentCategorySum
        }
        
        // Convert to TopCategorySummary and sort by amount
        return allSums
            .entries
            .sortedByDescending { it.value }
            .map { (category, amount) ->
                val previousAmount = allPreviousSums[category] ?: 0.0
                val trendPercentage = if (previousAmount != 0.0) {
                    ((amount - previousAmount) / previousAmount) * 100
                } else if (amount > 0) {
                    100.0 // New subcategory this month
                } else {
                    0.0
                }
                
                TopCategorySummary(
                    category = category,
                    amount = amount,
                    formatted = formatAmount(amount, baseCurrency),
                    percentOfRevenue = calculatePercentage(amount, categoryTotal),
                    trendPercentage = trendPercentage
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