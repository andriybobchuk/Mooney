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
    val isLoading: Boolean = true
)

sealed interface AssetCategoriesAction {
    data class Add(val title: String, val isLiability: Boolean) : AssetCategoriesAction
    data class Delete(val categoryId: String) : AssetCategoriesAction
    data class Rename(val categoryId: String, val newTitle: String) : AssetCategoriesAction
}

class AssetCategoriesViewModel(
    private val assetCategoryDao: AssetCategoryDao
) : ViewModel() {

    private val _state = MutableStateFlow(AssetCategoriesState())
    val state: StateFlow<AssetCategoriesState> = _state

    init {
        observeCategories()
    }

    private fun observeCategories() {
        viewModelScope.launch {
            try {
                assetCategoryDao.getAll().collect { categories ->
                    _state.update { it.copy(assetCategories = categories, isLoading = false) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
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
