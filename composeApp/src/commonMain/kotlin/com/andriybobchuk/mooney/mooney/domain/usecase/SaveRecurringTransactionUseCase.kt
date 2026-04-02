package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.core.data.database.RecurringTransactionDao
import com.andriybobchuk.mooney.core.data.database.RecurringTransactionEntity
import com.andriybobchuk.mooney.mooney.domain.RecurringTransaction
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class SaveRecurringTransactionUseCase(
    private val recurringTransactionDao: RecurringTransactionDao
) {
    suspend operator fun invoke(recurring: RecurringTransaction) {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

        // Preserve createdDate and lastProcessedDate on edit
        val existing = if (recurring.id != 0) {
            recurringTransactionDao.getById(recurring.id)
        } else null

        recurringTransactionDao.upsert(
            RecurringTransactionEntity(
                id = recurring.id,
                title = recurring.title,
                subcategoryId = recurring.subcategory?.id ?: "",
                amount = recurring.amount,
                accountId = recurring.account?.id ?: 0,
                dayOfMonth = recurring.schedule.dayOfMonth,
                frequency = recurring.schedule.frequency.name,
                weekDay = recurring.schedule.weekDay,
                monthOfYear = recurring.schedule.monthOfYear,
                isActive = recurring.isActive,
                createdDate = existing?.createdDate ?: today.toString(),
                lastProcessedDate = existing?.lastProcessedDate
            )
        )
    }
}
