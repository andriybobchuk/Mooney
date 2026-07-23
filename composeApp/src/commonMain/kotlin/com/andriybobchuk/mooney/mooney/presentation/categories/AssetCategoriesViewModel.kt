package com.andriybobchuk.mooney.mooney.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.core.data.database.AssetCategoryDao
import com.andriybobchuk.mooney.core.data.database.AssetCategoryEntity
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssetCategoriesState(
    val assetCategories: List<AssetCategoryEntity> = emptyList(),
    /**
     * `true` until the AppDataCache has emitted; gates the shimmer and prevents
     * an empty-state flash before the first emission lands.
     */
    val isInitialLoading: Boolean = true
)

sealed interface AssetCategoriesAction {
    data class Add(val title: String, val isLiability: Boolean) : AssetCategoriesAction
    data class Delete(val categoryId: String) : AssetCategoriesAction
    data class Rename(val categoryId: String, val newTitle: String) : AssetCategoriesAction
}

class AssetCategoriesViewModel(
    private val assetCategoryDao: AssetCategoryDao,
    private val appDataCache: com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache
) : ViewModel() {

    // Always start with isInitialLoading=true — see TransactionViewModel.
    private val _state = MutableStateFlow(AssetCategoriesState(isInitialLoading = true))
    val state: StateFlow<AssetCategoriesState> = _state

    init {
        observeCategoriesFromCache()
    }

    private fun observeCategoriesFromCache() {
        viewModelScope.launch {
            try {
                // Asset categories live in the app cache snapshot; reading from
                // it means screen-entry paints the previous list on the first
                // frame instead of running a fresh DAO query.
                appDataCache.snapshot.collect { snapshot ->
                    if (!snapshot.isReady) return@collect
                    _state.update {
                        it.copy(
                            assetCategories = snapshot.assetCategories,
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

    fun onAction(action: AssetCategoriesAction) {
        when (action) {
            is AssetCategoriesAction.Add -> addCategory(action.title, action.isLiability)
            is AssetCategoriesAction.Delete -> deleteCategory(action.categoryId)
            is AssetCategoriesAction.Rename -> renameCategory(action.categoryId, action.newTitle)
        }
    }

    private fun addCategory(title: String, isLiability: Boolean) {
        viewModelScope.launch {
            val id = title.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")
            val currentCount = assetCategoryDao.getCount()
            assetCategoryDao.upsert(
                AssetCategoryEntity(
                    id = id,
                    title = title,
                    emoji = "",
                    sortOrder = currentCount,
                    isLiability = isLiability
                )
            )
        }
    }

    private fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            assetCategoryDao.delete(categoryId)
        }
    }

    private fun renameCategory(categoryId: String, newTitle: String) {
        viewModelScope.launch {
            val existing = assetCategoryDao.getById(categoryId) ?: return@launch
            assetCategoryDao.upsert(existing.copy(title = newTitle))
        }
    }
}
