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
    val isLoading: Boolean = false
)

class RecurringTransactionsViewModel(
    private val getRecurringTransactionsUseCase: GetRecurringTransactionsUseCase,
    private val saveRecurringTransactionUseCase: SaveRecurringTransactionUseCase,
    private val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase,
    private val createRecurringFromTransactionUseCase: CreateRecurringFromTransactionUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val convertAccountsToUiUseCase: ConvertAccountsToUiUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringTransactionsState())
    val state = _uiState
        .onStart { loadData() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _uiState.value
        )

    private fun loadData() {
        getRecurringTransactionsUseCase().onEach { list ->
            _uiState.update { it.copy(recurringTransactions = list) }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            try {
                getAccountsUseCase().collect { accounts ->
                    val categories = getCategoriesUseCase()
                    _uiState.update {
                        it.copy(
                            accounts = convertAccountsToUiUseCase(accounts),
                            categories = categories,
                            isLoading = false
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
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
