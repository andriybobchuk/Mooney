package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.data.CategoryDataSource
import com.andriybobchuk.mooney.mooney.domain.CategorySheetType
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.TopCategorySummary
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas

class LoadCategoriesForSheetTypeUseCase {

    operator fun invoke(
        sheetType: CategorySheetType,
        currentTransactions: List<Transaction>,
        previousMonthTransactions: List<Transaction>,
        baseCurrency: Currency,
        exchangeRates: ExchangeRates
    ): List<TopCategorySummary> {
        val relevantCategories = when (sheetType) {
            CategorySheetType.REVENUE -> listOf(
                CategoryDataSource.salary,
                CategoryDataSource.tax_return,
                CategoryDataSource.refund,
                CategoryDataSource.repayment,
                CategoryDataSource.positive_reconciliation
            )
            CategorySheetType.TAXES -> CategoryDataSource.taxSub
            CategorySheetType.OPERATING_COSTS -> {
                CategoryDataSource.categories.filter { category ->
                    category.type == CategoryType.EXPENSE &&
                        category.parent == CategoryDataSource.expense &&
                        category != CategoryDataSource.tax
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
