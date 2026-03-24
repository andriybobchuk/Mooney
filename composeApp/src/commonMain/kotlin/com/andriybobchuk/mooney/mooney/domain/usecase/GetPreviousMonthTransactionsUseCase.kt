package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.Transaction
import kotlinx.coroutines.flow.first

class GetPreviousMonthTransactionsUseCase(
    private val getTransactionsUseCase: GetTransactionsUseCase
) {
    suspend operator fun invoke(currentMonth: MonthKey): List<Transaction> {
        val previousMonth = currentMonth.previousMonth()
        val previousStart = previousMonth.firstDay()
        val previousEnd = previousMonth.firstDayOfNextMonth()

        return try {
            getTransactionsUseCase().first().filterNotNull().filter { transaction ->
                transaction.date >= previousStart && transaction.date < previousEnd
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
