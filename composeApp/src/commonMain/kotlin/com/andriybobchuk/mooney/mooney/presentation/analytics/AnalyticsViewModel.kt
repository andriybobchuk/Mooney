package com.andriybobchuk.mooney.mooney.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.AnalyticsMetric
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategorySheetType
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.MonthlyMetricSnapshot
import com.andriybobchuk.mooney.mooney.domain.TopCategorySummary
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.mooney.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class AnalyticsState(
    val selectedMonth: MonthKey = MonthKey.current(),
    val totalRevenuePlnForMonth: Double = 0.0,
    val transactionsForMonth: List<Transaction?> = emptyList(),
    val metrics: List<AnalyticsMetric> = emptyList(),
    val historicalMetrics: List<MonthlyMetricSnapshot> = emptyList(),
    val topCategories: List<TopCategorySummary> = emptyList(),
    val subcategories: List<TopCategorySummary> = emptyList(),
    val selectedCategory: Category? = null,
    val isSubcategorySheetOpen: Boolean = false,
    val isCategorySheetOpen: Boolean = false,
    val categorySheetType: CategorySheetType? = null,
    /**
     * Sticky record of the last [CategorySheetType] loaded into [sheetCategories]
     * — set by both the inline sheet path AND the full-screen [AnalyticsBreakdownScreen]
     * route. Unlike [categorySheetType], this outlives sheet dismissal so
     * [setCategoryMonthlyLimit] knows which breakdown to re-fetch after a budget
     * change even when the inline sheet was never open (e.g. user is on the
     * Expenses full screen).
     */
    val lastLoadedSheetType: CategorySheetType? = null,
    val isNetIncomeSheetOpen: Boolean = false,
    val isLoading: Boolean = false,
    /**
     * True until the first metrics + historical calculation finishes. Drives
     * the cold-start shimmer so we never render the analytics layout with
     * placeholder zeros while the real numbers are still being computed.
     */
    val isInitialLoading: Boolean = true,
    val sheetCategories: List<TopCategorySummary> = emptyList(),
    val isTransactionsSheetOpen: Boolean = false,
    val transactionsSheetCategory: Category? = null,
    val transactionsForCategory: List<Transaction> = emptyList(),
    /** Snapshot of current rates so UI can convert transaction amounts to base currency. */
    val exchangeRates: com.andriybobchuk.mooney.mooney.domain.ExchangeRates =
        com.andriybobchuk.mooney.mooney.domain.ExchangeRates(emptyMap()),
    /** Lifetime view loads lazily — these track its state. */
    val lifetimeMetrics: List<MonthlyMetricSnapshot> = emptyList(),
    val isLifetimeLoading: Boolean = false,
    val lifetimeLoaded: Boolean = false,
    /**
     * Sum of all account balances (assets minus liabilities) converted to
     * base currency. Updated whenever accounts emit.
     */
    val currentNetWorth: Double = 0.0,
    /**
     * Per-month transaction count across the entire ledger — INCLUDING future
     * months if the user has planned any. Drives the month-picker's caption
     * (dots under each cell) and, load-bearing, the future-month unlock: any
     * future month with count > 0 becomes selectable in the picker.
     */
    val monthlyTransactionCounts: Map<MonthKey, Int> = emptyMap()
)

