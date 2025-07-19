package com.andriybobchuk.mooney.mooney.presentation.transaction


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.usecase.*
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateTransactionTotalUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.ConvertAccountsToUiUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetCategoriesUseCase
import com.andriybobchuk.mooney.mooney.presentation.account.UiAccount

import com.andriybobchuk.mooney.mooney.presentation.analytics.MonthKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class TransactionState(
    val selectedMonth: MonthKey = MonthKey.current(),
    val transactions: List<Transaction?> = emptyList(),
    val accounts: List<UiAccount?> = emptyList(),
    val categories: List<Category> = emptyList(),
    val total: Double = 0.0,
    val totalCurrency: Currency = GlobalConfig.baseCurrency,
    val isLoading: Boolean = false,
    val isError: Boolean = false
)

class TransactionViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val calculateTransactionTotalUseCase: CalculateTransactionTotalUseCase,
    private val convertAccountsToUiUseCase: ConvertAccountsToUiUseCase
) : ViewModel() {

    private var observeTransactionsJob: Job? = null

    private val _uiState = MutableStateFlow(TransactionState())
    val state = _uiState
        .onStart {
            observeTransactions(_uiState.value.selectedMonth)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _uiState.value
        )

    fun onMonthSelected(month: MonthKey) {
        _uiState.update { it.copy(selectedMonth = month) }
        observeTransactions(month)
    }

    private fun observeTransactions(month: MonthKey) {
        observeTransactionsJob?.cancel()

        val start = month.firstDay()
        val end = month.firstDayOfNextMonth()


        observeTransactionsJob = getTransactionsUseCase()
            .map { transactions ->
                transactions.filterNotNull().filter {
                    it.date >= start && it.date < end
                }
            }
            .onEach { filteredTransactions ->
                val sorted = filteredTransactions.sortedByDescending { it.date }
                _uiState.update { it.copy(transactions = sorted) }
                loadTotal()
            }
            .launchIn(viewModelScope)
    }


    private val availableCurrencies = GlobalConfig.testExchangeRates.rates.keys.toList()
    private var selectedCurrencyIndex = 0
    private var selectedCurrency = GlobalConfig.baseCurrency

    init {
        loadDataForBottomSheet()
        //loadTotal()
    }

    private fun loadTotal() {
        val result = calculateTransactionTotalUseCase(
            transactions = _uiState.value.transactions,
            selectedCurrency = selectedCurrency,
            baseCurrency = GlobalConfig.baseCurrency
        )

        _uiState.update {
            it.copy(
                total = result.total,
                totalCurrency = result.currency
            )
        }
    }

    private fun loadDataForBottomSheet() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
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
            } catch (e: Exception) {
                _uiState.update { it.copy(isError = true, isLoading = false) }
            }
        }
    }

    fun onTotalCurrencyClick() {
        selectedCurrencyIndex = (selectedCurrencyIndex + 1) % availableCurrencies.size
        selectedCurrency = availableCurrencies[selectedCurrencyIndex]
        loadTotal()
    }

    fun upsertTransaction(transaction: Transaction) {
        viewModelScope.launch {
            addTransactionUseCase(transaction)
            observeTransactions(_uiState.value.selectedMonth)
            loadTotal()
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            deleteTransactionUseCase(id)
            observeTransactions(_uiState.value.selectedMonth)
            loadTotal()
        }
    }
}
