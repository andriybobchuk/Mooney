package com.andriybobchuk.mooney.mooney.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.AssetCategory
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.usecase.*
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateNetWorthUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.ConvertAccountsToUiUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.CreateReconciliationUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.CurrencyManagerUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.ReconciliationDifference
import com.andriybobchuk.mooney.core.presentation.theme.ThemeManager
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

class AccountViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val addAccountUseCase: AddAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val calculateNetWorthUseCase: CalculateNetWorthUseCase,
    private val convertAccountsToUiUseCase: ConvertAccountsToUiUseCase,
    private val currencyManagerUseCase: CurrencyManagerUseCase,
    private val createReconciliationUseCase: CreateReconciliationUseCase,
    private val repository: CoreRepository,
    private val themeManager: ThemeManager
) : ViewModel() {

    private var observeAccountsJob: Job? = null

    private val _uiState = MutableStateFlow(AccountState())

    //val state: StateFlow<AccountState> = _uiState
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
            // Only refresh if rates are older than 1 hour (3600000 ms)
            val oneHourAgo = Clock.System.now().toEpochMilliseconds() - 3600000
            val lastUpdate = _uiState.value.ratesLastUpdated
            
            if (lastUpdate == 0L || lastUpdate < oneHourAgo) {
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
                // Update net worth after accounts loaded
                updateTotalNetWorth()
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isError = true, isLoading = false) }
        }
    }

    private fun updateTotalNetWorth() {
        val result = calculateNetWorthUseCase(
            accounts = state.value.accounts.filterNotNull().toAccounts(),
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
                    // Refresh UI accounts with new rates using current accounts
                    val currentAccounts = getAccountsUseCase().first()
                    _uiState.update { 
                        it.copy(
                            accounts = convertAccountsToUiUseCase(currentAccounts),
                            isRefreshingRates = false,
                            ratesLastUpdated = Clock.System.now().toEpochMilliseconds()
                        )
                    }
                    // Update net worth with fresh rates
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
            // Check for reconciliation difference before updating
            val reconciliationDiff = createReconciliationUseCase.detectReconciliationDifference(id, amount)
            
            if (reconciliationDiff?.shouldShowDialog == true) {
                // Store the pending account update and show reconciliation dialog
                _uiState.update { 
                    it.copy(
                        reconciliationDifference = reconciliationDiff,
                        pendingAccountUpdate = PendingAccountUpdate(id, title, emoji, amount, currency)
                    )
                }
            } else {
                // No reconciliation needed or difference too small, proceed with update
                performAccountUpdate(id, title, emoji, amount, currency)
                // Set flag to close sheet since no dialog will be shown
                _uiState.update { it.copy(shouldCloseSheet = true) }
            }
        }
    }
    
    // TEMPORARY TEST FUNCTION - REMOVE AFTER TESTING
    fun testReconciliationDialog() {
        println("TEST: testReconciliationDialog called!")
        
        // Test simple state update first
        _uiState.update { 
            it.copy(isLoading = !it.isLoading) // Simple toggle to see if ANY state updates work
        }
        
        println("TEST: isLoading toggled to ${_uiState.value.isLoading}")
        
        // Create a fake reconciliation difference to test the dialog
        val testAccount = Account(1, "Test Account", 1000.0, Currency.USD, "💰", AssetCategory.BANK_ACCOUNT)
        val testDifference = ReconciliationDifference(
            account = testAccount,
            oldAmount = 1000.0,
            newAmount = 1500.0,
            difference = 500.0,
            isGain = true
        )
        
        println("TEST: Setting reconciliation difference...")
        _uiState.update { 
            it.copy(
                reconciliationDifference = testDifference,
                pendingAccountUpdate = PendingAccountUpdate(1, "Test Account", "💰", 1500.0, Currency.USD)
            )
        }
        println("TEST: State updated! reconciliationDifference = ${_uiState.value.reconciliationDifference}")
    }
    
    /**
     * Confirms reconciliation and creates both the reconciliation transaction and account update
     */
    fun confirmReconciliation() {
        viewModelScope.launch(Dispatchers.IO) {
            val reconciliationDiff = _uiState.value.reconciliationDifference
            val pendingUpdate = _uiState.value.pendingAccountUpdate
            
            if (reconciliationDiff != null && pendingUpdate != null) {
                try {
                    // IMPORTANT: These operations should be atomic to prevent inconsistent state
                    // If app crashes between these two operations, we could have:
                    // 1. Reconciliation transaction exists but account not updated
                    // 2. Account updated but reconciliation transaction missing
                    
                    // Create reconciliation transaction record (no balance change)
                    createReconciliationUseCase.createReconciliationTransaction(reconciliationDiff)
                    
                    // Set account to target amount (the reconciliation transaction explains the difference)
                    performAccountMetadataUpdate(
                        pendingUpdate.id,
                        pendingUpdate.title,
                        pendingUpdate.emoji,
                        pendingUpdate.amount, // This is the target amount
                        pendingUpdate.currency
                    )
                    
                    // TODO: Consider wrapping both operations in a database transaction for atomicity
                } catch (e: Exception) {
                    // Handle error - could show an error message
                    // TODO: Add proper error handling and possibly rollback mechanism
                }
            }
            
            // Clear reconciliation dialog state
            clearReconciliationState()
        }
    }
    
    /**
     * Dismisses reconciliation dialog and proceeds with account update only
     */
    fun dismissReconciliation() {
        val pendingUpdate = _uiState.value.pendingAccountUpdate
        
        // Clear reconciliation dialog state
        clearReconciliationState()
        
        // Proceed with direct account update without reconciliation transaction
        if (pendingUpdate != null) {
            viewModelScope.launch(Dispatchers.IO) {
                performAccountMetadataUpdate(
                    pendingUpdate.id,
                    pendingUpdate.title,
                    pendingUpdate.emoji,
                    pendingUpdate.amount,
                    pendingUpdate.currency
                )
            }
        }
    }
    
    /**
     * Clears reconciliation dialog state
     */
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
    
    /**
     * Performs the actual account update (through normal flow with balance calculations)
     */
    private suspend fun performAccountUpdate(
        id: Int,
        title: String,
        emoji: String,
        amount: Double,
        currency: Currency
    ) {
        val account = Account(id, title, amount, currency, emoji, AssetCategory.BANK_ACCOUNT)
        
        try {
            addAccountUseCase(account)
            observeAccounts()
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    /**
     * Performs direct account metadata update to target amount (bypasses balance calculations)
     * Used after reconciliation transaction has already handled the balance difference
     */
    private suspend fun performAccountMetadataUpdate(
        id: Int,
        title: String,
        emoji: String,
        targetAmount: Double,
        currency: Currency
    ) {
        val account = Account(id, title, targetAmount, currency, emoji, AssetCategory.BANK_ACCOUNT)
        
        try {
            // Directly update account metadata via repository (bypassing use cases)
            // This sets the exact target amount without any balance calculations
            repository.upsertAccount(account)
            observeAccounts()
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun deleteAccount(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                deleteAccountUseCase(id)
            } catch (e: Exception) {
            }
        }
    }
    
    fun toggleTheme() {
        viewModelScope.launch {
            themeManager.toggleTheme()
        }
    }
}

data class UiAccount(
    val id: Int,
    val title: String,
    val emoji: String,
    val originalAmount: Double,
    val originalCurrency: Currency,
    val baseCurrencyAmount: Double,
    val exchangeRate: Double?
)


data class AccountState(
    val accounts: List<UiAccount?> = emptyList(),
    val totalNetWorth: Double = 0.0,
    val totalNetWorthCurrency: Currency = GlobalConfig.baseCurrency,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val isRefreshingRates: Boolean = false,
    val ratesLastUpdated: Long = 0L,
    val ratesError: String? = null,
    // Reconciliation dialog state
    val reconciliationDifference: ReconciliationDifference? = null,
    val pendingAccountUpdate: PendingAccountUpdate? = null,
    // Flag to close sheet after account update
    val shouldCloseSheet: Boolean = false
)

/**
 * Data class to hold pending account update details for reconciliation flow
 */
data class PendingAccountUpdate(
    val id: Int,
    val title: String,
    val emoji: String,
    val amount: Double,
    val currency: Currency
)

fun List<UiAccount>.toAccounts(): List<Account> {
    return this.map { uiAccount ->
        Account(
            id = uiAccount.id,
            title = uiAccount.title,
            emoji = uiAccount.emoji,
            amount = uiAccount.originalAmount,
            currency = uiAccount.originalCurrency,
            assetCategory = AssetCategory.BANK_ACCOUNT
        )
    }
}
