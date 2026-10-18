package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.Transaction
import kotlin.math.abs
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Shape of a single reconciliation outcome. The ViewModel reads this to show
 * a confirmation ("+52.45 PLN adjustment added") after the use case runs.
 */
data class ReconcileOutcome(
    val account: Account,
    val oldBalance: Double,
    val newBalance: Double,
    /** Signed: positive = Mooney was understating (we add an income
     *  adjustment), negative = Mooney was overstating (expense adjustment). */
    val delta: Double,
    /** True iff a reconciliation transaction was actually inserted. False
     *  for exact-match reconciliations (|delta| < 0.01) where we only
     *  stamp the timestamp. */
    val adjustmentInserted: Boolean
)

/**
 * One-shot wizard entry point: user types "the actual balance in my bank is
 * X" → we compute delta, insert ONE reconciliation transaction if the delta
 * is non-trivial, update the account balance to the user's new value, and
 * stamp `lastReconciledAt` so the Balance card can show "Reconciled 3 days ago".
 *
 * Why this is a NEW use case instead of extending [CreateReconciliationUseCase]:
 * the existing one is wired into the Add Transaction sheet (user picks the
 * "Account Reconciliation" category to trigger it). That flow is unintuitive
 * — users never find it. This use case backs the dedicated Reconcile button
 * on the Balance tab and takes a simple (accountId, newBalance) signature
 * instead of a pre-computed `ReconciliationDifference`.
 */
class ReconcileAccountFromBalanceUseCase(
    private val repository: CoreRepository
) {
    companion object {
        /** Below this threshold the delta is considered noise and no
         *  transaction is inserted — we only stamp the timestamp. */
        const val NOISE_THRESHOLD = 0.01

        /** Category IDs for the reconciliation adjustment transactions.
         *  Pre-existing in the default category set; the Reconcile wizard
         *  reuses them so the adjustment rows are findable in Analytics
         *  filtered by category (users can retroactively see "I lost 52 PLN
         *  in untracked spending last month"). */
        const val NEGATIVE_RECONCILIATION_CATEGORY_ID = "reconciliation"
        const val POSITIVE_RECONCILIATION_CATEGORY_ID = "positive_reconciliation"
    }

    suspend operator fun invoke(accountId: Int, newBalance: Double): ReconcileOutcome? {
        val account = repository.getAccountById(accountId) ?: return null
        val oldBalance = account.amount
        val delta = newBalance - oldBalance
        val now = Clock.System.now()
        val isGain = delta > 0
        val absDelta = abs(delta)

        val adjustmentInserted = if (absDelta >= NOISE_THRESHOLD) {
            val categoryId = if (isGain) {
                POSITIVE_RECONCILIATION_CATEGORY_ID
            } else {
                NEGATIVE_RECONCILIATION_CATEGORY_ID
            }
            val category = repository.getCategoryById(categoryId)
            if (category != null) {
                // Insert the adjustment transaction with the ORIGINAL account
                // (pre-update) referenced — this way AddTransactionUseCase
                // wouldn't double-apply the delta if we routed through it;
                // we use upsertTransaction directly to insert a bare row
                // and set the account balance manually below.
                val tx = Transaction(
                    id = 0,
                    subcategory = category,
                    amount = absDelta,
                    account = account,
                    date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date,
                    description = "Reconciliation adjustment" // allow-hardcoded: internal transaction description — reconcile adjustments are a fixed system-generated note, not a user-facing UI string
                )
                repository.upsertTransaction(tx)
                true
            } else {
                false
            }
        } else {
            false
        }

        // Overwrite the account balance to exactly the user-provided value
        // AND stamp the reconciliation timestamp in the same write.
        repository.upsertAccount(
            account.copy(
                amount = newBalance,
                lastReconciledAt = now
            )
        )

        return ReconcileOutcome(
            account = account,
            oldBalance = oldBalance,
            newBalance = newBalance,
            delta = delta,
            adjustmentInserted = adjustmentInserted
        )
    }
}
