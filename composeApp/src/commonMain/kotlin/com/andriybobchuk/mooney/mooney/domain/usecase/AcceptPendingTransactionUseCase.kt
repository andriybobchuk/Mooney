package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.core.data.database.PendingTransactionDao
import com.andriybobchuk.mooney.core.data.database.PendingTransactionEntity
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.Transaction
import kotlinx.datetime.LocalDate

class AcceptPendingTransactionUseCase(
    private val pendingTransactionDao: PendingTransactionDao,
    private val addTransactionUseCase: AddTransactionUseCase
) {
    suspend operator fun invoke(
        pending: PendingTransactionEntity,
        account: Account,
        subcategory: Category
    ) {
        val transaction = Transaction(
            id = 0,
            subcategory = subcategory,
            amount = pending.amount,
            account = account,
            date = LocalDate.parse(pending.scheduledDate),
            // Carry the note from the pending row (which itself came from
            // the recurring template) so the user sees the same context on
            // the materialized transaction.
            description = pending.description
        )
        addTransactionUseCase(transaction)
        pendingTransactionDao.updateStatus(pending.id, "ACCEPTED")
    }
}
