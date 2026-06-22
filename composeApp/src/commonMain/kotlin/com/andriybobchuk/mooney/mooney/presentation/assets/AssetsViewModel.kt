package com.andriybobchuk.mooney.mooney.presentation.assets

import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import kotlinx.coroutines.flow.map
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.AccountWithConversion
import com.andriybobchuk.mooney.mooney.domain.AssetCategory
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.core.data.database.AssetCategoryDao
import com.andriybobchuk.mooney.core.data.database.AssetCategoryEntity
import com.andriybobchuk.mooney.core.premium.PRODUCT_ID_MONTHLY
import com.andriybobchuk.mooney.core.premium.PremiumConfig
import com.andriybobchuk.mooney.core.premium.PremiumManager
import com.andriybobchuk.mooney.core.premium.PurchaseResult
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
    private val premiumManager: PremiumManager,
    private val loadHistoricalRatesUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.LoadHistoricalRatesUseCase,
    private val calculateRatePercentileUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.CalculateRatePercentileUseCase,
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
    private val trackFirstEventUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.TrackFirstEventUseCase,
    private val appDataCache: com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache
) : ViewModel() {

    private var observeAccountsJob: Job? = null

    // Always default to isInitialLoading=true (do NOT seed from cache).
    // Net worth needs computation (convert each foreign-currency account into
    // base, sum, etc.) — even when the raw account list is cached, the
    // computed total is not. Seeding from cache.isReady was making the
    // shimmer never appear on first-visit-after-launch because the cache
    // pre-warm in NavigationHost won the race. Tab switching still skips
    // the shimmer because the VM is shared across the nav graph, so the
    // already-loaded state survives navigation.
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
            observeCurrencyInsightsSetting()
            observeAssetsOnlyInTopBar()
            // Refresh rates BEFORE observing assets so conversions use live rates
            ensureRatesLoaded()
            observeAssets()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _uiState.value
        )

    private fun observeUserCurrencies() {
        // Pulled from the app cache so we share one subscription across the app.
        appDataCache.snapshot.map { it.userCurrencies }.onEach { currencies ->
            val codes = currencies.map { it.code }
            currencyManagerUseCase.setUserCurrencies(codes)
            val resolved = codes.mapNotNull { code -> Currency.entries.find { it.name == code } }
            _uiState.update { it.copy(userCurrencies = resolved) }
        }.launchIn(viewModelScope)
    }

    private fun observeCurrencyInsightsSetting() {
        dataStore.data.map { prefs ->
            prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.CURRENCY_INSIGHTS_ENABLED] ?: false
        }.onEach { enabled ->
            val wasEnabled = _uiState.value.currencyInsightsEnabled
            _uiState.update { it.copy(currencyInsightsEnabled = enabled) }
            if (enabled && !wasEnabled) {
                loadCurrencyInsights(_uiState.value.assets)
            }
        }.launchIn(viewModelScope)
    }

    private fun observeAssetsOnlyInTopBar() {
        dataStore.data.map { prefs ->
            prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.ASSETS_ONLY_IN_TOP_BAR] ?: false
        }.onEach { enabled ->
            _uiState.update { it.copy(showAssetsOnlyInTopBar = enabled) }
        }.launchIn(viewModelScope)
    }

    fun toggleAssetsOnlyInTopBar() {
        viewModelScope.launch {
            val newValue = !_uiState.value.showAssetsOnlyInTopBar
            dataStore.edit { prefs ->
                prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.ASSETS_ONLY_IN_TOP_BAR] = newValue
            }
            _uiState.update { it.copy(showAssetsOnlyInTopBar = newValue) }
        }
    }

    private fun observeAssetCategories() {
        // Read asset categories from the app cache — one shared subscription
        // for the whole app instead of a per-VM DAO query.
        appDataCache.snapshot.map { it.assetCategories }.onEach { categories ->
            _uiState.update { it.copy(assetCategories = categories) }
        }.launchIn(viewModelScope)
    }

    private suspend fun ensureRatesLoaded() {
        if (shouldRefreshExchangeRatesUseCase(_uiState.value.ratesLastUpdated)) {
            val result = currencyManagerUseCase.refreshExchangeRates()
            if (result is Result.Success) {
                _uiState.update {
                    it.copy(
                        ratesLastUpdated = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                        exchangeRates = result.data
                    )
                }
            }
        } else {
            _uiState.update { it.copy(exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()) }
        }
        // Clear the initial-load shimmer once the rate attempt has resolved
        // (success or failure) so we never get stuck shimmering forever.
        _uiState.update { it.copy(isInitialLoading = false) }
    }

    private fun observeAssets() {
        observeAccountsJob?.cancel()
        observeAccountsJob = combine(
            // Accounts come from the shared app cache — pre-warmed at app
            // start, survives tab switches, no re-query cost.
            appDataCache.snapshot.map { it.accounts },
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
                    isInitialLoading = false,
                    // Auto-switch to Assets tab if no liabilities remain
                    selectedTab = if (!hasLiabilities) AssetsTab.ASSETS else it.selectedTab
                )
            }
            updateTotalNetWorth()
            loadCurrencyInsights(uiAssets)
        }.launchIn(viewModelScope)
    }

    private fun loadCurrencyInsights(assets: List<UiAsset>) {
        if (!_uiState.value.currencyInsightsEnabled) return
        viewModelScope.launch {
            try {
                val base = GlobalConfig.baseCurrency
                val foreignCurrencies = assets
                    .map { it.originalCurrency }
                    .filter { it != base }
                    .distinct()
                if (foreignCurrencies.isEmpty()) return@launch

                // Compute current rates: "how many base per 1 foreign"
                val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
                val currentRatesMap = foreignCurrencies.associateWith { currency ->
                    exchangeRates.convert(1.0, currency, base)
                }

                // Load historical (from=base, to=foreign), then invert
                val rawHistorical = loadHistoricalRatesUseCase(base, foreignCurrencies, months = 6)
                val invertedHistorical = rawHistorical.mapValues { (_, rates) ->
                    rates.map { hr ->
                        com.andriybobchuk.mooney.mooney.domain.HistoricalRate(
                            hr.date,
                            if (hr.rate != 0.0) 1.0 / hr.rate else 0.0
                        )
                    }
                }

                // Percentiles
                val percentiles = foreignCurrencies.associateWith { currency ->
                    val history = invertedHistorical[currency] ?: return@associateWith 50
                    val currentRate = currentRatesMap[currency] ?: return@associateWith 50
                    calculateRatePercentileUseCase(currentRate, history)
                }

                _uiState.update {
                    it.copy(
                        historicalRates = invertedHistorical,
                        currentRates = currentRatesMap,
                        percentiles = percentiles
                    )
                }
            } catch (_: Exception) { }
        }
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
    }

    fun refreshExchangeRates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingRates = true) }
            when (val result = currencyManagerUseCase.refreshExchangeRates()) {
                is Result.Success -> {
                    analyticsTracker.log("Exchange rates refreshed (Assets)")
                    val currentAccounts = getAccountsUseCase().first()
                    val uiAssets = convertAccountsToUiUseCase(currentAccounts).filterNotNull()
                    _uiState.update {
                        it.copy(
                            assets = uiAssets,
                            isRefreshingRates = false,
                            ratesLastUpdated = Clock.System.now().toEpochMilliseconds()
                        )
                    }
                    updateTotalNetWorth()
                    loadCurrencyInsights(uiAssets)
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
        _uiState.update { it.copy(showPaywall = false, purchaseError = null) }
    }

    fun onSubscribe() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true, purchaseError = null) }
            try {
                val result = kotlinx.coroutines.withTimeoutOrNull(25_000L) {
                    premiumManager.purchase(PRODUCT_ID_MONTHLY)
                }
                when (result) {
                    is PurchaseResult.Success -> _uiState.update { it.copy(showPaywall = false, isPurchasing = false) }
                    is PurchaseResult.Cancelled -> _uiState.update { it.copy(isPurchasing = false) }
                    is PurchaseResult.Error -> _uiState.update { it.copy(isPurchasing = false, purchaseError = result.message) }
                    null -> _uiState.update {
                        it.copy(
                            isPurchasing = false,
                            purchaseError = "Purchase didn't respond in time. Please try again."
                        )
                    }
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isPurchasing = false, purchaseError = e.message) }
            }
        }
    }

    fun onRestorePurchases() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true, purchaseError = null) }
            try {
                val restored = premiumManager.restorePurchases()
                if (restored) {
                    _uiState.update { it.copy(showPaywall = false, isPurchasing = false) }
                } else {
                    _uiState.update { it.copy(isPurchasing = false, purchaseError = "No active subscription found") }
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isPurchasing = false, purchaseError = e.message) }
            }
        }
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
                    val allAccounts = getAccountsUseCase().first().filterNotNull()
                    val currentCount = allAccounts.size
                    if (currentCount >= PremiumConfig.maxFreeAccounts) {
                        analyticsTracker.trackEvent(AnalyticsEvent.FeatureLimitHit("accounts"))
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
                if (id == 0) {
                    analyticsTracker.trackEvent(
                        AnalyticsEvent.AccountAdded(
                            currency = currency.name,
                            isLiability = existingIsLiability
                        )
                    )
                    trackFirstEventUseCase.firstAccount()
                }
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
    val isInitialLoading: Boolean = true,
    val ratesError: String? = null,
    val showPaywall: Boolean = false,
    val isPurchasing: Boolean = false,
    val purchaseError: String? = null,
    val historicalRates: Map<Currency, List<com.andriybobchuk.mooney.mooney.domain.HistoricalRate>> = emptyMap(),
    val percentiles: Map<Currency, Int> = emptyMap(),
    val currentRates: Map<Currency, Double> = emptyMap(),
    val currencyInsightsEnabled: Boolean = false,
    val userCurrencies: List<Currency> = emptyList(),
    val exchangeRates: com.andriybobchuk.mooney.mooney.domain.ExchangeRates =
        com.andriybobchuk.mooney.mooney.domain.ExchangeRates(emptyMap()),
    /**
     * When true, the Assets top-bar shows gross assets instead of net worth.
     * Toggle from the overflow menu in the top bar — only useful for users
     * who track liabilities and want a clean view of just what they own.
     */
    val showAssetsOnlyInTopBar: Boolean = false
)