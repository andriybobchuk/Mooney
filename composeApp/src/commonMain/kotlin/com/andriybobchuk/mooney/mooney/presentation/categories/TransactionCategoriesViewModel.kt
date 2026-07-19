package com.andriybobchuk.mooney.mooney.presentation.categories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.core.data.database.CategoryDao
import com.andriybobchuk.mooney.core.data.database.CategoryEntity
import com.andriybobchuk.mooney.core.premium.PRODUCT_ID_MONTHLY
import com.andriybobchuk.mooney.core.premium.PremiumConfig
import com.andriybobchuk.mooney.core.premium.PremiumManager
import com.andriybobchuk.mooney.core.premium.PurchaseResult
import com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.usecase.GetCategoriesUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionCategoriesState(
    val allCategories: List<Category> = emptyList(),
    /**
     * User-picked top-level category order, mirrored from the same
     * DataStore key the Transaction picker reads. Empty when the user
     * hasn't reordered yet — screen falls back to natural cache order.
     */
    val categoryOrder: List<String> = emptyList(),
    /**
     * Flips to true after the first DataStore emission. The screen holds
     * the list off until then so users don't see a brief render-in-cache-
     * order → jump-to-saved-order flash.
     */
    val isOrderLoaded: Boolean = false,
    val showPaywall: Boolean = false,
    val isPurchasing: Boolean = false,
    val purchaseError: String? = null,
    /**
     * True until the AppDataCache emits; gates the shimmer / empty placeholder.
     */
    val isInitialLoading: Boolean = true
)

sealed interface TransactionCategoriesAction {
    data class AddCategory(val title: String, val type: String, val emoji: String?, val parentId: String?) : TransactionCategoriesAction
    data class DeleteCategory(val categoryId: String) : TransactionCategoriesAction
    /** Rename an existing category (seed or custom). Emoji/parent/type stay put. */
    data class RenameCategory(val categoryId: String, val newTitle: String) : TransactionCategoriesAction
    /** Set (or clear with null) the monthly budget on a category. */
    data class SetMonthlyLimit(val categoryId: String, val limit: Double?) : TransactionCategoriesAction
    data object DismissPaywall : TransactionCategoriesAction
    data object Subscribe : TransactionCategoriesAction
    data object RestorePurchases : TransactionCategoriesAction
}

