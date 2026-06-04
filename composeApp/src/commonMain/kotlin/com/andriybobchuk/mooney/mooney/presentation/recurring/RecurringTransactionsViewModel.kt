package com.andriybobchuk.mooney.mooney.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.domain.AccountWithConversion
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.RecurringSchedule
import com.andriybobchuk.mooney.mooney.domain.RecurringTransaction
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.usecase.ConvertAccountsToUiUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.CreateRecurringFromTransactionUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.DeleteRecurringTransactionUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetAccountsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetCategoriesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetRecurringTransactionsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.SaveRecurringTransactionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class RecurringTransactionsState(
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val accounts: List<AccountWithConversion?> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    /**
     * True until the AppDataCache has emitted its first snapshot. Gates both
     * the shimmer skeleton and the empty-state placeholder so neither flashes
     * during the brief window between VM construction and first emission.
     */
    val isInitialLoading: Boolean = true,
    // Mirror of the Transactions screen state so the embedded
    // TransactionBottomSheet renders its account picker with display titles
    // and a working expand/collapse toggle. Without these the picker fell
    // back to showing raw `assetCategoryId` enum values like "CHECKING".
    val assetCategories: List<com.andriybobchuk.mooney.core.data.database.AssetCategoryEntity> = emptyList(),
    val categoryOrder: List<String> = emptyList(),
    val expandedCategories: Set<String> = emptySet()
)

class RecurringTransactionsViewModel(
    private val saveRecurringTransactionUseCase: SaveRecurringTransactionUseCase,
    private val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase,
    private val createRecurringFromTransactionUseCase: CreateRecurringFromTransactionUseCase,
    private val convertAccountsToUiUseCase: ConvertAccountsToUiUseCase,
    private val manageAssetCategoryOrderUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.assets.ManageAssetCategoryOrderUseCase,
    private val manageCategoryExpansionUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.assets.ManageCategoryExpansionUseCase,
    private val appDataCache: com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache
) : ViewModel() {

    // Seed initial state from the cache; subsequent updates flow through
    // observeFromCache().
    private val _uiState = MutableStateFlow(
        RecurringTransactionsState(isInitialLoading = !appDataCache.snapshot.value.isReady)
    )
    val state = _uiState
        .onStart { observeFromCache() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _uiState.value
        )

    private fun observeFromCache() {
        // Everything we need lives in the app cache except the category-order
        // and expansion preferences (which are persisted in DataStore, not Room),
        // so wire those separately.
        appDataCache.snapshot.onEach { snapshot ->
            if (!snapshot.isReady) return@onEach
            _uiState.update {
                it.copy(
                    recurringTransactions = snapshot.recurringTransactions,
                    accounts = convertAccountsToUiUseCase(snapshot.accounts),
                    categories = snapshot.categories,
                    assetCategories = snapshot.assetCategories,
                    isLoading = false,
                    isInitialLoading = false
                )
            }
        }.launchIn(viewModelScope)

        manageAssetCategoryOrderUseCase.getCategoryOrder().onEach { order ->
            _uiState.update { it.copy(categoryOrder = order) }
        }.launchIn(viewModelScope)

        manageCategoryExpansionUseCase.getExpandedCategories().onEach { expanded ->
            _uiState.update { it.copy(expandedCategories = expanded) }
        }.launchIn(viewModelScope)
    }

    fun toggleAccountCategoryExpansion(categoryId: String) {
        viewModelScope.launch {
            manageCategoryExpansionUseCase.toggleCategoryExpansion(
                category = categoryId,
                currentExpanded = _uiState.value.expandedCategories
            )
        }
    }

    fun deleteRecurring(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteRecurringTransactionUseCase(id)
        }
    }

    fun saveRecurring(recurring: RecurringTransaction) {
        viewModelScope.launch(Dispatchers.IO) {
            saveRecurringTransactionUseCase(recurring)
        }
    }

    fun addWithRecurring(transaction: Transaction, schedule: RecurringSchedule) {
        viewModelScope.launch(Dispatchers.IO) {
            createRecurringFromTransactionUseCase(transaction, schedule)
        }
    }
}
