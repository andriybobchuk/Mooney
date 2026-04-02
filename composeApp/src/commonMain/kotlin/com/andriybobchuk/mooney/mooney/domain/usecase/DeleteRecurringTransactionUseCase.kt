package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.core.data.database.RecurringTransactionDao

class DeleteRecurringTransactionUseCase(
    private val recurringTransactionDao: RecurringTransactionDao
) {
    suspend operator fun invoke(id: Int) {
        recurringTransactionDao.delete(id)
    }
}