@Suppress("LongParameterList")
class TransactionCategoriesViewModel(
    private val categoryDao: CategoryDao,
    private val repository: CoreRepository,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val premiumManager: PremiumManager,
    private val dataStore: DataStore<Preferences>,
    private val analyticsTracker: AnalyticsTracker,
    private val transactionDao: com.andriybobchuk.mooney.core.data.database.TransactionDao,
    private val categoryUsageDao: com.andriybobchuk.mooney.core.data.database.CategoryUsageDao,
    private val recurringTransactionDao: com.andriybobchuk.mooney.core.data.database.RecurringTransactionDao,
    private val pendingTransactionDao: com.andriybobchuk.mooney.core.data.database.PendingTransactionDao,
    private val appDataCache: com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache,
    // Shared with the Transaction picker so a reorder on either surface is
    // reflected on the other.
    private val manageOrderUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.ManageTransactionCategoryOrderUseCase
) : ViewModel() {

    // Always start with isInitialLoading=true so the first frame never
    // skips the shimmer just because the cache happened to warm up before
    // the VM was constructed. See TransactionViewModel for the full note.
    private val _state = MutableStateFlow(TransactionCategoriesState(isInitialLoading = true))
    val state: StateFlow<TransactionCategoriesState> = _state

    init {
        observeCategoriesFromCache()
        observeCategoryOrder()
    }

    private fun observeCategoryOrder() {
        viewModelScope.launch {
            try {
                manageOrderUseCase.getCategoryOrder().collect { order ->
                    _state.update { it.copy(categoryOrder = order, isOrderLoaded = true) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Even if reading pref fails, unblock the UI so users can
                // interact — worst case they see cache-natural order.
                _state.update { it.copy(isOrderLoaded = true) }
            }
        }
    }

    fun updateCategoryOrder(orderedIds: List<String>) {
        viewModelScope.launch {
            try {
                manageOrderUseCase.saveCategoryOrder(orderedIds)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                /* best-effort */
            }
        }
    }

    private fun observeCategoriesFromCache() {
        viewModelScope.launch {
            try {
                // Categories live in the app cache. Reading from it gives us
                // (a) cache-first paint on screen entry and (b) automatic
                // updates when categories are added/deleted/renamed anywhere
                // else in the app — no manual reload needed.
                appDataCache.snapshot.collect { snapshot ->
                    if (!snapshot.isReady) return@collect
                    _state.update {
                        it.copy(
                            allCategories = snapshot.categories,
                            isInitialLoading = false
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(isInitialLoading = false) }
            }
        }
    }

    fun onAction(action: TransactionCategoriesAction) {
        when (action) {
            is TransactionCategoriesAction.AddCategory -> addCategory(action.title, action.type, action.emoji, action.parentId)
            is TransactionCategoriesAction.DeleteCategory -> deleteCategory(action.categoryId)
            is TransactionCategoriesAction.RenameCategory -> renameCategory(action.categoryId, action.newTitle)
            is TransactionCategoriesAction.SetMonthlyLimit -> setMonthlyLimit(action.categoryId, action.limit)
            is TransactionCategoriesAction.DismissPaywall -> _state.update { it.copy(showPaywall = false, purchaseError = null) }
            is TransactionCategoriesAction.Subscribe -> onSubscribe()
            is TransactionCategoriesAction.RestorePurchases -> onRestorePurchases()
        }
    }

    private fun renameCategory(categoryId: String, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val existing = categoryDao.getById(categoryId) ?: return@launch
            if (existing.title == trimmed) return@launch
            categoryDao.upsert(existing.copy(title = trimmed))
            repository.reloadCategories()
            _state.update { it.copy(allCategories = getCategoriesUseCase()) }
        }
    }

    private fun setMonthlyLimit(categoryId: String, limit: Double?) {
        viewModelScope.launch {
            val existing = categoryDao.getById(categoryId) ?: return@launch
            if (existing.monthlyLimit == limit) return@launch
            categoryDao.upsert(existing.copy(monthlyLimit = limit))
            repository.reloadCategories()
            _state.update { it.copy(allCategories = getCategoriesUseCase()) }
        }
    }

    private fun addCategory(title: String, type: String, emoji: String?, parentId: String?) {
        viewModelScope.launch {
            val isPremium = premiumManager.getIsPremium()
            if (!isPremium) {
                val currentCount = dataStore.data.first()[PreferencesKeys.CUSTOM_CATEGORY_COUNT] ?: 0
                if (currentCount >= PremiumConfig.maxFreeCustomCategories) {
                    analyticsTracker.trackEvent(AnalyticsEvent.FeatureLimitHit("custom_categories"))
                    _state.update { it.copy(showPaywall = true) }
                    return@launch
                }
            }

            var id = title.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")
            val existingIds = categoryDao.getAllIds()
            if (id in existingIds) {
                var counter = 2
                while ("${id}_$counter" in existingIds) counter++
                id = "${id}_$counter"
            }
            val resolvedParentId = parentId ?: type.lowercase()
            categoryDao.upsert(
                CategoryEntity(
                    id = id,
                    title = title,
                    type = type,
                    emoji = emoji,
                    parentId = resolvedParentId
                )
            )
            dataStore.edit { prefs ->
                val current = prefs[PreferencesKeys.CUSTOM_CATEGORY_COUNT] ?: 0
                prefs[PreferencesKeys.CUSTOM_CATEGORY_COUNT] = current + 1
            }
            analyticsTracker.trackEvent(AnalyticsEvent.CustomCategoryAdded(type))
            repository.reloadCategories()
            _state.update { it.copy(allCategories = getCategoriesUseCase()) }
        }
    }

    companion object {
        private val ROOT_CATEGORY_IDS = setOf("expense", "income", "transfer")
    }

    private fun deleteCategory(categoryId: String) {
        if (categoryId in ROOT_CATEGORY_IDS) return

        viewModelScope.launch {
            val category = categoryDao.getById(categoryId) ?: return@launch
            val children = categoryDao.getByParentId(categoryId)

            // Determine reassignment target:
            // - Subcategory → reassign to its parent (the general category)
            // - General category → reassign to the root type (expense/income/transfer)
            val reassignTo = category.parentId ?: categoryId
            val allIdsToDelete = children.map { it.id } + categoryId

            // Reassign all transactions, recurring, and pending that reference any deleted category
            for (id in allIdsToDelete) {
                transactionDao.reassignCategory(id, reassignTo)
                recurringTransactionDao.reassignCategory(id, reassignTo)
                pendingTransactionDao.reassignCategory(id, reassignTo)
                categoryUsageDao.delete(id)
            }

            // Delete children first, then the category itself
            children.forEach { categoryDao.delete(it.id) }
            categoryDao.delete(categoryId)

            dataStore.edit { prefs ->
                val current = prefs[PreferencesKeys.CUSTOM_CATEGORY_COUNT] ?: 0
                prefs[PreferencesKeys.CUSTOM_CATEGORY_COUNT] = (current - allIdsToDelete.size).coerceAtLeast(0)
            }
            analyticsTracker.log("Custom category deleted: $categoryId")
            repository.reloadCategories()
            _state.update { it.copy(allCategories = getCategoriesUseCase()) }
        }
    }

    private fun onSubscribe() {
        viewModelScope.launch {
            _state.update { it.copy(isPurchasing = true, purchaseError = null) }
            try {
                val result = kotlinx.coroutines.withTimeoutOrNull(25_000L) {
                    premiumManager.purchase(PRODUCT_ID_MONTHLY)
                }
                when (result) {
                    is PurchaseResult.Success -> _state.update { it.copy(showPaywall = false, isPurchasing = false) }
                    is PurchaseResult.Cancelled -> _state.update { it.copy(isPurchasing = false) }
                    is PurchaseResult.Error -> _state.update { it.copy(isPurchasing = false, purchaseError = result.message) }
                    null -> _state.update {
                        it.copy(
                            isPurchasing = false,
                            purchaseError = "Purchase didn't respond in time. Please try again."
                        )
                    }
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isPurchasing = false, purchaseError = e.message) }
            }
        }
    }

    private fun onRestorePurchases() {
        viewModelScope.launch {
            _state.update { it.copy(isPurchasing = true, purchaseError = null) }
            try {
                val restored = premiumManager.restorePurchases()
                if (restored) {
                    _state.update { it.copy(showPaywall = false, isPurchasing = false) }
                } else {
                    _state.update { it.copy(isPurchasing = false, purchaseError = "No active subscription found") }
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isPurchasing = false, purchaseError = e.message) }
            }
        }
    }
}
