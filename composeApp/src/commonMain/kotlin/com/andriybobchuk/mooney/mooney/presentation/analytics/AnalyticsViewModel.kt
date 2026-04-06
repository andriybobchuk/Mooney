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
    val sheetCategories: List<TopCategorySummary> = emptyList(),
    val isTransactionsSheetOpen: Boolean = false,
    val transactionsSheetCategory: Category? = null,
    val transactionsForCategory: List<Transaction> = emptyList()
)

class AnalyticsViewModel(
    private val calculateMonthlyAnalyticsUseCase: CalculateMonthlyAnalyticsUseCase,
    private val calculateSubcategoriesUseCase: CalculateSubcategoriesUseCase,
    private val currencyManagerUseCase: CurrencyManagerUseCase,
    private val calculateAnalyticsMetricsUseCase: CalculateAnalyticsMetricsUseCase,
    private val loadHistoricalAnalyticsUseCase: LoadHistoricalAnalyticsUseCase,
    private val loadCategoriesForSheetTypeUseCase: LoadCategoriesForSheetTypeUseCase,
    private val getPreviousMonthTransactionsUseCase: GetPreviousMonthTransactionsUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {
    private val baseCurrency: Currency = GlobalConfig.baseCurrency

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state

    init {
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

                _state.update {
                    it.copy(
                        transactionsForMonth = analyticsResult.transactions,
                        totalRevenuePlnForMonth = analyticsResult.totalRevenue,
                        topCategories = analyticsResult.topCategories,
                        isLoading = false
                    )
                }

                val previousMonth = month.previousMonth()
                val previousStart = previousMonth.firstDay()
                val previousEnd = previousMonth.firstDayOfNextMonth()
                val previousAnalytics = calculateMonthlyAnalyticsUseCase(previousStart, previousEnd, baseCurrency)

                val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
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
            val historicalData = loadHistoricalAnalyticsUseCase(
                currentMonth = _state.value.selectedMonth,
                baseCurrency = baseCurrency
            )
            _state.update { it.copy(historicalMetrics = historicalData) }
        }
    }

    fun onCategoryClicked(category: Category) {
        viewModelScope.launch {
            val transactions = _state.value.transactionsForMonth.filterNotNull()
            val previousTransactions = getPreviousMonthTransactionsUseCase(_state.value.selectedMonth)

            val subcategories = calculateSubcategoriesUseCase(category, transactions, baseCurrency, previousTransactions)

            val hasRealSubcategories = subcategories.size > 1 ||
                (subcategories.size == 1 && subcategories.first().category != category)

            if (hasRealSubcategories) {
                _state.update {
                    it.copy(
                        selectedCategory = category,
                        subcategories = subcategories,
                        isSubcategorySheetOpen = true
                    )
                }
            }
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
                    "Operating Costs" -> CategorySheetType.OPERATING_COSTS
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
            val currentTransactions = _state.value.transactionsForMonth.filterNotNull()
            val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
            val previousMonthTransactions = getPreviousMonthTransactionsUseCase(_state.value.selectedMonth)

            val categories = loadCategoriesForSheetTypeUseCase(
                sheetType = sheetType,
                currentTransactions = currentTransactions,
                previousMonthTransactions = previousMonthTransactions,
                baseCurrency = baseCurrency,
                exchangeRates = exchangeRates
            )

            _state.update { it.copy(sheetCategories = categories) }
        }
    }
}
