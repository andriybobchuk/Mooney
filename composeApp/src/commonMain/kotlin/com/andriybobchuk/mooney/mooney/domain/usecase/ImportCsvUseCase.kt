package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.core.data.database.AccountDao
import com.andriybobchuk.mooney.core.data.database.AccountEntity
import com.andriybobchuk.mooney.core.data.database.TransactionDao
import com.andriybobchuk.mooney.core.data.database.TransactionEntity
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.backup.UniversalCsvImporter
import kotlinx.coroutines.flow.first

/**
 * Takes the [UniversalCsvImporter.ParsedCsv] preview the user confirmed and
 * actually writes the transactions to the database. Side effects:
 *
 *  - Creates a "CSV import" account if none of the user's existing accounts
 *    match the per-row account hint. Single bucket beats spamming the user
 *    with five accounts they didn't ask for.
 *  - Defaults every transaction to the existing "Other expense" subcategory
 *    when [defaultExpenseSubcategoryId] is provided. Category text from the
 *    CSV becomes the transaction's description prefix so the data isn't lost.
 *  - Negative amounts in the CSV become "EXPENSE" type (we flip the sign so
 *    Mooney stores absolute values), positives stay as income.
 *
 * Designed for one-shot user-initiated imports; we don't try to dedupe
 * against existing transactions — the user can clear/clean up afterwards if
 * they need to.
 */
class ImportCsvUseCase(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val getAccountsUseCase: GetAccountsUseCase
) {
    data class Summary(
        val transactionsImported: Int,
        val accountCreated: Boolean,
        val firstFailureMessage: String?
    )

    suspend operator fun invoke(
        transactions: List<UniversalCsvImporter.ParsedTransaction>,
        defaultExpenseSubcategoryId: String,
        defaultIncomeSubcategoryId: String
    ): Summary {
        if (transactions.isEmpty()) return Summary(0, false, "Empty list — nothing to import")

        val existingAccounts = getAccountsUseCase().first().filterNotNull()
        val targetAccountId: Int = if (existingAccounts.isEmpty()) {
            // Create an "Imported" account, then re-read accounts to find its
            // auto-generated id. Room's @Upsert returns Unit so we round-trip.
            accountDao.upsert(
                AccountEntity(
                    title = "Imported",
                    amount = 0.0,
                    currency = GlobalConfig.baseCurrency.name,
                    emoji = "📥",
                    assetCategory = "BANK_ACCOUNT",
                    isPrimary = true,
                    isLiability = false
                )
            )
            val refreshed = getAccountsUseCase().first().filterNotNull()
            refreshed.firstOrNull()?.id
                ?: return Summary(0, false, "Failed to create import account")
        } else existingAccounts.first().id

        var success = 0
        var firstFailure: String? = null
        for (parsed in transactions) {
            try {
                val isExpense = parsed.amount < 0
                val subcategory = if (isExpense) defaultExpenseSubcategoryId else defaultIncomeSubcategoryId
                transactionDao.upsert(
                    TransactionEntity(
                        subcategoryId = subcategory,
                        amount = kotlin.math.abs(parsed.amount),
                        accountId = targetAccountId,
                        date = parsed.date.toString()
                    )
                )
                success += 1
            } catch (e: Throwable) {
                if (firstFailure == null) firstFailure = e.message ?: e::class.simpleName
            }
        }
        return Summary(
            transactionsImported = success,
            accountCreated = existingAccounts.isEmpty(),
            firstFailureMessage = firstFailure
        )
    }
}
