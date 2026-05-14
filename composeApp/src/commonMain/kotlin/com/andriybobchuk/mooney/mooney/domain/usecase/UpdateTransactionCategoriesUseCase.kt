package com.andriybobchuk.mooney.mooney.domain.usecase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.andriybobchuk.mooney.core.data.category.DefaultCategoryProvider
import com.andriybobchuk.mooney.core.data.category.DefaultTransactionCategory
import com.andriybobchuk.mooney.core.data.database.CategoryDao
import com.andriybobchuk.mooney.core.data.database.CategoryEntity
import com.andriybobchuk.mooney.core.data.database.CategoryUsageDao
import com.andriybobchuk.mooney.core.data.database.PendingTransactionDao
import com.andriybobchuk.mooney.core.data.database.RecurringTransactionDao
import com.andriybobchuk.mooney.core.data.database.TransactionDao
import com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

data class UpdateCategoriesResult(
    val added: Int,
    val updated: Int,
    val removed: Int,
    val remapped: Int
)

/**
 * Migrates user's existing categories to the latest schema (from RemoteCategoryProvider).
 *
 * Migration strategy:
 *   1. NEW IDs not in user's DB → inserted
 *   2. EXISTING IDs present in new schema → updated (title/emoji/parent refreshed)
 *   3. OLD IDs not in new schema → mapped via best-effort to a new ID, then references
 *      reassigned, then old category deleted. Root categories (expense/income/transfer)
 *      are never deleted.
 *
 * Best-effort mapping order for each old category id:
 *   a) Same id exists in new schema → no remap needed
 *   b) Title match within same type (case/whitespace-insensitive) → remap
 *   c) Old parent maps to a new id whose title matches → remap to the matched new general
 *   d) Old parent has a known mapping → remap to parent's mapped id (collapse subcategory)
 *   e) Fall back to root type id (expense/income/transfer)
 */
class UpdateTransactionCategoriesUseCase(
    private val categoryProvider: DefaultCategoryProvider,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val pendingTransactionDao: PendingTransactionDao,
    private val categoryUsageDao: CategoryUsageDao,
    private val coreRepository: CoreRepository,
    private val dataStore: DataStore<Preferences>
) {
    suspend operator fun invoke(): UpdateCategoriesResult {
        try {
            val newSchema = categoryProvider.getTransactionCategories()
            val newById = newSchema.categories.associateBy { it.id }
            val oldCategories = categoryDao.getAll().first()
            val oldById = oldCategories.associateBy { it.id }

            val rootIds = setOf("expense", "income", "transfer")

            // 1) Compute mapping for each old id that no longer exists or whose type changed
            val mapping = mutableMapOf<String, String>()
            for (oldCat in oldCategories) {
                if (oldCat.id in rootIds) continue
                val newCat = newById[oldCat.id]
                if (newCat != null && newCat.type == oldCat.type) continue // stays

                // Need to remap
                val target = findMappingTarget(oldCat, oldById, newById, mapping)
                    ?: fallbackRootId(oldCat.type)
                mapping[oldCat.id] = target
            }

            // 2) Reassign references from each old id to its target
            var remapped = 0
            for ((oldId, newId) in mapping) {
                if (oldId == newId) continue
                transactionDao.reassignCategory(oldId, newId)
                recurringTransactionDao.reassignCategory(oldId, newId)
                pendingTransactionDao.reassignCategory(oldId, newId)
                categoryUsageDao.delete(oldId)
                remapped++
            }

            // 3) Delete old categories that are gone in new schema (after remap)
            var removed = 0
            for (oldCat in oldCategories) {
                if (oldCat.id in rootIds) continue
                if (oldCat.id !in newById || newById[oldCat.id]?.type != oldCat.type) {
                    categoryDao.delete(oldCat.id)
                    removed++
                }
            }

            // 4) Update existing categories with new metadata + insert new ones
            var added = 0
            var updated = 0
            for (newCat in newSchema.categories) {
                val existing = categoryDao.getById(newCat.id)
                if (existing == null) {
                    categoryDao.upsert(newCat.toEntity())
                    added++
                } else if (
                    existing.title != newCat.title ||
                    existing.emoji != newCat.emoji ||
                    existing.parentId != newCat.parentId ||
                    existing.type != newCat.type
                ) {
                    categoryDao.upsert(newCat.toEntity())
                    updated++
                }
            }

            // 5) Persist the new defaults version
            dataStore.edit { it[PreferencesKeys.DEFAULTS_VERSION] = newSchema.version }

            // 6) Reload in-memory cache
            coreRepository.reloadCategories()

            return UpdateCategoriesResult(added, updated, removed, remapped)
        } catch (e: CancellationException) {
            throw e
        }
    }

    private fun findMappingTarget(
        oldCat: CategoryEntity,
        oldById: Map<String, CategoryEntity>,
        newById: Map<String, DefaultTransactionCategory>,
        currentMapping: Map<String, String>
    ): String? {
        // b) Title match within same type
        val oldTitleKey = normalize(oldCat.title)
        val titleMatch = newById.values.firstOrNull {
            it.type == oldCat.type && normalize(it.title) == oldTitleKey
        }
        if (titleMatch != null) return titleMatch.id

        // c) Try to find a new general category whose title matches the old parent's title
        val oldParent = oldCat.parentId?.let { oldById[it] }
        if (oldParent != null) {
            val parentTitleKey = normalize(oldParent.title)
            val parentTitleMatch = newById.values.firstOrNull {
                it.type == oldCat.type && normalize(it.title) == parentTitleKey
            }
            if (parentTitleMatch != null) return parentTitleMatch.id

            // d) If old parent has been mapped, use that
            val parentMapping = currentMapping[oldParent.id]
            if (parentMapping != null) return parentMapping
            // e) If old parent still exists in new schema, use it
            if (oldParent.id in newById) return oldParent.id
        }

        return null
    }

    private fun fallbackRootId(type: String): String = when (type.uppercase()) {
        "INCOME" -> "income"
        "TRANSFER" -> "transfer"
        else -> "expense"
    }

    private fun normalize(s: String): String =
        s.lowercase().filter { it.isLetterOrDigit() }

    private fun DefaultTransactionCategory.toEntity() = CategoryEntity(
        id = id,
        title = title,
        type = type,
        emoji = emoji,
        parentId = parentId
    )
}
