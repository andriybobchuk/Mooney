package com.andriybobchuk.mooney.mooney.presentation.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.core.data.database.PendingTransactionDao
import com.andriybobchuk.mooney.core.data.database.PendingTransactionEntity
import com.andriybobchuk.mooney.mooney.domain.usecase.*
import com.andriybobchuk.mooney.mooney.domain.usecase.settings.GetPinnedCategoriesUseCase
import com.andriybobchuk.mooney.mooney.domain.AccountWithConversion
import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.RecurringSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

data class TransactionState(
    val selectedMonth: MonthKey = MonthKey.current(),
    val transactions: List<Transaction?> = emptyList(),
    val accounts: List<AccountWithConversion?> = emptyList(),
    val categories: List<Category> = emptyList(),
    val total: Double = 0.0,
    val totalCurrency: Currency = GlobalConfig.baseCurrency,
    val dailyTotals: Map<Int, Double> = emptyMap(),
    val pendingTransactions: List<PendingTransactionEntity> = emptyList(),
    val pendingCount: Int = 0,
    val isLoading: Boolean = false,
    val isError: Boolean = false
)

@Suppress("LongParameterList")
class TransactionViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val calculateTransactionTotalUseCase: CalculateTransactionTotalUseCase,
    private val calculateDailyTotalUseCase: CalculateDailyTotalUseCase,
    private val convertAccountsToUiUseCase: ConvertAccountsToUiUseCase,
    private val currencyManagerUseCase: CurrencyManagerUseCase,
    private val getPinnedCategoriesUseCase: GetPinnedCategoriesUseCase,
    private val filterTransactionsByMonthUseCase: FilterTransactionsByMonthUseCase,
    private val calculateDailyTotalsMapUseCase: CalculateDailyTotalsMapUseCase,
    private val pendingTransactionDao: PendingTransactionDao,
    private val acceptPendingTransactionUseCase: AcceptPendingTransactionUseCase,
    private val createRecurringFromTransactionUseCase: CreateRecurringFromTransactionUseCase,
    private val analyticsTracker: AnalyticsTracker
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

    val frequentCategories = getPinnedCategoriesUseCase()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            emptyList()
        )

    fun onMonthSelected(month: MonthKey) {
        _uiState.update { it.copy(selectedMonth = month) }
        observeTransactions(month)
    }

    private fun observeTransactions(month: MonthKey) {
        observeTransactionsJob?.cancel()

        observeTransactionsJob = getTransactionsUseCase()
            .map { transactions -> filterTransactionsByMonthUseCase(transactions, month) }
            .onEach { filteredTransactions ->
                val sorted = filteredTransactions.sortedByDescending { it.date }
                _uiState.update { it.copy(transactions = sorted) }
                loadTotal()
                loadDailyTotals(filteredTransactions, month)
            }
            .launchIn(viewModelScope)
    }

    init {
        loadDataForBottomSheet()
        observePendingTransactions()
    }

    private fun observePendingTransactions() {
        pendingTransactionDao.getAllPending().onEach { pending ->
            _uiState.update { it.copy(pendingTransactions = pending, pendingCount = pending.size) }
        }.launchIn(viewModelScope)
    }

    private fun loadTotal() {
        val result = calculateTransactionTotalUseCase(
            transactions = _uiState.value.transactions,
            selectedCurrency = currencyManagerUseCase.getCurrentCurrency(),
            baseCurrency = GlobalConfig.baseCurrency
        )

        _uiState.update {
            it.copy(
                total = result.total,
                totalCurrency = result.currency
            )
        }
    }

    private fun loadDailyTotals(transactions: List<Transaction>, month: MonthKey) {
        val dailyTotalsMap = calculateDailyTotalsMapUseCase(transactions, month)
        _uiState.update { it.copy(dailyTotals = dailyTotalsMap) }
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                analyticsTracker.recordException(e, "Transactions")
                _uiState.update { it.copy(isError = true, isLoading = false) }
            }
        }
    }

    fun onTotalCurrencyClick() {
        currencyManagerUseCase.cycleToNextCurrency()
        analyticsTracker.trackEvent(
            AnalyticsEvent.CycleCurrencyDisplay(currencyManagerUseCase.getCurrentCurrency().name)
        )
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

    fun getDailyTotal(date: kotlinx.datetime.LocalDate): Double {
        val allTransactions = _uiState.value.transactions.filterNotNull()
        return calculateDailyTotalUseCase(allTransactions, date)
    }

    suspend fun getDailyTotalForMonth(date: kotlinx.datetime.LocalDate): Double {
        val allTransactions = getTransactionsUseCase().first().filterNotNull()
        return calculateDailyTotalUseCase(allTransactions, date)
    }

    fun acceptPendingTransaction(pending: PendingTransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val account = getAccountsUseCase(pending.accountId) ?: return@launch
                val category = getCategoriesUseCase(pending.subcategoryId) ?: return@launch
                acceptPendingTransactionUseCase(pending, account, category)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) { }
        }
    }

    fun skipPendingTransaction(pendingId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            pendingTransactionDao.updateStatus(pendingId, "SKIPPED")
        }
    }

    fun acceptAllPending() {
        viewModelScope.launch(Dispatchers.IO) {
            val pending = _uiState.value.pendingTransactions
            for (p in pending) {
                try {
                    val account = getAccountsUseCase(p.accountId)
                    val category = getCategoriesUseCase(p.subcategoryId)
                    if (account != null && category != null) {
                        acceptPendingTransactionUseCase(p, account, category)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
            }
        }
    }

    fun createRecurringFromTransaction(
        transaction: Transaction,
        schedule: RecurringSchedule
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            createRecurringFromTransactionUseCase(transaction, schedule)
        }
    }
}
