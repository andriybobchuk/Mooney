package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.presentation.analytics.TopCategorySummary
import com.andriybobchuk.mooney.mooney.presentation.formatWithCommas

class CalculateSubcategoriesUseCase {
    private val exchangeRates = GlobalConfig.testExchangeRates
    
    operator fun invoke(
        parentCategory: Category,
        transactions: List<Transaction>,
        baseCurrency: Currency
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
        
        // Convert to TopCategorySummary and sort by amount
        return allSums
            .entries
            .sortedByDescending { it.value }
            .map { (category, amount) ->
                TopCategorySummary(
                    category = category,
                    amount = amount,
                    formatted = formatAmount(amount, baseCurrency),
                    percentOfRevenue = calculatePercentage(amount, categoryTotal)
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