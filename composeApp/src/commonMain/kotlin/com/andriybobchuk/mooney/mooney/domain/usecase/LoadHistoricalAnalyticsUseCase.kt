package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.MonthlyMetricSnapshot

class LoadHistoricalAnalyticsUseCase(
    private val calculateMonthlyAnalyticsUseCase: CalculateMonthlyAnalyticsUseCase,
    private val calculateTaxesUseCase: CalculateTaxesUseCase,
    private val currencyManagerUseCase: CurrencyManagerUseCase
) {
    /**
     * @param anchorMonth the newest month in the returned window. Defaults to
     *   today for the standard past-only views (6mo, 1y, Lifetime), but the
     *   Future/Planning view passes the furthest month with transactions —
     *   e.g. anchor=Oct 2026 + monthCount=6 → May-Oct 2026 (a mix of past +
     *   future). Result is sorted oldest-first.
     */
    suspend operator fun invoke(
        anchorMonth: MonthKey,
        monthCount: Int = 12,
        baseCurrency: Currency
    ): List<MonthlyMetricSnapshot> {
        val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
        val historicalData = mutableListOf<MonthlyMetricSnapshot>()

        repeat(monthCount) { offset ->
            val month = anchorMonth.monthsAgo(offset)
            val start = month.firstDay()
            val end = month.firstDayOfNextMonth()

            try {
                val analytics = calculateMonthlyAnalyticsUseCase(start, end, baseCurrency)
                val taxes = calculateTaxesUseCase(analytics.transactions, baseCurrency, exchangeRates)

                historicalData.add(
                    MonthlyMetricSnapshot(
                        month = month,
                        revenue = analytics.totalRevenue,
                        taxes = taxes,
                        operatingCosts = analytics.totalExpenses,
                        netIncome = analytics.totalRevenue - taxes - analytics.totalExpenses,
                        transactionCount = analytics.transactions.size
                    )
                )
            } catch (_: Exception) {
                // Skip month if data unavailable
            }
        }

        return historicalData.reversed()
    }
}
