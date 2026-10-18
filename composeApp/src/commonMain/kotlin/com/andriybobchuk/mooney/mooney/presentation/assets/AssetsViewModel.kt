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
import com.andriybobchuk.mooney.core.premium.PremiumConfig
import com.andriybobchuk.mooney.core.premium.PremiumManager
import com.andriybobchuk.mooney.core.premium.PurchaseResult
import com.andriybobchuk.mooney.mooney.domain.usecase.*
import com.andriybobchuk.mooney.mooney.domain.usecase.GetUserCurrenciesUseCase
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

@Suppress("LongParameterList", "TooManyFunctions")
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
    private val getUserCurrenciesUseCase: GetUserCurrenciesUseCase,
    private val assetCategoryDao: AssetCategoryDao,
    private val analyticsTracker: AnalyticsTracker,
    private val premiumManager: PremiumManager,
    private val loadHistoricalRatesUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.LoadHistoricalRatesUseCase,
    private val calculateRatePercentileUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.CalculateRatePercentileUseCase,
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
    private val trackFirstEventUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.TrackFirstEventUseCase,
    private val appDataCache: com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache,
    private val reconcileAccountFromBalanceUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.ReconcileAccountFromBalanceUseCase,
    private val accountImagePickerFacade: com.andriybobchuk.mooney.core.platform.AccountImagePickerFacade,
    private val imageStorage: com.andriybobchuk.mooney.core.data.ImageStorage,
    private val catchUpNetWorthSnapshotsUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.networth.CatchUpNetWorthSnapshotsUseCase,
    private val getNetWorthHistoryUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.networth.GetNetWorthHistoryUseCase,
    private val setManualNetWorthUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.networth.SetManualNetWorthUseCase
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
            observeRecentTransactions()
            catchUpNetWorthSnapshots()
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

    /** Mirrors the shared cache's transaction list into our state so the
     *  AssetDetailSheet can show per-account recent-transaction history
     *  without needing its own DAO reference. */
    /**
     * Fires once per Balance-tab visit. Backfills any month-ends that
     * elapsed since the last snapshot (idempotent — unique index on the
     * table makes repeat runs a no-op for already-captured months). Runs
     * on IO; silent failures by design — snapshot history is a convenience
     * feature, never block UI if the catch-up hits a transient error.
     */
    private fun catchUpNetWorthSnapshots() {
        viewModelScope.launch(Dispatchers.IO) {
            catchUpNetWorthSnapshotsUseCase()
        }
    }

    private fun observeRecentTransactions() {
        appDataCache.snapshot
            .map { it.transactions }
            .onEach { txs -> _uiState.update { it.copy(recentTransactions = txs) } }
            .launchIn(viewModelScope)
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
        // Read from `_uiState` (initialized on line 76) rather than `state`
        // (initialized further down). The `init { observeBaseCurrencyChanges() }`
        // block above can trigger this method via a coroutine before the
        // `state` property line has run, at which point reading `state.value`
        // NPEs on the still-null JVM field. Crashlytics caught this on
        // Android in the first prod release. `_uiState.value` holds the
        // exact same snapshot with no race.
        val allAccounts = _uiState.value.assets.map { it.toAccount() }
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

        // Compute totals per type for percentage bars. Opted-out accounts
        // ("Include in total net worth" toggle) drop out of these totals so
        // the summary at the top of the Balance screen honors the flag.
        val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
        val base = GlobalConfig.baseCurrency
        val countedAccounts = allAccounts.filter { it.includeInNetWorth }
        val assetsBase = countedAccounts.filter { !it.isLiability }.sumOf { a ->
            if (a.currency != base) exchangeRates.convert(a.amount, a.currency, base) else a.amount
        }
        val liabilitiesBase = countedAccounts.filter { it.isLiability }.sumOf { a ->
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

    fun onSubscribe(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true, purchaseError = null) }
            try {
                val result = kotlinx.coroutines.withTimeoutOrNull(25_000L) {
                    premiumManager.purchase(productId)
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
        isLiability: Boolean = false,
        currentMarketValue: Double? = null,
        includeInNetWorth: Boolean = true,
        isPrimaryForExpenses: Boolean = false,
        isPrimaryForIncome: Boolean = false
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

            // Preserve ALL fields the edit sheet doesn't manage. The sheet
            // only collects form inputs; anything else (isPrimary legacy
            // flag, reconciliation history, photo path) must be carried
            // forward from the existing row — otherwise saving the form
            // nulls out those fields. The user hit this with "updating the
            // account title fully removes the photo from it" — that was
            // this exact class of bug.
            val existing = if (id != 0) getAccountsUseCase(id) else null
            val existingIsPrimary = existing?.isPrimary ?: false
            val existingIsLiability = existing?.isLiability ?: isLiability

            val account = Account(
                id = id,
                title = title,
                amount = amount,
                currency = currency,
                emoji = emoji,
                assetCategory = AssetCategory.fromString(assetCategoryId),
                assetCategoryId = assetCategoryId,
                isPrimary = existingIsPrimary,
                isLiability = existingIsLiability,
                currentMarketValue = currentMarketValue,
                includeInNetWorth = includeInNetWorth,
                isPrimaryForExpenses = isPrimaryForExpenses,
                isPrimaryForIncome = isPrimaryForIncome,
                lastReconciledAt = existing?.lastReconciledAt,
                imagePath = existing?.imagePath
            )

            // Single-primary-per-role invariant. If this account is being
            // promoted to primary-for-expenses or primary-for-income, clear
            // the same flag on every other account first — otherwise the UI
            // would show two "primary" badges for the same role.
            if (isPrimaryForExpenses || isPrimaryForIncome) {
                val allAccounts = getAccountsUseCase().first().filterNotNull()
                allAccounts.forEach { other ->
                    if (other.id == id) return@forEach
                    val newE = if (isPrimaryForExpenses) false else other.isPrimaryForExpenses
                    val newI = if (isPrimaryForIncome) false else other.isPrimaryForIncome
                    if (newE != other.isPrimaryForExpenses || newI != other.isPrimaryForIncome) {
                        addAccountUseCase(
                            other.copy(
                                isPrimaryForExpenses = newE,
                                isPrimaryForIncome = newI
                            )
                        )
                    }
                }
            }

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
                trackPrimaryAccountRole(isPrimaryForExpenses, isPrimaryForIncome)
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

    /** Emit the `primary_account_set` event with a bucketed role. Extracted
     *  from `upsertAsset` to keep that method under the detekt cyclomatic
     *  threshold. */
    private fun trackPrimaryAccountRole(forExpenses: Boolean, forIncome: Boolean) {
        if (!forExpenses && !forIncome) return
        val role = when {
            forExpenses && forIncome -> "both"
            forExpenses -> "expense"
            else -> "income"
        }
        analyticsTracker.trackEvent(AnalyticsEvent.PrimaryAccountSet(role))
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

    /**
     * Reconcile wizard commit — delegates to [ReconcileAccountFromBalanceUseCase]
     * which inserts the adjustment transaction (if any) and overwrites the
     * account balance to exactly the user's input in one atomic sequence.
     * Fires `reconciliation_completed` analytics with a signed-delta bucket.
     */
    fun reconcileAccount(accountId: Int, newBalance: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isReconciling = true) }
                val outcome = reconcileAccountFromBalanceUseCase(accountId, newBalance)
                if (outcome != null) {
                    analyticsTracker.trackEvent(
                        AnalyticsEvent.ReconciliationCompleted(
                            delta = outcome.delta,
                            adjustmentInserted = outcome.adjustmentInserted
                        )
                    )
                }
                val accounts = getAccountsUseCase().first()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            assets = convertAccountsToUiUseCase(accounts).filterNotNull(),
                            isReconciling = false,
                            lastReconcileOutcome = outcome
                        )
                    }
                    updateTotalNetWorth()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isReconciling = false) }
                analyticsTracker.recordException(e, "Reconcile")
            }
        }
    }

    fun clearReconcileOutcome() {
        _uiState.update { it.copy(lastReconcileOutcome = null) }
    }

    /**
     * Flips the account's `includeInNetWorth` flag inline from the detail
     * sheet, without opening the full Edit flow. Persists via the same
     * upsert path the Add/Edit screen uses.
     */
    /**
     * Launches the native photo picker, saves the chosen image under the
     * app's Documents directory, deletes any previous image on this
     * account, and persists the new path on the Account.
     */
    fun pickAccountImage(accountId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { _uiState.update { it.copy(isPickingImage = true) } }
            try {
                val newPath = accountImagePickerFacade.pickImage()
                if (newPath == null) {
                    withContext(Dispatchers.Main) { _uiState.update { it.copy(isPickingImage = false) } }
                    return@launch
                }
                val current = getAccountsUseCase(accountId) ?: run {
                    withContext(Dispatchers.Main) { _uiState.update { it.copy(isPickingImage = false) } }
                    return@launch
                }
                current.imagePath
                    ?.takeIf { it.isNotBlank() && it != newPath }
                    ?.let { oldPath -> runCatching { imageStorage.deleteImage(oldPath) } }
                addAccountUseCase(current.copy(imagePath = newPath))
                val accounts = getAccountsUseCase().first()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            assets = convertAccountsToUiUseCase(accounts).filterNotNull(),
                            isPickingImage = false
                        )
                    }
                }
            } catch (e: CancellationException) {
                withContext(Dispatchers.Main) { _uiState.update { it.copy(isPickingImage = false) } }
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _uiState.update { it.copy(isPickingImage = false) } }
                analyticsTracker.recordException(e, "AccountImage")
            }
        }
    }

    /** Removes the current image from storage + clears the path on account. */
    fun removeAccountImage(accountId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val current = getAccountsUseCase(accountId) ?: return@launch
                current.imagePath
                    ?.takeIf { it.isNotBlank() }
                    ?.let { path -> runCatching { imageStorage.deleteImage(path) } }
                addAccountUseCase(current.copy(imagePath = null))
                val accounts = getAccountsUseCase().first()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(assets = convertAccountsToUiUseCase(accounts).filterNotNull())
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                analyticsTracker.recordException(e, "AccountImage")
            }
        }
    }

    fun toggleIncludeInNetWorth(accountId: Int, include: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val current = getAccountsUseCase(accountId) ?: return@launch
                addAccountUseCase(current.copy(includeInNetWorth = include))
                val accounts = getAccountsUseCase().first()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(assets = convertAccountsToUiUseCase(accounts).filterNotNull())
                    }
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

    /**
     * Pulls the snapshot timeline for the Flex screen. Called from the
     * Balance screen the moment the user long-presses the net-worth header.
     * The catch-up job runs on tab visit, so the DB should already be fresh
     * — this is just a point-in-time read.
     */
    fun loadNetWorthHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val points = getNetWorthHistoryUseCase()
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(netWorthHistory = points) }
            }
        }
    }

    /**
     * User-entered override from the Flex screen's inline edit. Writes the
     * manual value, then re-reads history so the UI reflects it. Uses the
     * last day of the month as the canonical date key — matches the
     * month-end dates used by the catch-up job.
     */
    fun setManualNetWorth(monthEnd: kotlinx.datetime.LocalDate, netWorth: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            setManualNetWorthUseCase(monthEnd, netWorth)
            val points = getNetWorthHistoryUseCase()
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(netWorthHistory = points) }
            }
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
    val showAssetsOnlyInTopBar: Boolean = false,
    /** All transactions, straight from the app cache. The AssetDetailSheet
     *  filters by accountId + takes the last 15 to show recent activity on
     *  the selected account — keeps the ViewModel out of per-account
     *  filtering logic. Lifetime matches the Assets screen; cheap to pass
     *  since Transaction is a value class and the cache already keeps the
     *  authoritative list in memory. */
    val recentTransactions: List<com.andriybobchuk.mooney.mooney.domain.Transaction> = emptyList(),
    /** True while [ReconcileAccountFromBalanceUseCase] is inserting the
     *  adjustment + updating the account. Drives the sheet's spinner. */
    val isReconciling: Boolean = false,
    /** True while the native photo picker is up AND / OR we're saving the
     *  picked image. The AssetDetailSheet shows a loading overlay on the
     *  hero slot while this is true. */
    val isPickingImage: Boolean = false,
    /** Monthly net-worth history for the Flex screen's timeline. Loaded
     *  lazily when the Flex sheet opens; refreshed after a manual edit. */
    val netWorthHistory: List<com.andriybobchuk.mooney.mooney.domain.usecase.networth.GetNetWorthHistoryUseCase.Point> = emptyList(),
    /** Set after reconciliation completes; the Balance screen reads this to
     *  show a toast with the delta. Cleared via [clearReconcileOutcome]. */
    val lastReconcileOutcome: com.andriybobchuk.mooney.mooney.domain.usecase.ReconcileOutcome? = null
)