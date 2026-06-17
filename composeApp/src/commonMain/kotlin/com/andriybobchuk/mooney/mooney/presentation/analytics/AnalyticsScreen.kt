package com.andriybobchuk.mooney.mooney.presentation.analytics

import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.andriybobchuk.mooney.app.appColors
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MeshGradientBackground
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyButton
import com.andriybobchuk.mooney.core.presentation.designsystem.components.ButtonVariant
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.AnalyticsMetric
import com.andriybobchuk.mooney.mooney.domain.CategorySheetType
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.MonthlyMetricSnapshot
import com.andriybobchuk.mooney.mooney.domain.TopCategorySummary
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import com.andriybobchuk.mooney.mooney.domain.formatToShortString
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.andriybobchuk.mooney.mooney.presentation.transaction.formatForDisplay
import org.jetbrains.compose.resources.stringResource
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = koinViewModel(),
    bottomNavbar: @Composable () -> Unit,
    onSettingsClick: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToBreakdown: (String) -> Unit = {},
    onNavigateToNetIncome: () -> Unit = {},
    onNavigateToNetWorth: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAnalyticsRequestSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    // Refresh data each time the screen appears
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    var selectedTimePeriod by remember { mutableStateOf(TimePeriod.SIX_MONTHS) }

    LaunchedEffect(selectedTimePeriod) {
        if (selectedTimePeriod == TimePeriod.LIFETIME) viewModel.loadLifetimeData()
    }

    // Auto-bump the chart window when the user picks a month outside the
    // currently visible range. We only upgrade (6mo → 1y → Lifetime), never
    // downgrade — the user can always manually pull the period back down via
    // the in-chart period chips if they want a narrower view.
    LaunchedEffect(state.selectedMonth) {
        val now = MonthKey.current()
        val monthsAgo = (now.year - state.selectedMonth.year) * 12 +
            (now.month - state.selectedMonth.month)
        when {
            monthsAgo >= TimePeriod.ONE_YEAR.months &&
                selectedTimePeriod != TimePeriod.LIFETIME -> {
                selectedTimePeriod = TimePeriod.LIFETIME
            }
            monthsAgo >= TimePeriod.SIX_MONTHS.months &&
                selectedTimePeriod == TimePeriod.SIX_MONTHS -> {
                selectedTimePeriod = TimePeriod.ONE_YEAR
            }
        }
    }

    // Counts per month for the picker sheet caption. Built from whichever
    // dataset is richer — lifetime data when it's loaded covers the full
    // history; otherwise fall back to the 6-month historical window.
    val monthlyCounts = remember(state.historicalMetrics, state.lifetimeMetrics) {
        val source = if (state.lifetimeMetrics.isNotEmpty()) state.lifetimeMetrics
        else state.historicalMetrics
        source.associate { it.month to it.transactionCount }
    }
    val hasAnyData = state.transactionsForMonth.filterNotNull().isNotEmpty() ||
        state.historicalMetrics.any { it.revenue > 0 || it.taxes > 0 || it.operatingCosts > 0 || it.netIncome != 0.0 }
    // Wrapped flag ensures the shimmer is actually visible on cold start
    // even when the underlying load races to completion within a frame.
    val showShimmer by com.andriybobchuk.mooney.core.presentation.rememberMinDisplayShimmer(state.isInitialLoading)
    // Don't show the "no analytics yet" empty state while the very first load
    // is in progress — otherwise the user sees the empty state for a frame
    // before the data populates. Once showShimmer flips to false, the empty
    // state is the right thing to render if there really is no data.
    val isEmptyState = !hasAnyData && !state.isLoading && !showShimmer

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(Color.Transparent),
        topBar = {
            Toolbars.Primary(
                containerColor = Color.Transparent,
                // Title is the plain word — the month is already shown inside
                // the selector pill sitting in customContent.
                title = stringResource(Res.string.analytics_title),
                scrollBehavior = scrollBehavior,
                customContent = {
                    com.andriybobchuk.mooney.mooney.presentation.components.MonthSelector(
                        selectedMonth = state.selectedMonth,
                        onMonthSelected = viewModel::onMonthSelected,
                        monthlyCounts = monthlyCounts
                    )
                },
                actions = emptyList()
            )
        },
        bottomBar = {
            androidx.compose.foundation.layout.Column {
                com.andriybobchuk.mooney.core.ads.AdBannerSlot(
                    placement = com.andriybobchuk.mooney.core.ads.AdPlacement.ANALYTICS_BREAKDOWN_BANNER
                )
                bottomNavbar()
            }
        },
        content = { paddingValues ->
            val isEmpty = isEmptyState

            if (isEmpty) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MeshGradientBackground()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = stringResource(Res.string.no_analytics_yet),
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.analytics_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        MooneyButton(
                            text = stringResource(Res.string.go_to_transactions),
                            onClick = onNavigateToTransactions,
                            variant = ButtonVariant.PRIMARY,
                            fullWidth = true
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            if (!isEmpty && showShimmer) {
                // True cold start: real metrics still computing. Render
                // placeholder cards so the user doesn't see a grid of zeros.
                AnalyticsScreenShimmer(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            if (!isEmpty && !showShimmer) {
            val analyticsScrollState = androidx.compose.foundation.rememberScrollState()
            val scrollToTopBus: com.andriybobchuk.mooney.app.ScrollToTopBus = org.koin.compose.koinInject()
            LaunchedEffect(scrollToTopBus) {
                scrollToTopBus.events.collect { tab ->
                    if (tab == com.andriybobchuk.mooney.app.ScrollToTopBus.Tab.ANALYTICS) {
                        analyticsScrollState.animateScrollTo(0)
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(analyticsScrollState)
            ) {

                // Trend Chart — month picking lives in the toolbar's MonthSelector.
                // The chart visualizes the trend and highlights the selected month
                // with a vertical accent line.
                TrendChart(
                    historicalData = state.historicalMetrics,
                    lifetimeData = state.lifetimeMetrics,
                    isLifetimeLoading = state.isLifetimeLoading,
                    selectedMonth = state.selectedMonth,
                    selectedPeriod = selectedTimePeriod,
                    onPeriodSelected = { selectedTimePeriod = it },
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 8.dp)
                )

                Column(
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    state.metrics.forEach { metric ->
                        EnhancedMetricCard(
                            metric = metric,
                            onClick = {
                                when (metric.title) {
                                    METRIC_NET_INCOME -> onNavigateToNetIncome()
                                    METRIC_REVENUE -> onNavigateToBreakdown("REVENUE")
                                    METRIC_EXPENSES -> onNavigateToBreakdown("OPERATING_COSTS")
                                    METRIC_TAXES -> onNavigateToBreakdown("TAXES")
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    // Net Worth card hidden for now — detail screen exists but
                    // entry point is gated until the underlying history math
                    // is fleshed out (current reconstruction relies on net
                    // income summation which is approximate). Re-enable by
                    // un-commenting the call below.
                    // NetWorthCard(
                    //     amount = state.currentNetWorth,
                    //     currency = GlobalConfig.baseCurrency,
                    //     onClick = onNavigateToNetWorth
                    // )
                }

                Spacer(modifier = Modifier.height(20.dp))

                AnalyticsRequestCard(
                    onClick = { showAnalyticsRequestSheet = true }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
            } // end if (!isEmpty)

            if (showAnalyticsRequestSheet) {
                com.andriybobchuk.mooney.core.feedback.FeedbackSheet(
                    onDismiss = { showAnalyticsRequestSheet = false }
                )
            }

            // Subcategory Bottom Sheet (used from breakdown screens)
            if (state.isSubcategorySheetOpen) {
                state.selectedCategory?.let { category ->
                    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                    MooneyBottomSheet(
                        onDismissRequest = { viewModel.onSubcategorySheetDismissed() },
                        sheetState = bottomSheetState
                    ) {
                        SubcategoryBottomSheet(
                            parentCategory = category,
                            subcategories = state.subcategories,
                            onSubcategoryClick = { leafCategory ->
                                viewModel.onLeafCategoryClicked(leafCategory)
                            },
                            onDismiss = { viewModel.onSubcategorySheetDismissed() }
                        )
                    }
                }
            }

            // Category Transactions Bottom Sheet
            if (state.isTransactionsSheetOpen) {
                state.transactionsSheetCategory?.let { category ->
                    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                    MooneyBottomSheet(
                        onDismissRequest = { viewModel.onTransactionsSheetDismissed() },
                        sheetState = bottomSheetState
                    ) {
                        CategoryTransactionsSheet(
                            category = category,
                            transactions = state.transactionsForCategory,
                            exchangeRates = state.exchangeRates,
                            onDismiss = { viewModel.onTransactionsSheetDismissed() }
                        )
                    }
                }
            }
        }
    )

}


@Composable
fun CategoryItem(
    topCategorySummary: TopCategorySummary,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 5.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.appColors.cardBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(topCategorySummary.category.resolveEmoji(), fontSize = 25.sp)
        }

        Spacer(Modifier.width(11.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                topCategorySummary.category.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal, fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (topCategorySummary.category.isSubCategory()) {
                Text(
                    topCategorySummary.category.parent?.title ?: "???",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${topCategorySummary.amount.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 14.5.sp),
                color = if (topCategorySummary.category.type == CategoryType.INCOME) MaterialTheme.appColors.incomeColor else MaterialTheme.appColors.expenseColor
            )
            
            // Trend pill
            if (topCategorySummary.trendPercentage != 0.0) {
                Spacer(modifier = Modifier.height(4.dp))
                
                val isPositive = topCategorySummary.trendPercentage > 0
                val trendColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
                val sign = if (isPositive) "+" else ""
                
                Box(
                    modifier = Modifier
                        .background(
                            trendColor.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$sign${kotlin.math.round(topCategorySummary.trendPercentage * 10) / 10}%",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = trendColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthPicker(
    selectedMonth: MonthKey,
    onMonthSelected: (MonthKey) -> Unit,
    monthRange: List<MonthKey> = generateRecentMonths(36)
) {
    var showSheet by remember { mutableStateOf(false) }

    IconButton(onClick = { showSheet = true }) {
        Icon(
            painter = com.andriybobchuk.mooney.core.presentation.Icons.CalendarIcon(),
            contentDescription = stringResource(Res.string.select_month),
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onBackground
        )
    }

    if (showSheet) {
        MooneyBottomSheet(
            onDismissRequest = { showSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.select_month),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Group months by year, sorted descending
                val years = monthRange.map { it.year }.distinct().sortedDescending()

                years.forEach { year ->
                    val monthsInYear = monthRange.filter { it.year == year }

                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )

                    monthsInYear.forEach { month ->
                        val isSelected = month == selectedMonth
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .clickable {
                                    onMonthSelected(month)
                                    showSheet = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = month.toDisplayString().substringBeforeLast(' '),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun EnhancedMetricCard(
    metric: AnalyticsMetric,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = metric.isClickable) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored circle indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color(metric.color))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Metric info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localizedMetricTitle(metric.title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                metric.subtitle?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = localizedMetricSubtitle(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Trend pill
            if (metric.trendPercentage != 0.0) {
                val isPositive = metric.trendPercentage > 0
                val trendColor = if (isPositive) Color(0xFF16A34A) else Color(0xFFDC2626)
                val pillBg = if (isPositive) Color(0xFF16A34A).copy(alpha = 0.10f) else Color(0xFFDC2626).copy(alpha = 0.10f)
                val sign = if (isPositive) "+" else ""
                val value = kotlin.math.round(metric.trendPercentage * 10) / 10

                Box(
                    modifier = Modifier
                        .background(pillBg, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$sign$value%",
                        style = MaterialTheme.typography.labelSmall,
                        color = trendColor
                    )
                }
            }
        }
    }
}


@Composable
fun SubcategoryBottomSheet(
    parentCategory: com.andriybobchuk.mooney.mooney.domain.Category,
    subcategories: List<TopCategorySummary>,
    onSubcategoryClick: (com.andriybobchuk.mooney.mooney.domain.Category) -> Unit = {},
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = parentCategory.resolveEmoji(),
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = parentCategory.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = stringResource(Res.string.subcategories_breakdown),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Subcategories list
        LazyColumn {
            items(subcategories) { subcategory ->
                CategoryItem(
                    topCategorySummary = subcategory,
                    onClick = { onSubcategoryClick(subcategory.category) }
                )
            }
        }
    }
}

@Composable
fun CategoryTransactionsSheet(
    category: com.andriybobchuk.mooney.mooney.domain.Category,
    transactions: List<com.andriybobchuk.mooney.mooney.domain.Transaction>,
    exchangeRates: com.andriybobchuk.mooney.mooney.domain.ExchangeRates,
    onDismiss: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Header with close button — sheet covers most of the screen, so the
        // explicit close gives users a clear way out (swipe-down isn't always
        // discoverable, especially on iOS).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.resolveEmoji(),
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
                if (category.isSubCategory()) {
                    Text(
                        text = category.parent?.title ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            androidx.compose.material3.IconButton(onClick = onDismiss) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (transactions.isEmpty()) {
            Text(
                text = stringResource(Res.string.no_transactions_short),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            return@Column
        }

        // Group by subcategory ID. Each group gets a header with name + count +
        // total; tapping expands it to reveal that group's transactions sorted
        // by date.
        // Convert every transaction's amount to the base currency before
        // summing — otherwise a USD salary of 6300 + a PLN salary of 100 sums
        // to 6400 and gets rendered with the base-currency symbol, which is
        // visibly wrong.
        val baseCurrency = GlobalConfig.baseCurrency
        val groups: List<Triple<com.andriybobchuk.mooney.mooney.domain.Category, Double, List<com.andriybobchuk.mooney.mooney.domain.Transaction>>> =
            transactions
                .groupBy { it.subcategory.id }
                .map { (_, txs) ->
                    val first = txs.first().subcategory
                    val totalInBase = txs.sumOf {
                        exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
                    }
                    Triple(first, totalInBase, txs.sortedByDescending { it.date })
                }
                .sortedByDescending { it.second }

        // Collapsed by default — except when the category has no subcategory
        // breakdown (single group), where collapsing the only group would just
        // hide everything for no reason.
        val defaultExpanded = groups.size <= 1
        val expanded = remember(category.id) {
            androidx.compose.runtime.mutableStateMapOf<String, Boolean>().apply {
                groups.forEach { (cat, _, _) -> put(cat.id, defaultExpanded) }
            }
        }

        LazyColumn {
            groups.forEach { (subcat, total, txs) ->
                val isExpanded = expanded[subcat.id] ?: defaultExpanded
                item("header_${subcat.id}") {
                    SubcategoryGroupHeader(
                        subcategory = subcat,
                        count = txs.size,
                        total = total,
                        expanded = isExpanded,
                        onToggle = { expanded[subcat.id] = !isExpanded }
                    )
                }
                if (isExpanded) {
                    // Group this subcategory's transactions by day so the user
                    // can scan "what happened on March 14" at a glance, with
                    // the description as the differentiator inside each row.
                    val byDay: List<Pair<kotlinx.datetime.LocalDate, List<com.andriybobchuk.mooney.mooney.domain.Transaction>>> =
                        txs.groupBy { it.date }.toList().sortedByDescending { (day, _) -> day }
                    byDay.forEach { (day, dayTxs) ->
                        item("day_${subcat.id}_$day") {
                            DayHeaderRow(date = day)
                        }
                        items(items = dayTxs, key = { tx -> "tx_${subcat.id}_${tx.id}" }) { transaction ->
                            TxDrilldownRow(transaction = transaction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubcategoryGroupHeader(
    subcategory: com.andriybobchuk.mooney.mooney.domain.Category,
    count: Int,
    total: Double,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = subcategory.resolveEmoji(),
            fontSize = 18.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subcategory.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (count == 1) "1 transaction" else "$count transactions",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${total.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (subcategory.type == com.andriybobchuk.mooney.mooney.domain.CategoryType.INCOME)
                MaterialTheme.appColors.incomeColor
            else
                MaterialTheme.appColors.expenseColor,
            modifier = Modifier.padding(end = 8.dp)
        )
        androidx.compose.material3.Icon(
            imageVector = if (expanded)
                androidx.compose.material.icons.Icons.Filled.KeyboardArrowUp
            else
                androidx.compose.material.icons.Icons.Filled.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun HorizontalMonthSelector(
    selectedMonth: MonthKey,
    onMonthSelected: (MonthKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthRange = generateRecentMonths(6).reversed() // Newest on the right
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(2.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            monthRange.forEach { month ->
                val isSelected = month == selectedMonth
                
                Button(
                    onClick = { onMonthSelected(month) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.appColors.cardBackground else Color.Transparent,
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = month.toShortDisplayString(),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    )
                }
                
                if (month != monthRange.last()) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

fun generateRecentMonths(count: Int): List<MonthKey> {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val months = mutableListOf<MonthKey>()
    var year = now.year
    var month = now.monthNumber

    repeat(count) {
        months.add(MonthKey(year, month))
        month--
        if (month == 0) {
            month = 12
            year--
        }
    }

    return months
}


@Composable
fun CategoryBreakdownSheet(
    sheetType: CategorySheetType,
    categories: List<TopCategorySummary>,
    historicalData: List<MonthlyMetricSnapshot> = emptyList(),
    onCategoryClick: (com.andriybobchuk.mooney.mooney.domain.Category) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        
        // 6-month trend mini chart
        if (historicalData.isNotEmpty()) {
            val chartData = historicalData.takeLast(6)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val values = chartData.map { snapshot ->
                        when (sheetType) {
                            CategorySheetType.REVENUE -> snapshot.revenue
                            CategorySheetType.TAXES -> snapshot.taxes
                            CategorySheetType.OPERATING_COSTS -> snapshot.operatingCosts
                        }
                    }
                    val maxVal = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

                    chartData.forEachIndexed { index, snapshot ->
                        val value = values[index]
                        val barHeight = ((value / maxVal) * 80).dp.coerceAtLeast(4.dp)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(barHeight)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = snapshot.month.toShortDisplayString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp
                            )
                            Text(
                                text = when (sheetType) {
                                    CategorySheetType.REVENUE -> snapshot.revenue.formatToShortString()
                                    CategorySheetType.TAXES -> snapshot.taxes.formatToShortString()
                                    CategorySheetType.OPERATING_COSTS -> snapshot.operatingCosts.formatToShortString()
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }
        }

        // Categories list
        LazyColumn {
            items(categories) { category ->
                CategoryItem(
                    topCategorySummary = category,
                    onClick = { onCategoryClick(category.category) }
                )
            }
        }
    }
}

@Composable
fun NetIncomeChartBottomSheet(
    historicalData: List<MonthlyMetricSnapshot>,
    selectedMonth: MonthKey,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val netIncomeData = historicalData.takeLast(6)
    val selectedSnapshot = historicalData.find { it.month == selectedMonth }
    val selectedMonthProfit = selectedSnapshot?.netIncome ?: 0.0
    val selectedMonthRevenue = selectedSnapshot?.revenue ?: 0.0

    val totalProfit = netIncomeData.sumOf { it.netIncome }
    val totalRevenue = netIncomeData.sumOf { it.revenue }
    val avgProfit = if (netIncomeData.isNotEmpty()) totalProfit / netIncomeData.size else 0.0

    val bestMonth = netIncomeData.maxByOrNull { it.netIncome }
    val worstMonth = netIncomeData.minByOrNull { it.netIncome }
    val greenMonths = netIncomeData.count { it.netIncome > 0 }
    val saveRateCurrent = if (selectedMonthRevenue > 0) (selectedMonthProfit / selectedMonthRevenue) * 100 else null
    val saveRateAvg = if (totalRevenue > 0) (totalProfit / totalRevenue) * 100 else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp)
    ) {
        NetIncomeHeroCard(
            value = selectedMonthProfit,
            saveRatePercent = saveRateCurrent,
            monthLabel = selectedMonth.toDisplayString()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Best / worst row.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NetIncomeStatCard(
                modifier = Modifier.weight(1f),
                label = stringResource(Res.string.best_month),
                value = bestMonth?.netIncome ?: 0.0,
                sublabel = bestMonth?.month?.toShortDisplayString()
            )
            NetIncomeStatCard(
                modifier = Modifier.weight(1f),
                label = stringResource(Res.string.worst_month),
                value = worstMonth?.netIncome ?: 0.0,
                sublabel = worstMonth?.month?.toShortDisplayString()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 6-month totals row.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NetIncomeStatCard(
                modifier = Modifier.weight(1f),
                label = "6-month total",
                value = totalProfit,
                sublabel = null
            )
            NetIncomeStatCard(
                modifier = Modifier.weight(1f),
                label = "6-month average",
                value = avgProfit,
                sublabel = null
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        NetIncomeInsightStrip(
            greenMonths = greenMonths,
            totalMonths = netIncomeData.size,
            avgSaveRate = saveRateAvg
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bar chart — color-coded positive/negative, far more readable for
        // net income than the previous line chart.
        NetIncomeBarChart(
            data = netIncomeData,
            selectedMonth = selectedMonth,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

/**
 * Hero card — selected month's net income with a "save rate" badge. The badge
 * is the most actionable number on the screen: "you kept X% of what you earned".
 */
@Composable
private fun NetIncomeHeroCard(
    value: Double,
    saveRatePercent: Double?,
    monthLabel: String
) {
    val accent = if (value >= 0.0) MaterialTheme.appColors.incomeColor else MaterialTheme.appColors.expenseColor
    com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyCard(
        modifier = Modifier.fillMaxWidth(),
        variant = com.andriybobchuk.mooney.core.presentation.designsystem.components.CardVariant.OUTLINED
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = monthLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${value.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                color = accent
            )
            if (saveRatePercent != null) {
                Spacer(modifier = Modifier.height(6.dp))
                val badgeText = when {
                    saveRatePercent >= 0 -> "${saveRatePercent.toInt()}% saved of income"
                    else -> "${(-saveRatePercent).toInt()}% over income"
                }
                val badgeColor = if (saveRatePercent >= 0)
                    MaterialTheme.appColors.incomeColor
                else
                    MaterialTheme.appColors.expenseColor
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = badgeColor
                    )
                }
            }
        }
    }
}

/**
 * Small horizontal strip that summarizes two non-obvious insights:
 * how many of the last N months you finished in the green, and your
 * average save rate. Designed to fit on one line of compact metadata.
 */
@Composable
private fun NetIncomeInsightStrip(
    greenMonths: Int,
    totalMonths: Int,
    avgSaveRate: Double?
) {
    if (totalMonths == 0) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(Res.string.in_the_green),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$greenMonths of $totalMonths months",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (avgSaveRate != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(Res.string.avg_save_rate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val rateColor = if (avgSaveRate >= 0)
                    MaterialTheme.appColors.incomeColor
                else
                    MaterialTheme.appColors.expenseColor
                Text(
                    text = "${avgSaveRate.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = rateColor
                )
            }
        }
    }
}

@Composable
private fun NetIncomeStatCard(
    modifier: Modifier,
    label: String,
    value: Double,
    sublabel: String? = null
) {
    val accent = if (value >= 0.0) MaterialTheme.appColors.incomeColor else MaterialTheme.appColors.expenseColor
    com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyCard(
        modifier = modifier,
        variant = com.andriybobchuk.mooney.core.presentation.designsystem.components.CardVariant.OUTLINED
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${value.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Color-coded bar chart for net income across recent months. Green bars for
 * positive months, red for negative — a glance tells the story. The bar for
 * the currently selected month gets a subtle accent ring to anchor the user.
 */
@Composable
private fun NetIncomeBarChart(
    data: List<MonthlyMetricSnapshot>,
    selectedMonth: MonthKey,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val incomeColor = MaterialTheme.appColors.incomeColor
    val expenseColor = MaterialTheme.appColors.expenseColor
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    // Zero line needs to read as a clear baseline against bars in either
    // theme. In dark mode the chart sits on a dark surface and `onSurface`
    // at 0.45 alpha washed out to faint gray — bump it and stroke harder
    // in dark mode specifically.
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val zeroLineColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (isDark) 0.75f else 0.55f
    )
    val zeroLineWidth = if (isDark) 2.dp else 1.5.dp

    Column(modifier = modifier.fillMaxWidth()) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val maxValue = data.maxOf { it.netIncome }
            val minValue = kotlin.math.min(0.0, data.minOf { it.netIncome })
            val range = (maxValue - minValue).takeIf { it > 0 } ?: return@Canvas

            val padding = 8f
            val w = size.width - padding * 2
            val h = size.height - padding * 2
            val zeroY = padding + h - (h * ((0.0 - minValue) / range)).toFloat()
            val slotWidth = w / data.size

            // Subtle horizontal grid (zero line gets a stronger stroke).
            for (i in 0..3) {
                val y = padding + (h * i / 3)
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(padding, y),
                    end = androidx.compose.ui.geometry.Offset(padding + w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            drawLine(
                color = zeroLineColor,
                start = androidx.compose.ui.geometry.Offset(padding, zeroY),
                end = androidx.compose.ui.geometry.Offset(padding + w, zeroY),
                strokeWidth = zeroLineWidth.toPx()
            )

            // Bars
            data.forEachIndexed { index, snapshot ->
                val barLeft = padding + slotWidth * index + slotWidth * 0.15f
                val barWidth = slotWidth * 0.7f
                val value = snapshot.netIncome
                val topY = padding + h - (h * ((value - minValue) / range)).toFloat()
                val barTop = kotlin.math.min(topY, zeroY)
                val barBottom = kotlin.math.max(topY, zeroY)
                val color = if (value >= 0) incomeColor else expenseColor

                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(barLeft, barTop),
                    size = androidx.compose.ui.geometry.Size(barWidth, barBottom - barTop),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Highlight ring for the selected month.
                if (snapshot.month == selectedMonth) {
                    drawRoundRect(
                        color = color.copy(alpha = 0.35f),
                        topLeft = androidx.compose.ui.geometry.Offset(barLeft - 3, barTop - 3),
                        size = androidx.compose.ui.geometry.Size(barWidth + 6, (barBottom - barTop) + 6),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { snapshot ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = snapshot.month.toShortDisplayString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Text(
                        text = snapshot.netIncome.formatToShortString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        fontSize = 10.sp,
                        color = if (snapshot.netIncome >= 0) incomeColor else expenseColor
                    )
                }
            }
        }
    }
}

/**
 * Cold-start placeholder for the Analytics screen. Same alpha animation as
 * Assets/Transactions shimmers so the loading feel is consistent across the
 * app. Shows the rough layout: trend chart band + 3 metric card placeholders.
 */
@Composable
private fun AnalyticsScreenShimmer(modifier: Modifier = Modifier) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "analyticsShimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "analyticsShimmerAlpha"
    )
    val barColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = 0.08f * (alpha * 2f).coerceAtMost(1f)
    )
    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Trend chart band placeholder.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(barColor)
        )

        // Metric card placeholders (revenue / expenses / net income).
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun NetWorthCard(
    amount: Double,
    currency: com.andriybobchuk.mooney.mooney.domain.Currency,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // No colored dot — net worth sits on its own axis and would be
            // misleading to imply it shares the chart with income/expense.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.net_worth_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${amount.formatWithCommas()} ${currency.symbol}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(Res.string.tap_to_view_history),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painter = com.andriybobchuk.mooney.core.presentation.Icons.ChevronRightIcon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

internal const val METRIC_NET_INCOME = "Net Income"
internal const val METRIC_REVENUE = "Revenue"
internal const val METRIC_EXPENSES = "Expenses"
internal const val METRIC_TAXES = "Taxes"

@Composable
private fun localizedMetricTitle(rawTitle: String): String = when (rawTitle) {
    METRIC_NET_INCOME -> stringResource(Res.string.net_income_title)
    METRIC_REVENUE -> stringResource(Res.string.revenue)
    METRIC_EXPENSES -> stringResource(Res.string.operating_costs)
    METRIC_TAXES -> stringResource(Res.string.taxes)
    else -> rawTitle
}

@Composable
private fun localizedMetricSubtitle(rawSubtitle: String): String {
    val pctOfRevenuePrefix = "pct_of_revenue:"
    return if (rawSubtitle.startsWith(pctOfRevenuePrefix)) {
        stringResource(Res.string.pct_of_revenue, rawSubtitle.removePrefix(pctOfRevenuePrefix))
    } else {
        rawSubtitle
    }
}


@Composable
private fun AnalyticsRequestCard(onClick: () -> Unit) {
    // Same shape, surface tint, padding and inner layout as EnhancedMetricCard
    // so it visually slots in alongside the Revenue / Expenses / Net Income
    // cards instead of looking like an afterthought.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored circle indicator — keeps the visual rhythm with the
            // metric cards above; uses primary so the row reads as an
            // action prompt rather than just another data point.
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.analytics_request_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(Res.string.analytics_request_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun DayHeaderRow(date: kotlinx.datetime.LocalDate) {
    val day = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    Text(
        text = "$day · ${date.dayOfMonth} ${date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun TxDrilldownRow(transaction: com.andriybobchuk.mooney.mooney.domain.Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val description = transaction.description?.takeIf { it.isNotBlank() }
            Text(
                text = description ?: transaction.subcategory.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = transaction.account.title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${transaction.amount.formatWithCommas()} ${transaction.account.currency.symbol}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
