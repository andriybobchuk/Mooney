package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.CategoryType

sealed interface TransactionValidation {
    data object Valid : TransactionValidation
    data class Warning(val message: String) : TransactionValidation
    data class Error(val message: String) : TransactionValidation
}

class ValidateTransactionUseCase {
    @Suppress("ReturnCount")
    operator fun invoke(
        amount: Double,
        transactionType: CategoryType,
        sourceAccount: Account?,
        destinationAccount: Account? = null
    ): TransactionValidation {
        if (amount <= 0) return TransactionValidation.Error("Enter an amount")
        if (sourceAccount == null) return TransactionValidation.Error("Select an account")

        if (transactionType == CategoryType.TRANSFER) {
            if (destinationAccount == null) return TransactionValidation.Error("Select a destination account")
            if (sourceAccount.currency != destinationAccount.currency) {
                return TransactionValidation.Error("Accounts are in different currencies")
            }
            if (amount > sourceAccount.amount) {
                return TransactionValidation.Warning("Balance on ${sourceAccount.title} will go negative")
            }
        }

        if (transactionType == CategoryType.EXPENSE && amount > sourceAccount.amount) {
            return TransactionValidation.Warning("Balance on ${sourceAccount.title} will go negative")
        }

        return TransactionValidation.Valid
    }
}
