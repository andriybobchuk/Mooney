package com.andriybobchuk.mooney.mooney.presentation.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.mooney.domain.CategorySheetType
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsBreakdownScreen(
    viewModel: AnalyticsViewModel,
    type: String,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetType = CategorySheetType.valueOf(type)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(sheetType) {
        viewModel.loadCategoriesForSheetType(sheetType)
        // Month selector moved from the toolbar into the bar chart — the
        // chart therefore needs every captured month, not just the last
        // 12. Lifetime data is lazy-loaded + cached across the screen's
        // lifetime so re-entry is instant.
        viewModel.loadLifetimeData()
    }

    // Reuse the same MonthSelector as the main Analytics screen so the two
    // surfaces share vocabulary and users don't have to relearn the pattern.
    LaunchedEffect(state.selectedMonth, sheetType) {
        viewModel.loadCategoriesForSheetType(sheetType)
    }

    // Title = big spent-this-month number (adjusted for user exclusions).
    // Subtitle is "<MonthName> <SheetLabel>" (e.g. September Expenses) — // allow-hardcoded
    // month-name only (no year); the bar-chart row carries year context
    // for users scrolling back more than 12 months.
    val totalForSelectedMonth = state.sheetCategories
        .filter { it.category.id !in state.deselectedCategoryIds }
        .sumOf { it.amount }
    val titleAmount = "${totalForSelectedMonth.formatWithCommas()} ${com.andriybobchuk.mooney.mooney.data.GlobalConfig.baseCurrency.symbol}"
    val sheetLabel = when (sheetType) {
        CategorySheetType.REVENUE -> stringResource(Res.string.revenue)
        CategorySheetType.OPERATING_COSTS -> stringResource(Res.string.operating_costs)
        CategorySheetType.TAXES -> stringResource(Res.string.taxes)
    }
    val subtitle = "${state.selectedMonth.toMonthName()} $sheetLabel"

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(Color.Transparent),
        topBar = {
            // Dedicated title block — Toolbars.Primary's default
            // title/subtitle layout has a comfortable gap between them;
            // we tighten that to 0dp here so the amount and context line
            // read as one tight stack. Also drops the MonthSelector
            // chip from the toolbar's actions: month picking now lives
            // entirely in the bar chart underneath.
            Toolbars.Primary(
                titleContent = {
                    androidx.compose.foundation.layout.Column(
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(0.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = titleAmount,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 20.sp,
                            lineHeight = 22.sp
                        )
                        androidx.compose.material3.Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                        )
                    }
                },
                showBackButton = true,
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            com.andriybobchuk.mooney.core.ads.AdBannerSlot(
                placement = com.andriybobchuk.mooney.core.ads.AdPlacement.ANALYTICS_BREAKDOWN_BANNER
            )
        }
    ) { paddingValues ->
        // Prefer lifetime data when loaded (every historical month ever),
        // otherwise fall back to the 12-month window while lifetime is
        // still being computed. Drops empty leading months so the chart
        // doesn't scroll back through pre-first-transaction history.
        val chartSource = if (state.lifetimeMetrics.isNotEmpty()) {
            state.lifetimeMetrics.dropWhile {
                it.transactionCount == 0 && it.revenue == 0.0 &&
                    it.operatingCosts == 0.0 && it.taxes == 0.0
            }
        } else state.historicalMetrics

        CategoryBreakdownSheet(
            sheetType = sheetType,
            categories = state.sheetCategories,
            historicalData = chartSource,
            selectedMonth = state.selectedMonth,
            onMonthSelected = viewModel::onMonthSelected,
            onCategoryClick = { category -> viewModel.onCategoryClicked(category) },
            onDismiss = onBackClick,
            deselectedCategoryIds = state.deselectedCategoryIds,
            onToggleSelection = viewModel::toggleCategorySelection,
            monthlyDeductions = state.monthlyDeductions,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        )

        if (state.isSubcategorySheetOpen) {
            state.selectedCategory?.let { category ->
                MooneyBottomSheet(
                    onDismissRequest = { viewModel.onSubcategorySheetDismissed() },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {
                    SubcategoryBottomSheet(
                        parentCategory = category,
                        subcategories = state.subcategories,
                        onSubcategoryClick = { viewModel.onLeafCategoryClicked(it) },
                        onDismiss = { viewModel.onSubcategorySheetDismissed() },
                        selectedMonth = state.selectedMonth,
                        onMonthSelected = viewModel::onMonthSelected
                    )
                }
            }
        }

        if (state.isTransactionsSheetOpen) {
            state.transactionsSheetCategory?.let { category ->
                MooneyBottomSheet(
                    onDismissRequest = { viewModel.onTransactionsSheetDismissed() },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {
                    CategoryTransactionsSheet(
                        category = category,
                        transactions = state.transactionsForCategory,
                        exchangeRates = state.exchangeRates,
                        onDismiss = { viewModel.onTransactionsSheetDismissed() },
                        onSetLimit = { limit ->
                            viewModel.setCategoryMonthlyLimit(category.id, limit)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsNetIncomeScreen(
    viewModel: AnalyticsViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(Color.Transparent),
        topBar = {
            Toolbars.Primary(
                title = stringResource(Res.string.net_income_title),
                showBackButton = true,
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            com.andriybobchuk.mooney.core.ads.AdBannerSlot(
                placement = com.andriybobchuk.mooney.core.ads.AdPlacement.ANALYTICS_BREAKDOWN_BANNER
            )
        }
    ) { paddingValues ->
        com.andriybobchuk.mooney.mooney.presentation.analytics.NetIncomeRedesigned(
            historicalData = state.historicalMetrics,
            currentNetWorth = state.currentNetWorth,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        )
    }
}
