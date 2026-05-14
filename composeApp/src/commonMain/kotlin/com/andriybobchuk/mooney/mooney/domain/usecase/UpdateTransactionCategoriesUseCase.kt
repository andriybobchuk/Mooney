package com.andriybobchuk.mooney.mooney.domain.usecase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.andriybobchuk.mooney.core.data.category.DefaultCategoryProvider
import com.andriybobchuk.mooney.core.data.category.DefaultTransactionCategory
import com.andriybobchuk.mooney.core.data.database.CategoryDao
import com.andriybobchuk.mooney.core.data.database.CategoryEntity
import com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys
import com.andriybobchuk.mooney.mooney.domain.CoreRepository

data class UpdateCategoriesResult(
    val added: Int,
    val updated: Int
)

/**
 * SAFE additive-only category update.
 *
 * What this does:
 *   - ADDS new categories from the latest schema that aren't already in the DB.
 *   - UPDATES title/emoji/parent on categories where the ID already exists and the
 *     schema has fresher metadata.
 *
 * What this does NOT do:
 *   - Never deletes any category — including obsolete built-in ones or user-created ones.
 *   - Never reassigns any transaction, recurring, or pending category references.
 *   - Never touches transactions, accounts, goals, or balances.
 *
 * Why: distinguishing "obsolete built-in" from "user-created custom" categories without
 * an explicit flag is unreliable. Deleting custom categories could erase the user's
 * categorization work. Additive-only guarantees zero data loss; the cost is some legacy
 * built-in category rows remain in the user's list. The user can clean those up manually
 * via the Categories screen (which DOES safely reassign transactions on delete).
 */
class UpdateTransactionCategoriesUseCase(
    private val categoryProvider: DefaultCategoryProvider,
    private val categoryDao: CategoryDao,
    private val coreRepository: CoreRepository,
    private val dataStore: DataStore<Preferences>
) {
    suspend operator fun invoke(): UpdateCategoriesResult {
        val newSchema = categoryProvider.getTransactionCategories()

        var added = 0
        var updated = 0
        for (newCat in newSchema.categories) {
            val existing = categoryDao.getById(newCat.id)
            if (existing == null) {
                categoryDao.upsert(newCat.toEntity())
                added++
            } else if (existing.hasOutdatedMetadata(newCat)) {
                categoryDao.upsert(newCat.toEntity())
                updated++
            }
        }

        dataStore.edit { it[PreferencesKeys.DEFAULTS_VERSION] = newSchema.version }
        coreRepository.reloadCategories()

        return UpdateCategoriesResult(added, updated)
    }

    private fun CategoryEntity.hasOutdatedMetadata(newCat: DefaultTransactionCategory): Boolean {
        return title != newCat.title ||
            emoji != newCat.emoji ||
            parentId != newCat.parentId ||
            type != newCat.type
    }

    private fun DefaultTransactionCategory.toEntity() = CategoryEntity(
        id = id,
        title = title,
        type = type,
        emoji = emoji,
        parentId = parentId
    )
}
