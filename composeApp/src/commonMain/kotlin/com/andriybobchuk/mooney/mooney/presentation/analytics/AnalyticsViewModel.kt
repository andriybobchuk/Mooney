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
    val currentNetWorth: Double = 0.0
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
    private val coreRepository: com.andriybobchuk.mooney.mooney.domain.CoreRepository
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
            // Anchor the trailing-month window to today, NOT the selected month —
            // otherwise picking February would reframe the panel with February as
            // "most recent" until the app is restarted.
            val historicalData = loadHistoricalAnalyticsUseCase(
                currentMonth = MonthKey.current(),
                baseCurrency = baseCurrency
            )
            _state.update { it.copy(historicalMetrics = historicalData) }
            // Don't invalidate lifetime here — refresh() is called on every
            // resume, and a stale wipe race-condition'd with loadLifetimeData()
            // left the chart blank after switching tabs. Currency changes do
            // their own invalidation in observeBaseCurrency().
        }
    }

    fun loadLifetimeData() {
        if (_state.value.isLifetimeLoading || _state.value.lifetimeLoaded) return
        viewModelScope.launch {
            _state.update { it.copy(isLifetimeLoading = true) }
            try {
                val data = loadHistoricalAnalyticsUseCase(
                    currentMonth = MonthKey.current(),
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
                // Refresh the currently-open breakdown so the row shows the
                // new budget bar immediately.
                _state.value.categorySheetType?.let { sheetType ->
                    loadCategoriesForSheetType(sheetType)
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

            _state.update { it.copy(sheetCategories = categories) }
        }
    }
}
