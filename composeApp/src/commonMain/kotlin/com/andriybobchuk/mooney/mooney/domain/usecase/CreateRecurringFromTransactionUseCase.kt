package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.RecurringSchedule
import com.andriybobchuk.mooney.mooney.domain.RecurringTransaction
import com.andriybobchuk.mooney.mooney.domain.Transaction

class CreateRecurringFromTransactionUseCase(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val saveRecurringTransactionUseCase: SaveRecurringTransactionUseCase
) {
    suspend operator fun invoke(transaction: Transaction, schedule: RecurringSchedule) {
        addTransactionUseCase(transaction)
        saveRecurringTransactionUseCase(
            RecurringTransaction(
                id = 0,
                title = transaction.subcategory.title,
                subcategory = transaction.subcategory,
                amount = transaction.amount,
                account = transaction.account,
                schedule = schedule,
                isActive = true,
                // Propagate the note the user typed on the source transaction
                // so every future generated instance shows the same context.
                description = transaction.description
            )
        )
    }
}
