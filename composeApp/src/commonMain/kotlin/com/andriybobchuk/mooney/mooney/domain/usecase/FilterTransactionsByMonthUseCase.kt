package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.Transaction

class FilterTransactionsByMonthUseCase {

    operator fun invoke(
        transactions: List<Transaction?>,
        month: MonthKey
    ): List<Transaction> {
        val start = month.firstDay()
        val end = month.firstDayOfNextMonth()

        return transactions.filterNotNull().filter {
            it.date >= start && it.date < end
        }
    }
}
