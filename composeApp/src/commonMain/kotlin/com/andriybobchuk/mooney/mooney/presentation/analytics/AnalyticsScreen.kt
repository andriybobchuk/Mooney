package com.andriybobchuk.mooney.mooney.presentation.analytics

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTimePeriod by remember { mutableStateOf(TimePeriod.SIX_MONTHS) }
    val hasAnyData = state.transactionsForMonth.filterNotNull().isNotEmpty() ||
        state.historicalMetrics.any { it.revenue > 0 || it.taxes > 0 || it.operatingCosts > 0 || it.netIncome != 0.0 }
    val isEmptyState = !hasAnyData && !state.isLoading

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(Color.Transparent),
        topBar = {
            Toolbars.Primary(
                containerColor = Color.Transparent,
                title = run {
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                    if (state.selectedMonth.year == now.year) {
                        "${state.selectedMonth.toDisplayString().substringBeforeLast(' ')} Analytics"
                    } else {
                        "${state.selectedMonth.toDisplayString()} Analytics"
                    }
                },
                scrollBehavior = scrollBehavior,
                actions = listOf(
                    Toolbars.ToolBarAction(
                        painter = com.andriybobchuk.mooney.core.presentation.Icons.SettingsIcon(),
                        contentDescription = stringResource(Res.string.settings),
                        onClick = onSettingsClick
                    )
                )
            )
        },
        bottomBar = { bottomNavbar() },
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
                            text = "Start adding transactions to see revenue, expenses, and financial insights.",
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

            if (!isEmpty) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(scrollState)
                    .fillMaxSize()
            ) {

                // Trend Chart
                TrendChart(
                    historicalData = state.historicalMetrics,
                    selectedMonth = state.selectedMonth,
                    onMonthSelected = viewModel::onMonthSelected,
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
                            onClick = { viewModel.onMetricCardClicked(metric.title) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
            } // end if (!isEmpty)

            // Category Bottom Sheet
            if (state.isCategorySheetOpen) {
                state.categorySheetType?.let { sheetType ->
                    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    
                    MooneyBottomSheet(
                        onDismissRequest = { viewModel.onCategorySheetDismissed() },
                        sheetState = bottomSheetState
                    ) {
                        CategoryBreakdownSheet(
                            sheetType = sheetType,
                            categories = state.sheetCategories,
                            historicalData = state.historicalMetrics,
                            onCategoryClick = { category ->
                                viewModel.onCategoryClicked(category)
                                // Don't dismiss category sheet - keep it open behind subcategory sheet
                            },
                            onDismiss = { viewModel.onCategorySheetDismissed() }
                        )
                    }
                }
            }
            
            // Subcategory Bottom Sheet
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
                            transactions = state.transactionsForCategory
                        )
                    }
                }
            }

            // Net Income Chart Bottom Sheet
            if (state.isNetIncomeSheetOpen) {
                val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                
                MooneyBottomSheet(
                    onDismissRequest = { viewModel.onNetIncomeSheetDismissed() },
                    sheetState = bottomSheetState
                ) {
                    NetIncomeChartBottomSheet(
                        historicalData = state.historicalMetrics,
                        selectedMonth = state.selectedMonth,
                        onDismiss = { viewModel.onNetIncomeSheetDismissed() }
                    )
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
                    text = metric.title,
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
                        text = it,
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
    transactions: List<com.andriybobchuk.mooney.mooney.domain.Transaction>
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = category.resolveEmoji(),
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            if (category.isSubCategory()) {
                Text(
                    text = category.parent?.title ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (transactions.isEmpty()) {
            Text(
                text = "No transactions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        } else {
            val grouped = transactions
                .groupBy { it.date }
                .entries
                .sortedByDescending { it.key }

            LazyColumn {
                grouped.forEach { (date, txs) ->
                    item {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp, horizontal = 14.dp)
                                .background(
                                    MaterialTheme.colorScheme.background,
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = date.formatForDisplay(),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(txs.sortedByDescending { it.id }) { transaction ->
                        com.andriybobchuk.mooney.mooney.presentation.transaction.TransactionItem(
                            transaction = transaction,
                            accounts = emptyList()
                        )
                    }
                }
            }
        }
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
    onDismiss: () -> Unit
) {
    val title = when (sheetType) {
        CategorySheetType.REVENUE -> stringResource(Res.string.revenue_breakdown)
        CategorySheetType.OPERATING_COSTS -> stringResource(Res.string.operating_costs_breakdown)
        CategorySheetType.TAXES -> stringResource(Res.string.tax_breakdown)
    }
    
    val emoji = when (sheetType) {
        CategorySheetType.REVENUE -> "💰"
        CategorySheetType.OPERATING_COSTS -> "💸"
        CategorySheetType.TAXES -> "📊"
    }
    
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
                    text = emoji,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = stringResource(Res.string.tap_category_subcategories),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
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
    onDismiss: () -> Unit
) {
    val netIncomeData = historicalData.takeLast(6) // Last 6 months
    
    // Calculate total profit and selected month profit for projection
    val totalProfit = netIncomeData.sumOf { it.netIncome }
    val selectedMonthProfit = historicalData.find { it.month == selectedMonth }?.netIncome ?: 0.0
    val projectedProfit = selectedMonthProfit * 6 // Projection based on selected month
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        // Header
        Text(
            text = stringResource(Res.string.profit_over_6_months),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Profit Summary Cards Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Last 6 months card
            Card(
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp, 
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.last_6_months),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "${totalProfit.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Next 6 months card  
            Card(
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.next_6_months),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "${projectedProfit.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = stringResource(Res.string.based_on_selected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
        
        // Net Income Chart
        NetIncomeChart(
            data = netIncomeData,
            modifier = Modifier.padding(bottom = 20.dp)
        )
    }
}

@Composable
fun NetIncomeChart(
    data: List<MonthlyMetricSnapshot>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Chart Canvas
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.TopCenter)
        ) {
            drawNetIncomeChart(data, size.width, size.height)
        }
        
        // Month Labels with Values
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { snapshot ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = snapshot.month.toShortDisplayString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = snapshot.netIncome.formatToShortString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        color = if (snapshot.netIncome >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNetIncomeChart(
    data: List<MonthlyMetricSnapshot>,
    width: Float,
    height: Float
) {
    if (data.size < 2) return

    val padding = 40f
    val chartWidth = width - (padding * 2)
    val chartHeight = height - (padding * 2)
    
    // Calculate data range for net income only
    val netIncomes = data.map { it.netIncome }
    val maxValue = netIncomes.maxOrNull() ?: 0.0
    val minValue = kotlin.math.min(0.0, netIncomes.minOrNull() ?: 0.0)
    val valueRange = maxValue - minValue
    
    if (valueRange == 0.0) return
    
    val netIncomeColor = Color(0xFF2196F3) // Blue color for net income
    val gridColor = Color.Gray.copy(alpha = 0.2f)
    
    // Draw grid lines
    for (i in 0..4) {
        val y = padding + (chartHeight * i / 4)
        drawLine(
            color = gridColor,
            start = androidx.compose.ui.geometry.Offset(padding, y),
            end = androidx.compose.ui.geometry.Offset(padding + chartWidth, y),
            strokeWidth = 1.dp.toPx()
        )
    }
    
    // Draw vertical grid lines  
    for (i in 0..5) {
        val x = padding + (chartWidth * i / 5)
        drawLine(
            color = gridColor,
            start = androidx.compose.ui.geometry.Offset(x, padding),
            end = androidx.compose.ui.geometry.Offset(x, padding + chartHeight),
            strokeWidth = 1.dp.toPx()
        )
    }
    
    // Draw bold zero line if zero is within range
    if (minValue <= 0.0 && maxValue >= 0.0) {
        val zeroY = padding + chartHeight - (chartHeight * ((0.0 - minValue) / (maxValue - minValue))).toFloat()
        drawLine(
            color = Color.Black.copy(alpha = 0.6f),
            start = androidx.compose.ui.geometry.Offset(padding, zeroY),
            end = androidx.compose.ui.geometry.Offset(padding + chartWidth, zeroY),
            strokeWidth = 2.dp.toPx()
        )
    }
    
    // Draw net income line
    val path = androidx.compose.ui.graphics.Path()
    val points = mutableListOf<androidx.compose.ui.geometry.Offset>()
    
    data.forEachIndexed { index, snapshot ->
        val value = snapshot.netIncome
        val x = padding + (chartWidth * index / (data.size - 1))
        val y = padding + chartHeight - (chartHeight * ((value - minValue) / (maxValue - minValue))).toFloat()
        
        points.add(androidx.compose.ui.geometry.Offset(x, y))
        
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    
    // Draw the line
    drawPath(
        path = path,
        color = netIncomeColor,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
    )
    
    // Draw data points
    points.forEach { point ->
        drawCircle(
            color = netIncomeColor,
            radius = 4.dp.toPx(),
            center = point
        )
    }
}
