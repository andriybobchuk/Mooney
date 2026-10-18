package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.core.data.database.MerchantCategoryHistoryDao
import com.andriybobchuk.mooney.core.data.database.MerchantCategoryHistoryEntity
import com.andriybobchuk.mooney.core.data.database.TransactionDao
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Normalise a merchant string to a stable lookup key. Lowercase + trim +
 * collapse whitespace so "Biedronka ", "biedronka", and "BIEDRONKA  "
 * all map to the same history row.
 */
internal fun normalizeMerchantKey(raw: String?): String? {
    val trimmed = raw?.trim()?.replace(Regex("\\s+"), " ")?.lowercase()
    return trimmed?.takeIf { it.isNotEmpty() }
}

/**
 * Returns transactions where [Transaction.addedByAutomation] is true,
 * most-recent first. Drives both the dismissible banner's count badge
 * and the Upkeep review flow's card queue.
 */
class GetAutoAddedPendingUseCase(
    private val repository: CoreRepository
) {
    operator fun invoke(): Flow<List<Transaction>> =
        repository.getAllTransactions().map { list ->
            list.asSequence()
                .filterNotNull()
                .filter { it.addedByAutomation }
                .sortedByDescending { it.date }
                .toList()
        }
}

/**
 * Observable count of pending auto-added transactions. Combined with the
 * list flow intentionally — a single `collectAsState` on the Transactions
 * screen covers both the "show banner?" and "N" rendering decisions.
 */
class GetAutoAddedCountFlowUseCase(
    private val getAutoAddedPendingUseCase: GetAutoAddedPendingUseCase
) {
    operator fun invoke(): Flow<Int> =
        getAutoAddedPendingUseCase().map { it.size }
}

/**
 * Review action: commit the user's choice for a card.
 * - Flips [TransactionEntity.addedByAutomation] to false (tag disappears,
 *   tx leaves the queue).
 * - If the picked category differs from the current one, swap it.
 * - If the tx has a non-blank description (= merchant from Wallet), upsert
 *   the merchant → category mapping so future auto-adds for the same
 *   merchant land in the right place by default.
 */
class ReviewAutoAddedUseCase(
    private val repository: CoreRepository,
    private val merchantHistoryDao: MerchantCategoryHistoryDao,
    private val getCategoriesUseCase: GetCategoriesUseCase
) {
    suspend operator fun invoke(
        transaction: Transaction,
        newCategoryId: String
    ) {
        val newCategory = getCategoriesUseCase(newCategoryId) ?: transaction.subcategory
        val updated = transaction.copy(
            subcategory = newCategory,
            addedByAutomation = false
        )
        repository.upsertTransaction(updated)

        normalizeMerchantKey(transaction.description)?.let { key ->
            merchantHistoryDao.upsert(
                MerchantCategoryHistoryEntity(
                    merchantKey = key,
                    categoryId = newCategory.id,
                    updatedAt = Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .toString()
                )
            )
        }
    }
}

/**
 * Suggest the best category for a merchant based on prior reviews. Returns
 * null if we have no history — the intent handler then falls back to the
 * user's default expense category.
 */
class GetSmartCategoryForMerchantUseCase(
    private val merchantHistoryDao: MerchantCategoryHistoryDao,
    private val getCategoriesUseCase: GetCategoriesUseCase
) {
    suspend operator fun invoke(merchant: String?): String? {
        val key = normalizeMerchantKey(merchant) ?: return null
        val entry = merchantHistoryDao.getByMerchantKey(key) ?: return null
        // Guard against orphaned categories — if the user deleted the
        // learned category, the suggestion is useless; let the caller
        // fall back to default rather than inserting with a broken id.
        return if (getCategoriesUseCase(entry.categoryId) != null) {
            entry.categoryId
        } else null
    }
}

/**
 * Returns the top N most-recently-used EXPENSE subcategories for the Upkeep
 * review card's chip grid. We don't want the user staring at a wall of 100
 * categories; this surfaces the handful they actually touch day-to-day.
 */
class GetFrequentExpenseCategoryIdsUseCase(
    private val getAutoAddedPendingUseCase: GetAutoAddedPendingUseCase,
    private val repository: CoreRepository
) {
    operator fun invoke(limit: Int = 8): Flow<List<String>> {
        return combine(
            repository.getAllTransactions(),
            getAutoAddedPendingUseCase()
        ) { allTxs, _ ->
            allTxs.asSequence()
                .filterNotNull()
                .filter { it.subcategory.type == com.andriybobchuk.mooney.mooney.domain.CategoryType.EXPENSE }
                // Exclude auto-added-but-not-yet-reviewed txs — they skew
                // "frequent" toward whatever default the auto-adder chose,
                // which is exactly the signal we DON'T want to amplify.
                .filter { !it.addedByAutomation }
                .groupingBy { it.subcategory.id }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key }
        }
    }
}
