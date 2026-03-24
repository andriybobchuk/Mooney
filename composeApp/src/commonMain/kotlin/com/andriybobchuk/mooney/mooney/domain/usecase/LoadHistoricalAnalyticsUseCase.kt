package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.MonthlyMetricSnapshot

class LoadHistoricalAnalyticsUseCase(
    private val calculateMonthlyAnalyticsUseCase: CalculateMonthlyAnalyticsUseCase,
    private val calculateTaxesUseCase: CalculateTaxesUseCase,
    private val currencyManagerUseCase: CurrencyManagerUseCase
) {
    suspend operator fun invoke(
        currentMonth: MonthKey,
        monthCount: Int = 12,
        baseCurrency: Currency
    ): List<MonthlyMetricSnapshot> {
        val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
        val historicalData = mutableListOf<MonthlyMetricSnapshot>()

        repeat(monthCount) { offset ->
            val month = currentMonth.monthsAgo(offset)
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
