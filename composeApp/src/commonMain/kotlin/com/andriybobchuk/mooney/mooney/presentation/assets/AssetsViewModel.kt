package com.andriybobchuk.mooney.mooney.presentation.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.AssetCategory
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.usecase.*
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
import kotlinx.datetime.Clock

class AssetsViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val addAccountUseCase: AddAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val calculateNetWorthUseCase: CalculateNetWorthUseCase,
    private val convertAccountsToUiUseCase: ConvertAccountsToUiUseCase,
    private val currencyManagerUseCase: CurrencyManagerUseCase,
    private val manageAssetCategoryOrderUseCase: ManageAssetCategoryOrderUseCase,
    private val manageCategoryExpansionUseCase: ManageCategoryExpansionUseCase
) : ViewModel() {

    private var observeAccountsJob: Job? = null

    private val _uiState = MutableStateFlow(AssetsState())

    val state = _uiState
        .onStart {
            observeAssets()
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
                    assets = convertToUiAssets(accounts),
                    categoryOrder = categoryOrder,
                    expandedCategories = expandedCategories
                )
            }
            updateTotalNetWorth()
        }.launchIn(viewModelScope)
    }

    private fun convertToUiAssets(accounts: List<Account?>): List<UiAsset> {
        return accounts.filterNotNull().map { account ->
            val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
            val baseCurrencyAmount = if (account.currency != GlobalConfig.baseCurrency) {
                exchangeRates.convert(account.amount, account.currency, GlobalConfig.baseCurrency)
            } else {
                account.amount
            }
            
            val exchangeRate = if (account.currency != GlobalConfig.baseCurrency) {
                exchangeRates.rates[account.currency]
            } else {
                null
            }
            
            UiAsset(
                id = account.id,
                title = account.title,
                emoji = account.emoji,
                originalAmount = account.amount,
                originalCurrency = account.currency,
                baseCurrencyAmount = baseCurrencyAmount,
                exchangeRate = exchangeRate,
                assetCategory = account.assetCategory
            )
        }
    }

    private fun updateTotalNetWorth() {
        val accounts = state.value.assets.map { it.toAccount() }
        val result = calculateNetWorthUseCase(
            accounts = accounts,
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
        // Recalculate with new currency
        viewModelScope.launch {
            val accounts = getAccountsUseCase().first()
            _uiState.update { it.copy(assets = convertToUiAssets(accounts)) }
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
                            assets = convertToUiAssets(currentAccounts),
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
                observeAssets()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteAsset(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                deleteAccountUseCase(id)
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
    
}

data class UiAsset(
    val id: Int,
    val title: String,
    val emoji: String,
    val originalAmount: Double,
    val originalCurrency: Currency,
    val baseCurrencyAmount: Double,
    val exchangeRate: Double?,
    val assetCategory: AssetCategory
) {
    fun toAccount(): Account = Account(
        id = id,
        title = title,
        amount = originalAmount,
        currency = originalCurrency,
        emoji = emoji,
        assetCategory = assetCategory
    )
}

data class AssetsState(
    val assets: List<UiAsset> = emptyList(),
    val totalNetWorth: Double = 0.0,
    val totalNetWorthCurrency: Currency = GlobalConfig.baseCurrency,
    val categoryOrder: List<AssetCategory> = AssetCategory.entries,
    val expandedCategories: Set<AssetCategory> = AssetCategory.entries.toSet(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val isRefreshingRates: Boolean = false,
    val ratesLastUpdated: Long = 0L,
    val ratesError: String? = null
)