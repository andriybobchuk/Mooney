package com.andriybobchuk.mooney.mooney.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Account

import com.andriybobchuk.mooney.mooney.domain.Currency

import com.andriybobchuk.mooney.mooney.domain.usecase.*
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateNetWorthUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.ConvertAccountsToUiUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.CurrencyManagerUseCase
import com.andriybobchuk.mooney.core.presentation.theme.ThemeManager
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val addAccountUseCase: AddAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val calculateNetWorthUseCase: CalculateNetWorthUseCase,
    private val convertAccountsToUiUseCase: ConvertAccountsToUiUseCase,
    private val currencyManagerUseCase: CurrencyManagerUseCase,
    private val themeManager: ThemeManager
) : ViewModel() {

    private var observeAccountsJob: Job? = null

    private val _uiState = MutableStateFlow(AccountState())

    //val state: StateFlow<AccountState> = _uiState
    val state = _uiState
        .onStart {
            observeAccounts()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _uiState.value
        )

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
            accounts = state.value.accounts,
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

    fun upsertAccount(
        id: Int,
        title: String,
        emoji: String,
        amount: Double,
        currency: Currency
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val account = Account(id, title, amount, currency, emoji)

            try {
                addAccountUseCase(account)
                observeAccounts()
            } catch (e: Exception) {
            }
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
        themeManager.toggleTheme()
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
    val isError: Boolean = false
)

fun List<UiAccount>.toAccounts(): List<Account> {
    return this.map { uiAccount ->
        Account(
            id = uiAccount.id,
            title = uiAccount.title,
            emoji = uiAccount.emoji,
            amount = uiAccount.originalAmount,
            currency = uiAccount.originalCurrency
        )
    }
}
