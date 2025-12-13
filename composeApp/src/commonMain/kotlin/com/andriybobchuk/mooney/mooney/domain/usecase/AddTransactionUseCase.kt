package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Transaction

class AddTransactionUseCase(
    private val repository: CoreRepository
) {
    private val transferHandler = TransferHandler(repository)

    suspend operator fun invoke(transaction: Transaction) {
        val existingTransaction = repository.getTransactionById(transaction.id)

        // 1. If updating: reverse the old transaction's effect
        if (existingTransaction != null) {
            val oldCategoryType = transferHandler.getCategoryType(existingTransaction)
            
            if (oldCategoryType == CategoryType.TRANSFER) {
                transferHandler.reverseTransferEffect(existingTransaction)
            } else if (oldCategoryType != null) {
                transferHandler.reverseRegularTransactionEffect(existingTransaction, oldCategoryType)
            }
        }

        // 2. Apply the new transaction's effect
        val categoryType = transferHandler.getCategoryType(transaction)
        
        if (categoryType == CategoryType.TRANSFER) {
            transferHandler.applyTransferEffect(transaction)
        } else if (categoryType != null) {
            transferHandler.applyRegularTransactionEffect(transaction, categoryType)
        }

        // 3. Upsert transaction
        repository.upsertTransaction(transaction)
    }
} 