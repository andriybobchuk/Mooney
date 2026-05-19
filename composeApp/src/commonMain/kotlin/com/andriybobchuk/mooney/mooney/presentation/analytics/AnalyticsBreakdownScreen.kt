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
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
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
    }

    val title = when (sheetType) {
        CategorySheetType.REVENUE -> stringResource(Res.string.revenue_breakdown)
        CategorySheetType.OPERATING_COSTS -> "Expenses Breakdown"
        CategorySheetType.TAXES -> stringResource(Res.string.tax_breakdown)
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(Color.Transparent),
        topBar = {
            Toolbars.Primary(
                title = title,
                showBackButton = true,
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        CategoryBreakdownSheet(
            sheetType = sheetType,
            categories = state.sheetCategories,
            historicalData = state.historicalMetrics,
            onCategoryClick = { category -> viewModel.onCategoryClicked(category) },
            onDismiss = onBackClick,
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
                        onDismiss = { viewModel.onSubcategorySheetDismissed() }
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
                        onDismiss = { viewModel.onTransactionsSheetDismissed() }
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
                title = stringResource(Res.string.profit_over_6_months),
                showBackButton = true,
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        NetIncomeChartBottomSheet(
            historicalData = state.historicalMetrics,
            selectedMonth = state.selectedMonth,
            onDismiss = onBackClick,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        )
    }
}
