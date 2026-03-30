package com.andriybobchuk.mooney.mooney.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.AccountWithConversion
import com.andriybobchuk.mooney.mooney.domain.AssetCategory
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.mooney.domain.usecase.*
import com.andriybobchuk.mooney.core.domain.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.coroutines.cancellation.CancellationException

class AccountViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val addAccountUseCase: AddAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val calculateNetWorthUseCase: CalculateNetWorthUseCase,
    private val convertAccountsToUiUseCase: ConvertAccountsToUiUseCase,
    private val currencyManagerUseCase: CurrencyManagerUseCase,
    private val createReconciliationUseCase: CreateReconciliationUseCase,
    private val reconcileAccountUseCase: ReconcileAccountUseCase,
    private val shouldRefreshExchangeRatesUseCase: ShouldRefreshExchangeRatesUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private var observeAccountsJob: Job? = null

    private val _uiState = MutableStateFlow(AccountState())

    val state = _uiState
        .onStart {
            observeAccounts()
            refreshExchangeRatesOnStart()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _uiState.value
        )

    private fun refreshExchangeRatesOnStart() {
        viewModelScope.launch {
            if (shouldRefreshExchangeRatesUseCase(_uiState.value.ratesLastUpdated)) {
                refreshExchangeRates()
            }
        }
    }

    private fun observeAccounts() {
        observeAccountsJob?.cancel()
        observeAccountsJob = getAccountsUseCase()
            .onEach { accounts ->
                _uiState.update {
                    it.copy(
                        accounts = convertAccountsToUiUseCase(accounts)
                    )
                }
                loadAccountsAndWorth()
            }
            .launchIn(viewModelScope)
    }

    private suspend fun loadAccountsAndWorth() = withContext(Dispatchers.IO) {
        _uiState.update { it.copy(isLoading = true) }

        try {
            getAccountsUseCase().collect { accounts ->
                _uiState.update {
                    it.copy(
                        accounts = convertAccountsToUiUseCase(accounts),
                        isLoading = false
                    )
                }
                updateTotalNetWorth()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            analyticsTracker.recordException(e, "Account")
            _uiState.update { it.copy(isError = true, isLoading = false) }
        }
    }

    private fun updateTotalNetWorth() {
        val result = calculateNetWorthUseCase(
            accounts = state.value.accounts.filterNotNull().map { it.toAccount() },
            selectedCurrency = currencyManagerUseCase.getCurrentCurrency(),
            baseCurrency = GlobalConfig.baseCurrency
        )

        _uiState.update {
            it.copy(
                totalNetWorth = result.totalNetWorth,
                totalNetWorthCurrency = result.currency
            )
        }
    }

    fun onNetWorthLabelClick() {
        currencyManagerUseCase.cycleToNextCurrency()
        updateTotalNetWorth()
    }

    fun refreshExchangeRates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingRates = true) }

            when (val result = currencyManagerUseCase.refreshExchangeRates()) {
                is Result.Success -> {
                    val currentAccounts = getAccountsUseCase().first()
                    _uiState.update {
                        it.copy(
                            accounts = convertAccountsToUiUseCase(currentAccounts),
                            isRefreshingRates = false,
                            ratesLastUpdated = Clock.System.now().toEpochMilliseconds()
                        )
                    }
                    updateTotalNetWorth()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isRefreshingRates = false,
                            ratesError = "Failed to update exchange rates"
                        )
                    }
                }
            }
        }
    }

    fun upsertAccount(
        id: Int,
        title: String,
        emoji: String,
        amount: Double,
        currency: Currency
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val reconciliationDiff = createReconciliationUseCase.detectReconciliationDifference(id, amount)

            if (reconciliationDiff?.shouldShowDialog == true) {
                _uiState.update {
                    it.copy(
                        reconciliationDifference = reconciliationDiff,
                        pendingAccountUpdate = PendingAccountUpdate(id, title, emoji, amount, currency)
                    )
                }
            } else {
                addAccountUseCase(Account(id, title, amount, currency, emoji, AssetCategory.BANK_ACCOUNT))
                observeAccounts()
                _uiState.update { it.copy(shouldCloseSheet = true) }
            }
        }
    }

    fun confirmReconciliation() {
        viewModelScope.launch(Dispatchers.IO) {
            val reconciliationDiff = _uiState.value.reconciliationDifference
            val pendingUpdate = _uiState.value.pendingAccountUpdate

            if (reconciliationDiff != null && pendingUpdate != null) {
                try {
                    val targetAccount = Account(
                        pendingUpdate.id, pendingUpdate.title, pendingUpdate.amount,
                        pendingUpdate.currency, pendingUpdate.emoji, AssetCategory.BANK_ACCOUNT
                    )
                    reconcileAccountUseCase(reconciliationDiff, targetAccount)
                    observeAccounts()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    analyticsTracker.recordException(e, "Account")
                }
            }

            clearReconciliationState()
        }
    }

    fun dismissReconciliation() {
        val pendingUpdate = _uiState.value.pendingAccountUpdate
        clearReconciliationState()

        if (pendingUpdate != null) {
            viewModelScope.launch(Dispatchers.IO) {
                addAccountUseCase(
                    Account(pendingUpdate.id, pendingUpdate.title, pendingUpdate.amount,
                        pendingUpdate.currency, pendingUpdate.emoji, AssetCategory.BANK_ACCOUNT)
                )
                observeAccounts()
            }
        }
    }

    private fun clearReconciliationState() {
        _uiState.update {
            it.copy(
                reconciliationDifference = null,
                pendingAccountUpdate = null,
                shouldCloseSheet = true
            )
        }
    }

    fun clearSheetCloseFlag() {
        _uiState.update { it.copy(shouldCloseSheet = false) }
    }

    fun deleteAccount(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                deleteAccountUseCase(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                analyticsTracker.recordException(e, "Account")
            }
        }
    }

}

typealias UiAccount = AccountWithConversion

data class AccountState(
    val accounts: List<UiAccount?> = emptyList(),
    val totalNetWorth: Double = 0.0,
    val totalNetWorthCurrency: Currency = GlobalConfig.baseCurrency,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val isRefreshingRates: Boolean = false,
    val ratesLastUpdated: Long = 0L,
    val ratesError: String? = null,
    val reconciliationDifference: ReconciliationDifference? = null,
    val pendingAccountUpdate: PendingAccountUpdate? = null,
    val shouldCloseSheet: Boolean = false
)

data class PendingAccountUpdate(
    val id: Int,
    val title: String,
    val emoji: String,
    val amount: Double,
    val currency: Currency
)

fun List<UiAccount>.toAccounts(): List<Account> = map { it.toAccount() }
