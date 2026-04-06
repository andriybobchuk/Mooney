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
    val showPaywall: Boolean = false,
    val isPurchasing: Boolean = false,
    val purchaseError: String? = null,
    val isLoading: Boolean = true
)

sealed interface TransactionCategoriesAction {
    data class AddCategory(val title: String, val type: String, val emoji: String?, val parentId: String?) : TransactionCategoriesAction
    data class DeleteCategory(val categoryId: String) : TransactionCategoriesAction
    data object DismissPaywall : TransactionCategoriesAction
    data object Subscribe : TransactionCategoriesAction
    data object RestorePurchases : TransactionCategoriesAction
}

class TransactionCategoriesViewModel(
    private val categoryDao: CategoryDao,
    private val repository: CoreRepository,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val premiumManager: PremiumManager,
    private val dataStore: DataStore<Preferences>,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionCategoriesState())
    val state: StateFlow<TransactionCategoriesState> = _state

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val categories = getCategoriesUseCase()
                _state.update { it.copy(allCategories = categories, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onAction(action: TransactionCategoriesAction) {
        when (action) {
            is TransactionCategoriesAction.AddCategory -> addCategory(action.title, action.type, action.emoji, action.parentId)
            is TransactionCategoriesAction.DeleteCategory -> deleteCategory(action.categoryId)
            is TransactionCategoriesAction.DismissPaywall -> _state.update { it.copy(showPaywall = false, purchaseError = null) }
            is TransactionCategoriesAction.Subscribe -> onSubscribe()
            is TransactionCategoriesAction.RestorePurchases -> onRestorePurchases()
        }
    }

    private fun addCategory(title: String, type: String, emoji: String?, parentId: String?) {
        viewModelScope.launch {
            val isPremium = premiumManager.getIsPremium()
            if (!isPremium) {
                val currentCount = dataStore.data.first()[PreferencesKeys.CUSTOM_CATEGORY_COUNT] ?: 0
                if (currentCount >= PremiumConfig.maxFreeCustomCategories) {
                    _state.update { it.copy(showPaywall = true) }
                    return@launch
                }
            }

            val id = title.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")
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
            analyticsTracker.trackEvent(AnalyticsEvent.AddCustomCategory(type))
            repository.reloadCategories()
            _state.update { it.copy(allCategories = getCategoriesUseCase()) }
        }
    }

    private fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            val children = categoryDao.getByParentId(categoryId)
            children.forEach { categoryDao.delete(it.id) }
            categoryDao.delete(categoryId)
            val deletedCount = children.size + 1
            dataStore.edit { prefs ->
                val current = prefs[PreferencesKeys.CUSTOM_CATEGORY_COUNT] ?: 0
                prefs[PreferencesKeys.CUSTOM_CATEGORY_COUNT] = (current - deletedCount).coerceAtLeast(0)
            }
            analyticsTracker.trackEvent(AnalyticsEvent.DeleteCustomCategory)
            repository.reloadCategories()
            _state.update { it.copy(allCategories = getCategoriesUseCase()) }
        }
    }

    private fun onSubscribe() {
        viewModelScope.launch {
            _state.update { it.copy(isPurchasing = true, purchaseError = null) }
            try {
                when (val result = premiumManager.purchase(PRODUCT_ID_MONTHLY)) {
                    is PurchaseResult.Success -> _state.update { it.copy(showPaywall = false, isPurchasing = false) }
                    is PurchaseResult.Cancelled -> _state.update { it.copy(isPurchasing = false) }
                    is PurchaseResult.Error -> _state.update { it.copy(isPurchasing = false, purchaseError = result.message) }
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
