package com.andriybobchuk.mooney.mooney.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.TopCategorySummary
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.usecase.*
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import com.andriybobchuk.mooney.mooney.data.CategoryDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.ui.graphics.Color

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
    val sheetCategories: List<TopCategorySummary> = emptyList()
)

class AnalyticsViewModel(
    private val calculateMonthlyAnalyticsUseCase: CalculateMonthlyAnalyticsUseCase,
    private val calculateSubcategoriesUseCase: CalculateSubcategoriesUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val currencyManagerUseCase: CurrencyManagerUseCase
) : ViewModel() {
    private val baseCurrency: Currency = GlobalConfig.baseCurrency

    private fun getExchangeRates(): ExchangeRates = currencyManagerUseCase.getCurrentExchangeRates()

    private val calculators: List<AnalyticsMetricCalculator>
        get() {
            val rates = getExchangeRates()
            return listOf(
                RevenueCalculator(rates),
                TaxesCalculator(rates),
                OperatingCostsCalculator(rates),
                NetIncomeCalculator(rates),
            )
        }

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
                
                // Calculate metrics with trend data
                val previousMonth = month.previousMonth()
                val previousStart = previousMonth.firstDay()
                val previousEnd = previousMonth.firstDayOfNextMonth()
                val previousAnalytics = calculateMonthlyAnalyticsUseCase(previousStart, previousEnd, baseCurrency)
                
                val metrics = calculators.mapIndexed { index, calculator -> 
                    val currentValue = when (index) {
                        0 -> analyticsResult.totalRevenue
                        1 -> calculateTaxes(analyticsResult.transactions)
                        2 -> analyticsResult.totalExpenses
                        3 -> analyticsResult.totalRevenue - calculateTaxes(analyticsResult.transactions) - analyticsResult.totalExpenses
                        else -> 0.0
                    }
                    
                    val previousValue = when (index) {
                        0 -> previousAnalytics.totalRevenue
                        1 -> calculateTaxes(previousAnalytics.transactions)
                        2 -> previousAnalytics.totalExpenses
                        3 -> previousAnalytics.totalRevenue - calculateTaxes(previousAnalytics.transactions) - previousAnalytics.totalExpenses
                        else -> 0.0
                    }
                    
                    val baseMetric = calculator.calculate(analyticsResult.totalRevenue, analyticsResult.transactions, month, baseCurrency)
                    val trendPercentage = if (previousValue != 0.0) ((currentValue - previousValue) / previousValue) * 100 else 0.0
                    
                    baseMetric.copy(
                        trendPercentage = trendPercentage,
                        isClickable = index in listOf(0, 1, 2, 3) // Revenue, Taxes, Operating Costs, Net Income
                    )
                }
                _state.update { it.copy(metrics = metrics) }
                
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
    
    private fun calculateTaxes(transactions: List<Transaction>): Double {
        return transactions.sumConverted(baseCurrency, getExchangeRates()) {
            it.subcategory.title.contains("ZUS", ignoreCase = true) ||
            it.subcategory.title.contains("PIT", ignoreCase = true)
        }
    }
    
    private fun loadHistoricalData() {
        viewModelScope.launch {
            val historicalData = mutableListOf<MonthlyMetricSnapshot>()
            val currentMonth = _state.value.selectedMonth
            
            // Load 12 months of historical data to support 1-year view
            repeat(12) { offset ->
                val month = currentMonth.monthsAgo(offset)
                val start = month.firstDay()
                val end = month.firstDayOfNextMonth()
                
                try {
                    val analytics = calculateMonthlyAnalyticsUseCase(start, end, baseCurrency)
                    val taxes = calculateTaxes(analytics.transactions)
                    
                    historicalData.add(
                        MonthlyMetricSnapshot(
                            month = month,
                            revenue = analytics.totalRevenue,
                            taxes = taxes,
                            operatingCosts = analytics.totalExpenses,
                            netIncome = analytics.totalRevenue - taxes - analytics.totalExpenses,
                            transactionCount = analytics.transactions.filterNotNull().size
                        )
                    )
                } catch (e: Exception) {
                    // Skip this month if data unavailable
                }
            }
            
            _state.update { it.copy(historicalMetrics = historicalData.reversed()) }
        }
    }
    
    fun onCategoryClicked(category: Category) {
        viewModelScope.launch {
            val transactions = _state.value.transactionsForMonth.filterNotNull()

            val previousMonth = _state.value.selectedMonth.previousMonth()
            val previousStart = previousMonth.firstDay()
            val previousEnd = previousMonth.firstDayOfNextMonth()

            val previousTransactions = try {
                getTransactionsUseCase().first().filterNotNull().filter { transaction ->
                    transaction.date >= previousStart && transaction.date < previousEnd
                }
            } catch (e: Exception) {
                emptyList<Transaction>()
            }

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
    
    fun onMetricCardClicked(metricTitle: String) {
        when (metricTitle) {
            "Net Income" -> {
                _state.update {
                    it.copy(isNetIncomeSheetOpen = true)
                }
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
        _state.update {
            it.copy(isNetIncomeSheetOpen = false)
        }
    }
    
    fun loadCategoriesForSheetType(sheetType: CategorySheetType) {
        viewModelScope.launch {
            val currentTransactions = _state.value.transactionsForMonth.filterNotNull()
            val exchangeRates = getExchangeRates()

            val previousMonth = _state.value.selectedMonth.previousMonth()
            val previousStart = previousMonth.firstDay()
            val previousEnd = previousMonth.firstDayOfNextMonth()

            val previousMonthTransactions = try {
                getTransactionsUseCase().first().filterNotNull().filter { transaction ->
                    transaction.date >= previousStart && transaction.date < previousEnd
                }
            } catch (e: Exception) {
                emptyList<Transaction>()
            }

            val relevantCategories = when (sheetType) {
                CategorySheetType.REVENUE -> listOf(
                    CategoryDataSource.salary,
                    CategoryDataSource.tax_return,
                    CategoryDataSource.refund,
                    CategoryDataSource.repayment,
                    CategoryDataSource.positive_reconciliation
                )
                CategorySheetType.TAXES -> CategoryDataSource.taxSub
                CategorySheetType.OPERATING_COSTS -> {
                    CategoryDataSource.categories.filter { category ->
                        category.type == CategoryType.EXPENSE &&
                        category.parent == CategoryDataSource.expense &&
                        category != CategoryDataSource.tax
                    }
                }
            }

            val categories = relevantCategories.mapNotNull { category ->
                val categoryTransactions = currentTransactions.filter { transaction ->
                    transaction.subcategory == category || transaction.subcategory.parent == category
                }

                val currentAmount = categoryTransactions.sumOf {
                    exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
                }

                val previousCategoryTransactions = previousMonthTransactions.filter { transaction ->
                    transaction.subcategory == category || transaction.subcategory.parent == category
                }

                val previousAmount = previousCategoryTransactions.sumOf {
                    exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
                }

                val trendPercentage = if (previousAmount != 0.0) {
                    ((currentAmount - previousAmount) / previousAmount) * 100
                } else if (currentAmount > 0) {
                    100.0
                } else {
                    0.0
                }

                if (currentAmount > 0) {
                    TopCategorySummary(
                        category = category,
                        amount = currentAmount,
                        formatted = "${currentAmount.formatWithCommas()} ${baseCurrency.symbol}",
                        percentOfRevenue = "",
                        trendPercentage = trendPercentage
                    )
                } else {
                    null
                }
            }.sortedByDescending { it.amount }

            _state.update { it.copy(sheetCategories = categories) }
        }
    }
}


fun format(amount: Double, currency: Currency): String =
    "${amount.formatWithCommas()} ${currency.symbol}"


fun percentage(part: Double, total: Double): String =
    if (total == 0.0) "–" else (part / total * 100).formatWithCommas()

data class AnalyticsMetric(
    val title: String,
    val value: String,
    val subtitle: String? = null,
    val color: androidx.compose.ui.graphics.Color,
    val trendPercentage: Double = 0.0,
    val isClickable: Boolean = false
)

data class MonthlyMetricSnapshot(
    val month: MonthKey,
    val revenue: Double,
    val taxes: Double,
    val operatingCosts: Double,
    val netIncome: Double,
    val transactionCount: Int
)

enum class CategorySheetType {
    REVENUE, OPERATING_COSTS, TAXES
}

interface AnalyticsMetricCalculator {
    suspend fun calculate(
        revenue: Double,
        transactions: List<Transaction>,
        month: MonthKey,
        baseCurrency: Currency
    ): AnalyticsMetric
}

data class MonthKey(val year: Int, val month: Int) {
    fun toDisplayString(): String = "${monthName(month)} $year"
    
    fun toShortDisplayString(): String = shortMonthName(month)

    fun firstDay(): LocalDate = LocalDate(year, month, 1)

    fun firstDayOfNextMonth(): LocalDate {
        val nextMonth = if (month == 12) 1 else month + 1
        val nextYear = if (month == 12) year + 1 else year
        return LocalDate(nextYear, nextMonth, 1)
    }
    
    fun previousMonth(): MonthKey {
        val prevMonth = if (month == 1) 12 else month - 1
        val prevYear = if (month == 1) year - 1 else year
        return MonthKey(prevYear, prevMonth)
    }
    
    fun monthsAgo(count: Int): MonthKey {
        var result = this
        repeat(count) {
            result = result.previousMonth()
        }
        return result
    }

    private fun monthName(m: Int): String = when (m) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Invalid"
    }
    
    private fun shortMonthName(m: Int): String = when (m) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> "???"
    }

    companion object {
        fun current(): MonthKey {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            return MonthKey(now.year, now.monthNumber)
        }
    }
}

class RevenueCalculator(
    private val exchangeRates: ExchangeRates
) : AnalyticsMetricCalculator {
    override suspend fun calculate(
        revenue: Double,
        transactions: List<Transaction>,
        month: MonthKey,
        baseCurrency: Currency
    ): AnalyticsMetric {
        return AnalyticsMetric(
            title = "Revenue", 
            value = format(revenue, baseCurrency),
            color = Color(0xFF4CAF50) // Green
        )
    }
}

fun List<Transaction>.sumConverted(
    baseCurrency: Currency,
    exchangeRates: ExchangeRates,
    filter: (Transaction) -> Boolean
): Double {
    return this
        .asSequence()
        .filter(filter)
        .sumOf { exchangeRates.convert(it.amount, it.account.currency, baseCurrency) }
}

class TaxesCalculator(
    private val exchangeRates: ExchangeRates
) : AnalyticsMetricCalculator {
    override suspend fun calculate(
        revenue: Double,
        transactions: List<Transaction>,
        month: MonthKey,
        baseCurrency: Currency
    ): AnalyticsMetric {
        val taxes = transactions.sumConverted(baseCurrency, exchangeRates) {
            it.subcategory.title.contains("ZUS", ignoreCase = true) ||
                    it.subcategory.title.contains("PIT", ignoreCase = true)
        }

        val subtitle = if (revenue > 0) "${percentage(taxes, revenue)}% of revenue" else "–"
        return AnalyticsMetric(
            title = "Taxes", 
            value = format(taxes, baseCurrency), 
            subtitle = subtitle,
            color = Color(0xFFFF9800) // Orange
        )
    }
}

class OperatingCostsCalculator(
    private val exchangeRates: ExchangeRates
) : AnalyticsMetricCalculator {
    override suspend fun calculate(
        revenue: Double,
        transactions: List<Transaction>,
        month: MonthKey,
        baseCurrency: Currency
    ): AnalyticsMetric {
        val expenses = transactions.sumConverted(baseCurrency, exchangeRates) {
            it.subcategory.type == CategoryType.EXPENSE &&
                    !it.subcategory.title.contains("ZUS") &&
                    !it.subcategory.title.contains("PIT")
        }

        val subtitle = if (revenue > 0) "${percentage(expenses, revenue)}% of revenue" else "–"
        return AnalyticsMetric(
            title = "Operating Costs", 
            value = format(expenses, baseCurrency), 
            subtitle = subtitle,
            color = Color(0xFFF44336) // Red
        )
    }
}

class NetIncomeCalculator(
    private val exchangeRates: ExchangeRates
) : AnalyticsMetricCalculator {
    override suspend fun calculate(
        revenue: Double,
        transactions: List<Transaction>, month: MonthKey, baseCurrency: Currency
    ): AnalyticsMetric {
        val taxes = transactions.sumConverted(baseCurrency, exchangeRates) {
            it.subcategory.title.contains("ZUS") || it.subcategory.title.contains("PIT")
        }
        val expenses = transactions.sumConverted(baseCurrency, exchangeRates) {
            it.subcategory.type == CategoryType.EXPENSE &&
                    !it.subcategory.title.contains("ZUS") &&
                    !it.subcategory.title.contains("PIT")
        }

        val netIncome = revenue - taxes - expenses
        val subtitle = if (revenue > 0) "${percentage(netIncome, revenue)}% revenue" else "–"
        return AnalyticsMetric(
            title = "Net Income", 
            value = format(netIncome, baseCurrency), 
            subtitle = subtitle,
            color = Color(0xFF2196F3) // Blue
        )
    }
}
