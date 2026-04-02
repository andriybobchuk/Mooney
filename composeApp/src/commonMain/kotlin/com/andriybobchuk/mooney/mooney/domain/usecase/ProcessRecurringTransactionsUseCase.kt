package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.core.data.database.PendingTransactionDao
import com.andriybobchuk.mooney.core.data.database.PendingTransactionEntity
import com.andriybobchuk.mooney.core.data.database.RecurringTransactionDao
import com.andriybobchuk.mooney.core.data.database.RecurringTransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class ProcessRecurringTransactionsUseCase(
    private val recurringTransactionDao: RecurringTransactionDao,
    private val pendingTransactionDao: PendingTransactionDao
) {
    suspend operator fun invoke() {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        val activeRecurring = recurringTransactionDao.getAllActive().first()

        for (recurring in activeRecurring) {
            val lastProcessed = recurring.lastProcessedDate?.let {
                try { LocalDate.parse(it) } catch (_: Exception) { null }
            }

            val dueDate = calculateNextDueDate(recurring, lastProcessed, today)

            if (dueDate != null && dueDate <= today) {
                // Check if we already created a pending for this date
                val existingPending = pendingTransactionDao.getAllPending().first()
                val alreadyExists = existingPending.any {
                    it.recurringTransactionId == recurring.id &&
                    it.scheduledDate == dueDate.toString()
                }

                if (!alreadyExists) {
                    pendingTransactionDao.upsert(
                        PendingTransactionEntity(
                            recurringTransactionId = recurring.id,
                            subcategoryId = recurring.subcategoryId,
                            amount = recurring.amount,
                            accountId = recurring.accountId,
                            scheduledDate = dueDate.toString(),
                            status = "PENDING",
                            createdDate = today.toString()
                        )
                    )
                    recurringTransactionDao.updateLastProcessedDate(recurring.id, dueDate.toString())
                }
            }
        }
    }

    private fun calculateNextDueDate(
        recurring: RecurringTransactionEntity,
        lastProcessed: LocalDate?,
        today: LocalDate
    ): LocalDate? {
        if (lastProcessed == null) {
            // Never processed — due now based on creation date
            return when (recurring.frequency) {
                "MONTHLY" -> {
                    val day = recurring.dayOfMonth.coerceAtMost(28) // Safe day
                    LocalDate(today.year, today.monthNumber, day)
                }
                "WEEKLY" -> today // Due immediately
                "YEARLY" -> {
                    val month = recurring.monthOfYear ?: today.monthNumber
                    val day = recurring.dayOfMonth.coerceAtMost(28)
                    LocalDate(today.year, month, day)
                }
                else -> today
            }
        }

        return when (recurring.frequency) {
            "DAILY" -> lastProcessed.plus(1, DateTimeUnit.DAY)
            "WEEKLY" -> lastProcessed.plus(7, DateTimeUnit.DAY)
            "MONTHLY" -> {
                val nextMonth = lastProcessed.plus(1, DateTimeUnit.MONTH)
                val day = recurring.dayOfMonth.coerceAtMost(28)
                LocalDate(nextMonth.year, nextMonth.monthNumber, day)
            }
            "YEARLY" -> lastProcessed.plus(1, DateTimeUnit.YEAR)
            else -> null
        }
    }
}
