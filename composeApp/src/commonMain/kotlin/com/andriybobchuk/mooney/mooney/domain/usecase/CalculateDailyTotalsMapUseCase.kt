package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.Transaction
import kotlinx.datetime.LocalDate

class CalculateDailyTotalsMapUseCase(
    private val calculateDailyTotalUseCase: CalculateDailyTotalUseCase
) {
    operator fun invoke(
        transactions: List<Transaction>,
        month: MonthKey
    ): Map<Int, Double> {
        return transactions
            .groupBy { it.date.dayOfMonth }
            .mapValues { (_, dayTransactions) ->
                calculateDailyTotalUseCase(
                    dayTransactions,
                    dayTransactions.firstOrNull()?.date
                        ?: LocalDate(month.year, month.month, 1)
                )
            }
    }
}
