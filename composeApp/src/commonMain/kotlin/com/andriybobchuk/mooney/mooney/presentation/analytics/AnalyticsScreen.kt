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
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.andriybobchuk.mooney.core.presentation.theme.appColors
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.presentation.formatWithCommas
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = koinViewModel(),
    bottomNavbar: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.primary),
        topBar = {
            Toolbars.Primary(
                title = "Analytics",
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = { bottomNavbar() },
        content = { paddingValues ->
            val scrollState = rememberScrollState()
            
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.primary)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(scrollState)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    // Trend Chart
                    TrendChart(
                        historicalData = state.historicalMetrics,
                        selectedMonth = state.selectedMonth,
                        onMonthSelected = viewModel::onMonthSelected,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)
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
            }
            
            // Category Bottom Sheet
            if (state.isCategorySheetOpen) {
                state.categorySheetType?.let { sheetType ->
                    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.onCategorySheetDismissed() },
                        sheetState = bottomSheetState,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        CategoryBreakdownSheet(
                            sheetType = sheetType,
                            categories = viewModel.getAllCategoriesForSheetType(sheetType),
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
                    
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.onSubcategorySheetDismissed() },
                        sheetState = bottomSheetState,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        SubcategoryBottomSheet(
                            parentCategory = category,
                            subcategories = state.subcategories,
                            onDismiss = { viewModel.onSubcategorySheetDismissed() }
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
            .padding(vertical = 6.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.appColors.cardBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(topCategorySummary.category.resolveEmoji(), fontSize = 25.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                topCategorySummary.category.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
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
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
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

@Composable
fun MonthPicker(
    selectedMonth: MonthKey,
    onMonthSelected: (MonthKey) -> Unit,
    monthRange: List<MonthKey> = generateRecentMonths(4)
) {
    var expanded by remember { mutableStateOf(false) }

    Button(
        onClick = { expanded = true },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(0.dp),
        elevation = null
    ) {
        Text(text = selectedMonth.toDisplayString())
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        monthRange.forEach { month ->
            DropdownMenuItem(
                text = { Text(month.toDisplayString()) },
                onClick = {
                    onMonthSelected(month)
                    expanded = false
                }
            )
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
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.appColors.cardBackground)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored circle indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(metric.color)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Metric info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metric.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
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
            
            // Trend arrow
            if (metric.trendPercentage != 0.0) {
                val isPositive = metric.trendPercentage > 0
                val trendColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
                val arrow = if (isPositive) "↗" else "↘"
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = arrow,
                        fontSize = 20.sp,
                        color = trendColor
                    )
                    Text(
                        text = "${if (isPositive) "+" else ""}${kotlin.math.round(metric.trendPercentage * 10) / 10}%",
                        style = MaterialTheme.typography.bodySmall,
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
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Subcategories breakdown",
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
                    onClick = { /* No action needed for subcategories */ }
                )
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
    onCategoryClick: (com.andriybobchuk.mooney.mooney.domain.Category) -> Unit,
    onDismiss: () -> Unit
) {
    val title = when (sheetType) {
        CategorySheetType.REVENUE -> "Revenue Breakdown"
        CategorySheetType.OPERATING_COSTS -> "Operating Costs Breakdown"
        CategorySheetType.TAXES -> "Tax Breakdown"
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
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Tap a category to see subcategories",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
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
