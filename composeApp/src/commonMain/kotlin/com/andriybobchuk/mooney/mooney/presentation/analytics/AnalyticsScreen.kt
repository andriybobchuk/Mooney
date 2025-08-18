package com.andriybobchuk.mooney.mooney.presentation.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.primary),
        topBar = {
            Toolbars.Primary(
                title = "Analytics",
                scrollBehavior = scrollBehavior,
                customContent = {
                    MonthPicker(
                        selectedMonth = state.selectedMonth,
                        onMonthSelected = viewModel::onMonthSelected,
                    )
                }
            )
        },
        bottomBar = { bottomNavbar() },
        content = { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).background(MaterialTheme.colorScheme.primary)) {

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.metrics) { metric ->
                        MetricCard(metric)
                    }
                }


                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn {
                        items(state.topCategories) { category ->
                            CategoryItem(
                                topCategorySummary = category,
                                onClick = { viewModel.onCategoryClicked(category.category) }
                            )
                        }
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
            Text(
                topCategorySummary.percentOfRevenue + "%",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
//            if (transaction.exchangeRate != null) {
//                Text(
//                    "*${transaction.exchangeRate.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}",
//                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
//                )
//            }
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
fun MetricCard(metric: AnalyticsMetric) {
    Column(
        modifier = Modifier
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = metric.title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = metric.value,
            style = MaterialTheme.typography.titleMedium
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
