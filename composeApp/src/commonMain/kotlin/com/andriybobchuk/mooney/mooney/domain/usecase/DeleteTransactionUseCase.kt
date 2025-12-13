package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.CategoryType

class DeleteTransactionUseCase(
    private val repository: CoreRepository
) {
    private val transferHandler = TransferHandler(repository)

    suspend operator fun invoke(id: Int) {
        val transaction = repository.getTransactionById(id)

        if (transaction != null) {
            val categoryType = transferHandler.getCategoryType(transaction)

            if (categoryType == CategoryType.TRANSFER) {
                transferHandler.reverseTransferEffect(transaction)
            } else if (categoryType != null) {
                transferHandler.reverseRegularTransactionEffect(transaction, categoryType)
            }

            repository.deleteTransaction(id)
        }
    }
} 