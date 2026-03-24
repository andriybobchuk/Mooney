package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.AnalyticsMetric
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas

class CalculateAnalyticsMetricsUseCase(
    private val calculateTaxesUseCase: CalculateTaxesUseCase
) {
    operator fun invoke(
        currentRevenue: Double,
        currentExpenses: Double,
        currentTransactions: List<Transaction>,
        previousRevenue: Double,
        previousExpenses: Double,
        previousTransactions: List<Transaction>,
        baseCurrency: Currency,
        exchangeRates: ExchangeRates
    ): List<AnalyticsMetric> {
        val currentTaxes = calculateTaxesUseCase(currentTransactions, baseCurrency, exchangeRates)
        val previousTaxes = calculateTaxesUseCase(previousTransactions, baseCurrency, exchangeRates)

        val currentNetIncome = currentRevenue - currentTaxes - currentExpenses
        val previousNetIncome = previousRevenue - previousTaxes - previousExpenses

        return listOf(
            buildMetric(
                title = "Revenue",
                current = currentRevenue,
                previous = previousRevenue,
                baseCurrency = baseCurrency,
                color = 0xFF4CAF50,
                revenueForSubtitle = null
            ),
            buildMetric(
                title = "Taxes",
                current = currentTaxes,
                previous = previousTaxes,
                baseCurrency = baseCurrency,
                color = 0xFFFF9800,
                revenueForSubtitle = currentRevenue
            ),
            buildMetric(
                title = "Operating Costs",
                current = currentExpenses,
                previous = previousExpenses,
                baseCurrency = baseCurrency,
                color = 0xFFF44336,
                revenueForSubtitle = currentRevenue
            ),
            buildMetric(
                title = "Net Income",
                current = currentNetIncome,
                previous = previousNetIncome,
                baseCurrency = baseCurrency,
                color = 0xFF2196F3,
                revenueForSubtitle = currentRevenue
            )
        )
    }

    private fun buildMetric(
        title: String,
        current: Double,
        previous: Double,
        baseCurrency: Currency,
        color: Long,
        revenueForSubtitle: Double?
    ): AnalyticsMetric {
        val trendPercentage = if (previous != 0.0) ((current - previous) / previous) * 100 else 0.0
        val subtitle = if (revenueForSubtitle != null && revenueForSubtitle > 0) {
            "${percentage(current, revenueForSubtitle)}% of revenue"
        } else if (revenueForSubtitle != null) {
            "–"
        } else {
            null
        }

        return AnalyticsMetric(
            title = title,
            value = "${current.formatWithCommas()} ${baseCurrency.symbol}",
            subtitle = subtitle,
            color = color,
            trendPercentage = trendPercentage,
            isClickable = true
        )
    }

    private fun percentage(part: Double, total: Double): String =
        if (total == 0.0) "–" else (part / total * 100).formatWithCommas()
}
