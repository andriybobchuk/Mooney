package com.andriybobchuk.mooney.mooney.domain.usecase.screenshot

import com.andriybobchuk.mooney.core.platform.ScreenshotOcrParser
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.usecase.AddTransactionUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetSmartCategoryForMerchantUseCase
import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong

/**
 * Entry point for the screenshot bulk-import flow. Called by the ViewModel
 * when the user taps the 📸 chip on Transactions:
 *   1. Presents the native image picker + OCR via [ScreenshotOcrParser].
 *   2. Parses each image's text into [ParsedCandidate]s.
 *   3. Builds signatures of the user's recent transactions (last 7 days
 *      on the primary account) and runs the three-pass dedup.
 *   4. Returns the full annotated list — including dups and parse-fails,
 *      because the preview UI shows every row so the user can override.
 *
 * No side effects on the DB until [CommitScreenshotImportUseCase] runs.
 */
class RunScreenshotImportUseCase(
    private val parser: ScreenshotOcrParser,
    private val parseScreenshotUseCase: ParseScreenshotUseCase,
    private val dedupScreenshotCandidatesUseCase: DedupScreenshotCandidatesUseCase,
    private val repository: CoreRepository
) {

    suspend operator fun invoke(): List<ParsedCandidate> {
        val ocrResults = parser.pickAndExtractText()
        if (ocrResults.isEmpty()) return emptyList()

        val candidates = ocrResults.flatMap { parseScreenshotUseCase(it) }

        val existingSignatures = buildExistingSignatures()
        return dedupScreenshotCandidatesUseCase(candidates, existingSignatures)
    }

    /**
     * Signatures of the user's last 7 days of transactions on any account.
     * Pass A of the dedup compares each candidate against this set.
     *
     * We use any-account rather than primary-only because Wallet events
     * on a non-primary card still land in the DB on whichever account the
     * Shortcut selected — a notification OCR of the same payment should
     * still be flagged as a duplicate.
     */
    private suspend fun buildExistingSignatures(): Set<TxSignature> {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        val cutoffDaysAgo = 7
        val cutoffEpochDays = today.toEpochDays() - cutoffDaysAgo

        val recentTxs = repository.getAllTransactions().first()
            .filterNotNull()
            .filter { it.date.toEpochDays() >= cutoffEpochDays }

        return recentTxs.asSequence()
            .map { tx ->
                TxSignature(
                    amountCents = (tx.amount * 100).roundToLong(),
                    bucketKey = tx.date.hashCode().toLong()
                )
            }
            .toSet()
    }
}

/**
 * Commits the user-accepted [ParsedCandidate]s to the DB as real
 * transactions. Called from the preview screen's "Add N transactions"
 * primary button.
 *
 * Each inserted row:
 * - Uses the user's primary-for-expenses account (fallback: first account).
 * - Resolves category via the smart merchant→category history populated by
 *   Phase 1 Upkeep reviews, falling back to the user's default expense
 *   category. Reusing the Phase 1 learning is intentional: a notification
 *   OCR for "Biedronka 42,50 zł" should land under Groceries if the user
 *   already taught Mooney that mapping from a Wallet-created tx.
 * - Sets [Transaction.addedByAutomation] = true, so the row ships straight
 *   into the Upkeep queue and the user can bulk-correct categories via the
 *   same card-at-a-time flow (reusing Phase 1's UI).
 *
 * @return count of successfully inserted transactions. Zero for an empty
 *         accepted list or if the user has no accounts yet.
 */
class CommitScreenshotImportUseCase(
    private val repository: CoreRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val getSmartCategoryForMerchantUseCase: GetSmartCategoryForMerchantUseCase,
    private val preferencesRepository: PreferencesRepository
) {

    suspend operator fun invoke(accepted: List<ParsedCandidate>): Int {
        if (accepted.isEmpty()) return 0

        val account = repository.getAllAccounts().first()
            .filterNotNull()
            .let { accounts ->
                accounts.firstOrNull { it.isPrimaryForExpenses }
                    ?: accounts.firstOrNull { it.isPrimary }
                    ?: accounts.firstOrNull()
            } ?: return 0

        val prefs = preferencesRepository.getCurrentPreferences()
        var inserted = 0
        accepted.forEach { candidate ->
            if (candidate.status != ParsedCandidate.Status.NEW &&
                candidate.status != ParsedCandidate.Status.DUP_EXISTING &&
                candidate.status != ParsedCandidate.Status.DUP_BATCH) {
                // PARSE_FAILED candidates should never reach this call —
                // the UI gates the "Add" button on at least one NEW/dup
                // being toggled on — but belt-and-braces: skip.
                return@forEach
            }

            val smartCategoryId = getSmartCategoryForMerchantUseCase(candidate.description)
            val categoryId = smartCategoryId ?: prefs.defaultExpenseCategory
            val category = repository.getCategoryById(categoryId) ?: return@forEach
            if (category.type != CategoryType.EXPENSE) return@forEach

            val tx = Transaction(
                id = 0,
                subcategory = category,
                amount = candidate.amount,
                account = account,
                date = candidate.date,
                description = candidate.description
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.replaceFirstChar { it.uppercase() },
                addedByAutomation = true
            )
            addTransactionUseCase(tx)
            inserted++
        }
        return inserted
    }
}
