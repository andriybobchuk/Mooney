package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Transaction

/**
 * Helper class to handle transfer logic consistency across use cases
 */
class TransferHandler(
    private val repository: CoreRepository
) {
    /**
     * Determines if a transaction is a transfer based on category ID pattern
     */
    fun isTransfer(transaction: Transaction): Boolean {
        return transaction.subcategory.id.startsWith("transfer_to_")
    }

    /**
     * Gets the category type for a transaction, handling dynamic transfer categories
     */
    suspend fun getCategoryType(transaction: Transaction): CategoryType? {
        return if (isTransfer(transaction)) {
            CategoryType.TRANSFER
        } else {
            repository.getAllCategories().find { it.id == transaction.subcategory.id }?.getRoot()?.type
        }
    }

    /**
     * Applies transfer effect: subtract from source (in source currency), add to
     * destination (in destination currency). For cross-currency transfers,
     * [Transaction.destinationAmount] holds the destination-side amount converted
     * at the time of transfer. For same-currency transfers it's null and we
     * use [Transaction.amount] for both sides.
     */
    suspend fun applyTransferEffect(transaction: Transaction) {
        val sourceAccount = repository.getAccountById(transaction.account.id)

        // Extract destination account ID from category ID (format: "transfer_to_X")
        val destinationAccountId = transaction.subcategory.id.removePrefix("transfer_to_").toIntOrNull()
        val destinationAccount = destinationAccountId?.let { repository.getAccountById(it) }

        if (sourceAccount != null && destinationAccount != null) {
            // Prevent same account transfers (edge case protection)
            if (sourceAccount.id != destinationAccount.id) {
                val credit = transaction.destinationAmount ?: transaction.amount
                repository.upsertAccount(sourceAccount.copy(amount = sourceAccount.amount - transaction.amount))
                repository.upsertAccount(destinationAccount.copy(amount = destinationAccount.amount + credit))
            }
            // If same account transfer, do nothing (transaction is still saved but no balance changes)
        }
    }

    /**
     * Reverses transfer effect using the same per-side amounts that were applied.
     */
    suspend fun reverseTransferEffect(transaction: Transaction) {
        val sourceAccount = repository.getAccountById(transaction.account.id)
        val destinationAccountId = transaction.subcategory.id.removePrefix("transfer_to_").toIntOrNull()
        val destinationAccount = destinationAccountId?.let { repository.getAccountById(it) }

        if (sourceAccount != null && destinationAccount != null) {
            if (sourceAccount.id != destinationAccount.id) {
                val credit = transaction.destinationAmount ?: transaction.amount
                repository.upsertAccount(sourceAccount.copy(amount = sourceAccount.amount + transaction.amount))
                repository.upsertAccount(destinationAccount.copy(amount = destinationAccount.amount - credit))
            }
        }
    }

    /**
     * Applies regular expense/income effect to account
     */
    suspend fun applyRegularTransactionEffect(transaction: Transaction, categoryType: CategoryType) {
        val account = repository.getAccountById(transaction.account.id)
        if (account != null) {
            val adjustedAmount = when (categoryType) {
                CategoryType.EXPENSE -> account.amount - transaction.amount
                CategoryType.INCOME -> account.amount + transaction.amount
                CategoryType.TRANSFER -> account.amount // Fallback, shouldn't reach here
            }
            repository.upsertAccount(account.copy(amount = adjustedAmount))
        }
    }

    /**
     * Reverses regular expense/income effect from account
     */
    suspend fun reverseRegularTransactionEffect(transaction: Transaction, categoryType: CategoryType) {
        val account = repository.getAccountById(transaction.account.id)
        if (account != null) {
            val reversedAmount = when (categoryType) {
                CategoryType.EXPENSE -> account.amount + transaction.amount
                CategoryType.INCOME -> account.amount - transaction.amount
                CategoryType.TRANSFER -> account.amount // Fallback
            }
            repository.upsertAccount(account.copy(amount = reversedAmount))
        }
    }
}