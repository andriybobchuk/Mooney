package com.andriybobchuk.mooney.mooney.presentation.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.AccountWithConversion
import com.andriybobchuk.mooney.mooney.domain.AssetCategory
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.core.data.database.AssetCategoryDao
import com.andriybobchuk.mooney.core.data.database.AssetCategoryEntity
import com.andriybobchuk.mooney.core.premium.PremiumConfig
import com.andriybobchuk.mooney.core.premium.PremiumManager
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
import kotlin.coroutines.cancellation.CancellationException

@Suppress("LongParameterList")
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
    private val getUserCurrenciesUseCase: GetUserCurrenciesUseCase,
    private val assetCategoryDao: AssetCategoryDao,
    private val analyticsTracker: AnalyticsTracker,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private var observeAccountsJob: Job? = null

    private val _uiState = MutableStateFlow(AssetsState())

    init {
        observeBaseCurrencyChanges()
    }

    private fun observeBaseCurrencyChanges() {
        GlobalConfig.baseCurrencyFlow.onEach {
            // Re-convert all accounts and recalculate net worth when base currency changes
            observeAssets()
        }.launchIn(viewModelScope)
    }

    val state = _uiState
        .onStart {
            observeUserCurrencies()
            observeAssetCategories()
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

    private fun observeAssetCategories() {
        assetCategoryDao.getAll().onEach { categories ->
            _uiState.update { it.copy(assetCategories = categories) }
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
            val uiAssets = convertAccountsToUiUseCase(accounts).filterNotNull()
            val hasLiabilities = uiAssets.any { asset -> asset.isLiability }
            _uiState.update {
                it.copy(
                    assets = uiAssets,
                    categoryOrder = categoryOrder,
                    expandedCategories = expandedCategories,
                    hasLiabilities = hasLiabilities,
                    // Auto-switch to Assets tab if no liabilities remain
                    selectedTab = if (!hasLiabilities) AssetsTab.ASSETS else it.selectedTab
                )
            }
            updateTotalNetWorth()
        }.launchIn(viewModelScope)
    }

    private fun updateTotalNetWorth() {
        val allAccounts = state.value.assets.map { it.toAccount() }
        val result = calculateNetWorthUseCase(
            accounts = allAccounts,
            selectedCurrency = currencyManagerUseCase.getCurrentCurrency(),
            baseCurrency = GlobalConfig.baseCurrency
        )
        val baseResult = calculateNetWorthUseCase(
            accounts = allAccounts,
            selectedCurrency = GlobalConfig.baseCurrency,
            baseCurrency = GlobalConfig.baseCurrency
        )

        // Compute totals per type for percentage bars
        val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
        val base = GlobalConfig.baseCurrency
        val assetsBase = allAccounts.filter { !it.isLiability }.sumOf { a ->
            if (a.currency != base) exchangeRates.convert(a.amount, a.currency, base) else a.amount
        }
        val liabilitiesBase = allAccounts.filter { it.isLiability }.sumOf { a ->
            if (a.currency != base) exchangeRates.convert(a.amount, a.currency, base) else a.amount
        }

        _uiState.update {
            it.copy(
                totalNetWorth = result.totalNetWorth,
                baseNetWorth = baseResult.totalNetWorth,
                totalAssetsBase = assetsBase,
                totalLiabilitiesBase = liabilitiesBase,
                totalNetWorthCurrency = result.currency
            )
        }
    }

    fun onNetWorthLabelClick() {
        currencyManagerUseCase.cycleToNextCurrency()
        updateTotalNetWorth()
        analyticsTracker.trackEvent(
            AnalyticsEvent.CycleCurrencyDisplay(currencyManagerUseCase.getCurrentCurrency().name)
        )
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
                    analyticsTracker.trackEvent(AnalyticsEvent.RefreshExchangeRates)
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
                    analyticsTracker.log("Exchange rate refresh failed: ${result.error}")
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

    fun dismissPaywall() {
        _uiState.update { it.copy(showPaywall = false) }
    }

    fun upsertAsset(
        id: Int,
        title: String,
        emoji: String,
        amount: Double,
        currency: Currency,
        assetCategoryId: String,
        isLiability: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // Gate: check account limit for new accounts (id == 0 means new)
            if (id == 0) {
                val isPremium = premiumManager.getIsPremium()
                if (!isPremium) {
                    // Count assets and liabilities separately against the limit
                    val allAccounts = getAccountsUseCase().first().filterNotNull()
                    val currentCount = allAccounts.count { it.isLiability == isLiability }
                    if (currentCount >= PremiumConfig.maxFreeAccounts) {
                        withContext(Dispatchers.Main) {
                            _uiState.update { it.copy(showPaywall = true) }
                        }
                        return@launch
                    }
                }
            }

            // Preserve isPrimary when editing existing account
            val existingIsPrimary = if (id != 0) {
                getAccountsUseCase(id)?.isPrimary ?: false
            } else false

            val existingIsLiability = if (id != 0) {
                getAccountsUseCase(id)?.isLiability ?: isLiability
            } else isLiability

            val account = Account(
                id = id,
                title = title,
                amount = amount,
                currency = currency,
                emoji = emoji,
                assetCategory = AssetCategory.fromString(assetCategoryId),
                assetCategoryId = assetCategoryId,
                isPrimary = existingIsPrimary,
                isLiability = existingIsLiability
            )

            try {
                addAccountUseCase(account)
                // Force immediate recalculation
                val accounts = getAccountsUseCase().first()
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(assets = convertAccountsToUiUseCase(accounts).filterNotNull()) }
                    updateTotalNetWorth()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                analyticsTracker.recordException(e, "Assets")
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                analyticsTracker.recordException(e, "Assets")
            }
        }
    }

    fun selectTab(tab: AssetsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun updateCategoryOrder(categories: List<String>) {
        viewModelScope.launch {
            manageAssetCategoryOrderUseCase.saveCategoryOrder(categories)
        }
    }

    fun toggleCategoryExpansion(categoryId: String) {
        viewModelScope.launch {
            manageCategoryExpansionUseCase.toggleCategoryExpansion(
                category = categoryId,
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

enum class AssetsTab { ASSETS, LIABILITIES }

data class AssetsState(
    val assets: List<UiAsset> = emptyList(),
    val totalNetWorth: Double = 0.0,
    val baseNetWorth: Double = 0.0,
    val totalAssetsBase: Double = 0.0,
    val totalLiabilitiesBase: Double = 0.0,
    val totalNetWorthCurrency: Currency = GlobalConfig.baseCurrency,
    val assetCategories: List<AssetCategoryEntity> = emptyList(),
    val categoryOrder: List<String> = emptyList(),
    val expandedCategories: Set<String> = emptySet(),
    val selectedTab: AssetsTab = AssetsTab.ASSETS,
    val hasLiabilities: Boolean = false,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val isRefreshingRates: Boolean = false,
    val ratesLastUpdated: Long = 0L,
    val ratesError: String? = null,
    val showPaywall: Boolean = false
)