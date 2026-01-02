package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.Transaction
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

/**
 * Data class representing a reconciliation difference
 */
data class ReconciliationDifference(
    val account: Account,
    val oldAmount: Double,
    val newAmount: Double,
    val difference: Double,
    val isGain: Boolean
) {
    val formattedDifference: String
        get() = "${if (isGain) "+" else "-"}${abs(difference).formatAmount()} ${account.currency.symbol}"
    
    val shouldShowDialog: Boolean
        get() = abs(difference) >= MINIMUM_THRESHOLD
    
    companion object {
        internal const val MINIMUM_THRESHOLD = 0.01 // Show dialog for any meaningful difference
    }
}

/**
 * Use case for creating reconciliation transactions when account amounts change
 */
class CreateReconciliationUseCase(
    private val repository: CoreRepository
) {
    
    /**
     * Detects reconciliation difference when updating an account
     */
    suspend fun detectReconciliationDifference(
        accountId: Int,
        newAmount: Double
    ): ReconciliationDifference? {
        // Only check for existing accounts (not new ones)
        if (accountId == 0) return null
        
        val existingAccount = repository.getAccountById(accountId) ?: return null
        val difference = newAmount - existingAccount.amount
        
        // No difference or below threshold
        if (abs(difference) < ReconciliationDifference.MINIMUM_THRESHOLD) return null
        
        return ReconciliationDifference(
            account = existingAccount,
            oldAmount = existingAccount.amount,
            newAmount = newAmount,
            difference = difference,
            isGain = difference > 0
        )
    }
    
    /**
     * Creates a reconciliation transaction for the detected difference
     */
    suspend fun createReconciliationTransaction(
        reconciliationDiff: ReconciliationDifference
    ) {
        val reconciliationCategory = getReconciliationCategory(reconciliationDiff.isGain)
            ?: return // Category not found, skip reconciliation
        
        // Create updated account object with target amount for the transaction
        val updatedAccount = reconciliationDiff.account.copy(amount = reconciliationDiff.newAmount)
        
        val reconciliationTransaction = Transaction(
            id = 0, // New transaction
            subcategory = reconciliationCategory,
            amount = abs(reconciliationDiff.difference),
            account = updatedAccount, // Use account with target amount
            date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        )
        
        // Create the reconciliation transaction record only (no balance changes)
        // Balance will be set to target amount directly by the calling code
        // Use repository.upsertTransaction to ensure proper category tracking
        repository.upsertTransaction(reconciliationTransaction)
    }
    
    /**
     * Gets the appropriate reconciliation category based on gain/loss
     */
    private suspend fun getReconciliationCategory(isGain: Boolean): Category? {
        val categoryId = if (isGain) "positive_reconciliation" else "reconciliation"
        return repository.getAllCategories().find { it.id == categoryId }
    }
}

/**
 * Extension function to format double values for display
 */
private fun Double.formatAmount(): String {
    return if (this % 1.0 == 0.0) {
        this.toInt().toString()
    } else {
        // Format to 2 decimal places manually for KMP compatibility
        val rounded = (this * 100).toInt() / 100.0
        rounded.toString()
    }
}