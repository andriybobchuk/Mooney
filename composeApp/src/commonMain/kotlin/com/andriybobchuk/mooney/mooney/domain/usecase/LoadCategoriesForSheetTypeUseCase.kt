package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.CategorySheetType
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.TopCategorySummary
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas

class LoadCategoriesForSheetTypeUseCase(
    private val repository: CoreRepository
) {

    operator fun invoke(
        sheetType: CategorySheetType,
        currentTransactions: List<Transaction>,
        previousMonthTransactions: List<Transaction>,
        baseCurrency: Currency,
        exchangeRates: ExchangeRates
    ): List<TopCategorySummary> {
        val allCategories = repository.getAllCategories()
        val relevantCategories = when (sheetType) {
            CategorySheetType.REVENUE -> listOf("salary", "tax_return", "refund", "repayment", "positive_reconciliation")
                .mapNotNull { id -> allCategories.find { it.id == id } }
            CategorySheetType.TAXES -> repository.getSubcategories("tax")
            CategorySheetType.OPERATING_COSTS -> {
                allCategories.filter { category ->
                    category.type == CategoryType.EXPENSE &&
                        category.parent?.parent == null &&
                        category.parent != null &&
                        category.id != "tax"
                }
            }
        }

        return relevantCategories.mapNotNull { category ->
            val categoryTransactions = currentTransactions.filter { transaction ->
                transaction.subcategory == category || transaction.subcategory.parent == category
            }

            val currentAmount = categoryTransactions.sumOf {
                exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
            }

            val previousCategoryTransactions = previousMonthTransactions.filter { transaction ->
                transaction.subcategory == category || transaction.subcategory.parent == category
            }

            val previousAmount = previousCategoryTransactions.sumOf {
                exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
            }

            val trendPercentage = if (previousAmount != 0.0) {
                ((currentAmount - previousAmount) / previousAmount) * 100
            } else if (currentAmount > 0) {
                100.0
            } else {
                0.0
            }

            if (currentAmount > 0) {
                TopCategorySummary(
                    category = category,
                    amount = currentAmount,
                    formatted = "${currentAmount.formatWithCommas()} ${baseCurrency.symbol}",
                    percentOfRevenue = "",
                    trendPercentage = trendPercentage
                )
            } else {
                null
            }
        }.sortedByDescending { it.amount }
    }
}