@Suppress("LongParameterList")
class AnalyticsViewModel(
    private val calculateMonthlyAnalyticsUseCase: CalculateMonthlyAnalyticsUseCase,
    private val calculateSubcategoriesUseCase: CalculateSubcategoriesUseCase,
    private val currencyManagerUseCase: CurrencyManagerUseCase,
    private val calculateAnalyticsMetricsUseCase: CalculateAnalyticsMetricsUseCase,
    private val loadHistoricalAnalyticsUseCase: LoadHistoricalAnalyticsUseCase,
    private val loadCategoriesForSheetTypeUseCase: LoadCategoriesForSheetTypeUseCase,
    private val getPreviousMonthTransactionsUseCase: GetPreviousMonthTransactionsUseCase,
    private val analyticsTracker: AnalyticsTracker,
    // Net worth on the Analytics card = sum of current account balances. Both
    // sources cheap; observing accounts via the cache means the card updates
    // when balances change without an extra subscription.
    private val getAccountsUseCase: GetAccountsUseCase,
    private val calculateNetWorthUseCase: CalculateNetWorthUseCase,
    // Direct DAO access so the "Set a limit" button in the category-detail
    // sheet can persist without a round-trip through a dedicated use case.
    // The list refresh cascades via CoreRepository.reloadCategories().
    private val categoryDao: com.andriybobchuk.mooney.core.data.database.CategoryDao,
    private val coreRepository: com.andriybobchuk.mooney.mooney.domain.CoreRepository,
    // Cache exposes the full transactions list — needed to source the
    // month-picker's counts (including future months, so the picker can
    // unlock them for viewing).
    private val appDataCache: com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache
) : ViewModel() {
    private var baseCurrency: Currency = GlobalConfig.baseCurrency

    // Note: do NOT seed isInitialLoading from the cache. Cache warmth only
    // means raw transactions/accounts are loaded — analytics still needs to
    // recompute its derived metrics (revenue/expense/categories per month),
    // and rendering the screen with empty `metrics` while that runs would
    // look like a flash of zeros. Keep `isInitialLoading = true` until the
    // first loadMetricsForMonth() actually finishes.
    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state

    init {
        loadMetricsForMonth(_state.value.selectedMonth)
        loadHistoricalData()
        observeBaseCurrency()
        observeNetWorth()
        observeCategoryChanges()
        observeMonthlyTransactionCounts()
    }

    /**
     * Populate [AnalyticsState.monthlyTransactionCounts] from the full ledger.
     * Uses appDataCache so future-month transactions are captured (the historical
     * metrics feed only covers past months). When counts change we ALSO clamp
     * [selectedMonth] if it's beyond the latest month with data — that prevents
     * the picker showing a selected chip for a month the user just deleted the
     * last transaction from.
     */
    private fun observeMonthlyTransactionCounts() {
        // Deliberately NO drop(1): when the AnalyticsViewModel is constructed
        // AFTER the appDataCache is already warm (user opened another tab
        // first, cache populated, THEN switched to Analytics), the very first
        // emission carries the real transactions. Dropping it made planned
        // future months invisible until the user added/deleted another tx.
        appDataCache.snapshot
            .map { it.transactions }
            .onEach { transactions ->
                val counts = transactions.groupingBy {
                    MonthKey(it.date.year, it.date.monthNumber)
                }.eachCount()
                _state.update { it.copy(monthlyTransactionCounts = counts) }
                clampSelectedMonthIfNeeded(counts)
                // Lifetime metrics is a heavy 60-month scan cached behind
                // lifetimeLoaded. If we don't invalidate on transaction changes
                // the Lifetime chart stays stale until app restart. Any change
                // to the ledger => stale => next tab open recomputes.
                if (_state.value.lifetimeLoaded) {
                    _state.update { it.copy(lifetimeLoaded = false) }
                    loadLifetimeData()
                }
                // Same problem for the historical (6mo/1y) dataset, which
                // powers the default chart view. Refresh it in the background.
                loadHistoricalData()
            }
            .launchIn(viewModelScope)
    }

    /**
     * The anchor month for both the historical (6mo/1y) window and Lifetime
     * — takes the furthest month with data if it's past today, otherwise
     * today. This is what makes a planned Sep tx pull the 6mo chart window
     * to Apr-Sep instead of clipping to Mar-Aug.
     */
    private fun chartAnchorMonth(): MonthKey {
        val current = MonthKey.current()
        val currentOrdinal = current.year * 12 + current.month
        val furthest = _state.value.monthlyTransactionCounts.keys
            .maxByOrNull { it.year * 12 + it.month }
        return if (furthest != null &&
            (furthest.year * 12 + furthest.month) > currentOrdinal
        ) {
            furthest
        } else {
            current
        }
    }

    private fun clampSelectedMonthIfNeeded(counts: Map<MonthKey, Int>) {
        val current = MonthKey.current()
        val selected = _state.value.selectedMonth
        val isFutureSelection = selected.year > current.year ||
            (selected.year == current.year && selected.month > current.month)
        if (isFutureSelection && (counts[selected] ?: 0) == 0) {
            // User deleted the last future-month tx that made this month
            // reachable — snap back to current month.
            _state.update { it.copy(selectedMonth = current) }
            loadMetricsForMonth(current)
        }
    }

    /**
     * Watch every categories DB write and auto-refresh the currently-displayed
     * breakdown. This is what actually guarantees the budget bar appears on
     * the Expenses row the moment the user hits Save — regardless of which
     * entry path they used (inline sheet on the tab, full-screen breakdown
     * from a card tap, or a future new surface).
     *
     * Skips the very first emission — that's the initial snapshot, and
     * running loadCategoriesForSheetType before [lastLoadedSheetType] is set
     * would be a wasted call.
     */
    private fun observeCategoryChanges() {
        viewModelScope.launch {
            categoryDao.getAll()
                .drop(1)
                .collect {
                    val sheetType = _state.value.lastLoadedSheetType ?: return@collect
                    loadCategoriesForSheetType(sheetType)
                    loadMetricsForMonth(_state.value.selectedMonth)
                }
        }
    }

    private fun observeNetWorth() {
        viewModelScope.launch {
            getAccountsUseCase().collect { rawAccounts ->
                val accounts = rawAccounts.filterNotNull()
                val result = calculateNetWorthUseCase(
                    accounts = accounts,
                    selectedCurrency = baseCurrency,
                    baseCurrency = baseCurrency
                )
                _state.update { it.copy(currentNetWorth = result.totalNetWorth) }
            }
        }
    }

    private fun observeBaseCurrency() {
        viewModelScope.launch {
            GlobalConfig.baseCurrencyFlow.collect { newCurrency ->
                if (newCurrency != baseCurrency) {
                    baseCurrency = newCurrency
                    // Lifetime numbers are denominated in the old currency —
                    // wipe them so the next request to the Lifetime view
                    // recomputes against the new currency.
                    _state.update { it.copy(lifetimeLoaded = false, lifetimeMetrics = emptyList()) }
                    loadMetricsForMonth(_state.value.selectedMonth)
                    loadHistoricalData()
                }
            }
        }
    }

    fun refresh() {
        loadMetricsForMonth(_state.value.selectedMonth)
        loadHistoricalData()
    }

    fun onMonthSelected(month: MonthKey) {
        _state.update { it.copy(selectedMonth = month) }
        loadMetricsForMonth(month)
    }

    private fun loadMetricsForMonth(month: MonthKey) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val start = month.firstDay()
            val end = month.firstDayOfNextMonth()

            try {
                val analyticsResult = calculateMonthlyAnalyticsUseCase(start, end, baseCurrency)

                val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()

                _state.update {
                    it.copy(
                        transactionsForMonth = analyticsResult.transactions,
                        totalRevenuePlnForMonth = analyticsResult.totalRevenue,
                        topCategories = analyticsResult.topCategories,
                        exchangeRates = exchangeRates,
                        isLoading = false,
                        // First-emission lands here — turn off the cold-start
                        // shimmer. Subsequent month switches reuse this
                        // already-loaded state.
                        isInitialLoading = false
                    )
                }

                val previousMonth = month.previousMonth()
                val previousStart = previousMonth.firstDay()
                val previousEnd = previousMonth.firstDayOfNextMonth()
                val previousAnalytics = calculateMonthlyAnalyticsUseCase(previousStart, previousEnd, baseCurrency)
                val metrics = calculateAnalyticsMetricsUseCase(
                    currentRevenue = analyticsResult.totalRevenue,
                    currentExpenses = analyticsResult.totalExpenses,
                    currentTransactions = analyticsResult.transactions,
                    previousRevenue = previousAnalytics.totalRevenue,
                    previousExpenses = previousAnalytics.totalExpenses,
                    previousTransactions = previousAnalytics.transactions,
                    baseCurrency = baseCurrency,
                    exchangeRates = exchangeRates
                )
                _state.update { it.copy(metrics = metrics) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                analyticsTracker.recordException(e, "Analytics")
            }
        }
    }

    private fun loadHistoricalData() {
        viewModelScope.launch {
            // Anchor at [chartAnchorMonth] so a planned Sep tx (with today =
            // Aug) shifts the 6mo/1y window to end at Sep — the whole point
            // of the "show planned months in the graph" feature. When there's
            // no future data, this just returns today (== old behavior).
            val historicalData = loadHistoricalAnalyticsUseCase(
                anchorMonth = chartAnchorMonth(),
                baseCurrency = baseCurrency
            )
            _state.update { it.copy(historicalMetrics = historicalData) }
        }
    }

    fun loadLifetimeData() {
        if (_state.value.isLifetimeLoading || _state.value.lifetimeLoaded) return
        viewModelScope.launch {
            _state.update { it.copy(isLifetimeLoading = true) }
            try {
                val data = loadHistoricalAnalyticsUseCase(
                    anchorMonth = chartAnchorMonth(),
                    monthCount = LIFETIME_MONTHS,
                    baseCurrency = baseCurrency
                )
                _state.update {
                    it.copy(
                        lifetimeMetrics = data,
                        isLifetimeLoading = false,
                        lifetimeLoaded = true
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLifetimeLoading = false) }
                analyticsTracker.recordException(e, "Analytics", mapOf("action" to "load_lifetime"))
            }
        }
    }

    private companion object {
        const val LIFETIME_MONTHS = 60
    }

    fun onCategoryClicked(category: Category) {
        // Skip the intermediate subcategory sheet — open the transactions list
        // directly for this category (and all of its subcategories combined).
        // For categories without subcategories, this is now the only path and
        // the click is no longer a no-op.
        val transactions = _state.value.transactionsForMonth.filterNotNull()
        val matching = transactions.filter { tx ->
            tx.subcategory.id == category.id || tx.subcategory.parent?.id == category.id
        }
        _state.update {
            it.copy(
                isTransactionsSheetOpen = true,
                transactionsSheetCategory = category,
                transactionsForCategory = matching
            )
        }
    }

    fun onSubcategorySheetDismissed() {
        _state.update {
            it.copy(
                isSubcategorySheetOpen = false,
                selectedCategory = null,
                subcategories = emptyList()
            )
        }
    }

    /** Set (or clear with null) the monthly budget on a category from the
     *  transactions detail sheet. Sheet stays open — user might tweak the
     *  value and re-save. */
    fun setCategoryMonthlyLimit(categoryId: String, limit: Double?) {
        viewModelScope.launch {
            try {
                val existing = categoryDao.getById(categoryId) ?: return@launch
                if (existing.monthlyLimit == limit) return@launch
                categoryDao.upsert(existing.copy(monthlyLimit = limit))
                coreRepository.reloadCategories()
                // Refresh everything that renders Category refs so the limit
                // bar / budget label update immediately. Three surfaces read
                // categories: the main tab (topCategories), the breakdown
                // screen (sheetCategories), and the transactions sheet's
                // "Set a limit" button (transactionsSheetCategory). The
                // budget change is a rare user action, so re-running the
                // whole monthly analytics pass here is fine cost-wise.
                loadMetricsForMonth(_state.value.selectedMonth)
                // Refresh whichever breakdown the user was on. Prefer the
                // inline sheet's active type (categorySheetType) but fall
                // back to the last-loaded type — that's what covers the
                // full-screen AnalyticsBreakdownScreen path where the inline
                // sheet was never opened.
                val sheetTypeToReload = _state.value.categorySheetType ?: _state.value.lastLoadedSheetType
                sheetTypeToReload?.let { loadCategoriesForSheetType(it) }
                val fresh = coreRepository.getCategoryById(categoryId)
                if (fresh != null && _state.value.transactionsSheetCategory?.id == categoryId) {
                    _state.update { it.copy(transactionsSheetCategory = fresh) }
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort — sheet stays open, user can retry
            }
        }
    }

    fun onLeafCategoryClicked(category: Category) {
        val transactions = _state.value.transactionsForMonth.filterNotNull()
        val matching = transactions.filter { it.subcategory.id == category.id }
        _state.update {
            it.copy(
                isTransactionsSheetOpen = true,
                transactionsSheetCategory = category,
                transactionsForCategory = matching
            )
        }
    }

    fun onTransactionsSheetDismissed() {
        _state.update {
            it.copy(
                isTransactionsSheetOpen = false,
                transactionsSheetCategory = null,
                transactionsForCategory = emptyList()
            )
        }
    }

    fun onMetricCardClicked(metricTitle: String) {
        when (metricTitle) {
            "Net Income" -> {
                _state.update { it.copy(isNetIncomeSheetOpen = true) }
            }
            else -> {
                val sheetType = when (metricTitle) {
                    "Revenue" -> CategorySheetType.REVENUE
                    "Expenses" -> CategorySheetType.OPERATING_COSTS
                    "Taxes" -> CategorySheetType.TAXES
                    else -> return
                }

                _state.update {
                    it.copy(
                        categorySheetType = sheetType,
                        isCategorySheetOpen = true
                    )
                }
                loadCategoriesForSheetType(sheetType)
            }
        }
    }

    fun onCategorySheetDismissed() {
        _state.update {
            it.copy(
                isCategorySheetOpen = false,
                categorySheetType = null
            )
        }
    }

    fun onNetIncomeSheetDismissed() {
        _state.update { it.copy(isNetIncomeSheetOpen = false) }
    }

    fun loadCategoriesForSheetType(sheetType: CategorySheetType) {
        viewModelScope.launch {
            // Fetch fresh for the currently-selected month instead of leaning
            // on `transactionsForMonth` — that state slot is populated by
            // loadMetricsForMonth which races with this call whenever the
            // user changes the month on the breakdown screen. Stale reads
            // produced wrong spent/budget numbers per the user's bug.
            val month = _state.value.selectedMonth
            val analyticsResult = try {
                calculateMonthlyAnalyticsUseCase(
                    month.firstDay(),
                    month.firstDayOfNextMonth(),
                    baseCurrency
                )
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            } ?: return@launch
            val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
            val previousMonthTransactions = getPreviousMonthTransactionsUseCase(month)

            val categories = loadCategoriesForSheetTypeUseCase(
                sheetType = sheetType,
                currentTransactions = analyticsResult.transactions,
                previousMonthTransactions = previousMonthTransactions,
                baseCurrency = baseCurrency,
                exchangeRates = exchangeRates
            )

            _state.update { it.copy(sheetCategories = categories, lastLoadedSheetType = sheetType) }
        }
    }
}
