package com.andriybobchuk.mooney.mooney.presentation.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.AccountWithConversion
import com.andriybobchuk.mooney.mooney.domain.AssetCategory
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.usecase.*
import com.andriybobchuk.mooney.mooney.domain.usecase.GetUserCurrenciesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.SetPrimaryAccountUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.assets.ManageAssetCategoryOrderUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.assets.ManageCategoryExpansionUseCase
import com.andriybobchuk.mooney.core.domain.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class AssetsViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val addAccountUseCase: AddAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val calculateNetWorthUseCase: CalculateNetWorthUseCase,
    private val convertAccountsToUiUseCase: ConvertAccountsToUiUseCase,
    private val currencyManagerUseCase: CurrencyManagerUseCase,
    private val manageAssetCategoryOrderUseCase: ManageAssetCategoryOrderUseCase,
    private val manageCategoryExpansionUseCase: ManageCategoryExpansionUseCase,
    private val shouldRefreshExchangeRatesUseCase: ShouldRefreshExchangeRatesUseCase,
    private val setPrimaryAccountUseCase: SetPrimaryAccountUseCase,
    private val getUserCurrenciesUseCase: GetUserCurrenciesUseCase
) : ViewModel() {

    private var observeAccountsJob: Job? = null

    private val _uiState = MutableStateFlow(AssetsState())

    val state = _uiState
        .onStart {
            observeUserCurrencies()
            observeAssets()
            refreshExchangeRatesOnStart()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _uiState.value
        )

    private fun observeUserCurrencies() {
        getUserCurrenciesUseCase().onEach { currencies ->
            currencyManagerUseCase.setUserCurrencies(currencies.map { it.code })
        }.launchIn(viewModelScope)
    }

    private fun refreshExchangeRatesOnStart() {
        viewModelScope.launch {
            if (shouldRefreshExchangeRatesUseCase(_uiState.value.ratesLastUpdated)) {
                refreshExchangeRates()
            }
        }
    }

    private fun observeAssets() {
        observeAccountsJob?.cancel()
        observeAccountsJob = combine(
            getAccountsUseCase(),
            manageAssetCategoryOrderUseCase.getCategoryOrder(),
            manageCategoryExpansionUseCase.getExpandedCategories()
        ) { accounts, categoryOrder, expandedCategories ->
            Triple(accounts, categoryOrder, expandedCategories)
        }.onEach { (accounts, categoryOrder, expandedCategories) ->
            _uiState.update {
                it.copy(
                    assets = convertAccountsToUiUseCase(accounts).filterNotNull(),
                    categoryOrder = categoryOrder,
                    expandedCategories = expandedCategories
                )
            }
            updateTotalNetWorth()
        }.launchIn(viewModelScope)
    }

    private fun updateTotalNetWorth() {
        val accounts = state.value.assets.map { it.toAccount() }
        val result = calculateNetWorthUseCase(
            accounts = accounts,
            selectedCurrency = currencyManagerUseCase.getCurrentCurrency(),
            baseCurrency = GlobalConfig.baseCurrency
        )
        val baseResult = calculateNetWorthUseCase(
            accounts = accounts,
            selectedCurrency = GlobalConfig.baseCurrency,
            baseCurrency = GlobalConfig.baseCurrency
        )

        _uiState.update {
            it.copy(
                totalNetWorth = result.totalNetWorth,
                baseNetWorth = baseResult.totalNetWorth,
                totalNetWorthCurrency = result.currency
            )
        }
    }

    fun onNetWorthLabelClick() {
        currencyManagerUseCase.cycleToNextCurrency()
        updateTotalNetWorth()
        // Recalculate with new currency
        viewModelScope.launch {
            val accounts = getAccountsUseCase().first()
            _uiState.update { it.copy(assets = convertAccountsToUiUseCase(accounts).filterNotNull()) }
        }
    }

    fun refreshExchangeRates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingRates = true) }
            
            when (val result = currencyManagerUseCase.refreshExchangeRates()) {
                is Result.Success -> {
                    // Refresh UI assets with new rates using current accounts
                    val currentAccounts = getAccountsUseCase().first()
                    _uiState.update {
                        it.copy(
                            assets = convertAccountsToUiUseCase(currentAccounts).filterNotNull(),
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

    fun upsertAsset(
        id: Int,
        title: String,
        emoji: String,
        amount: Double,
        currency: Currency,
        assetCategory: AssetCategory
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val account = Account(id, title, amount, currency, emoji, assetCategory)

            try {
                addAccountUseCase(account)
                // Force immediate recalculation
                val accounts = getAccountsUseCase().first()
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(assets = convertAccountsToUiUseCase(accounts).filterNotNull()) }
                    updateTotalNetWorth()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteAsset(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                deleteAccountUseCase(id)
                val accounts = getAccountsUseCase().first()
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(assets = convertAccountsToUiUseCase(accounts).filterNotNull()) }
                    updateTotalNetWorth()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateCategoryOrder(categories: List<AssetCategory>) {
        viewModelScope.launch {
            manageAssetCategoryOrderUseCase.saveCategoryOrder(categories)
        }
    }

    fun toggleCategoryExpansion(category: AssetCategory) {
        viewModelScope.launch {
            manageCategoryExpansionUseCase.toggleCategoryExpansion(
                category = category,
                currentExpanded = state.value.expandedCategories
            )
        }
    }

    fun setPrimaryAccount(accountId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            setPrimaryAccountUseCase(accountId)
        }
    }
}

typealias UiAsset = AccountWithConversion

data class AssetsState(
    val assets: List<UiAsset> = emptyList(),
    val totalNetWorth: Double = 0.0,
    val baseNetWorth: Double = 0.0,
    val totalNetWorthCurrency: Currency = GlobalConfig.baseCurrency,
    val categoryOrder: List<AssetCategory> = AssetCategory.entries,
    val expandedCategories: Set<AssetCategory> = AssetCategory.entries.toSet(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val isRefreshingRates: Boolean = false,
    val ratesLastUpdated: Long = 0L,
    val ratesError: String? = null
)