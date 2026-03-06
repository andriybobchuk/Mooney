package com.andriybobchuk.mooney.mooney.presentation.assets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.app.appColors
import com.andriybobchuk.mooney.core.presentation.Toolbars
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.draw.alpha
import com.andriybobchuk.mooney.core.presentation.theme.ThemeManager
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.AssetCategory
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.settings.ThemeMode
import com.andriybobchuk.mooney.mooney.domain.usecase.assets.CategoryInfo
import com.andriybobchuk.mooney.mooney.presentation.formatWithCommas
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    viewModel: AssetsViewModel = koinViewModel(),
    bottomNavbar: @Composable () -> Unit,
    onSettingsClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val assets = state.assets
    val totalNetWorth = state.totalNetWorth
    val diversification = state.diversification
    
    val themeManager: ThemeManager = koinInject()
    val themeMode by themeManager.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

    // Sheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<UiAsset?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        topBar = {
            Toolbars.Primary(
                titleContent = {
                    Column(
                        modifier = Modifier.clickable { viewModel.onNetWorthLabelClick() }
                    ) {
                        Text(
                            text = "${totalNetWorth.formatWithCommas()} ${state.totalNetWorthCurrency.symbol}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        )
                        Text(
                            text = "Total Net Worth",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                actions = listOf(
                    Toolbars.ToolBarAction(
                        icon = Icons.Default.Refresh,
                        contentDescription = "Refresh Exchange Rates",
                        onClick = { viewModel.refreshExchangeRates() }
                    ),
                    Toolbars.ToolBarAction(
                        icon = Icons.Default.Settings,
                        contentDescription = "Settings",
                        onClick = onSettingsClick
                    )
                )
            )
        },
        bottomBar = {
            bottomNavbar()
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSheet = true },
                content = {
                    Icon(Icons.Default.Add, contentDescription = "Add Asset")
                },
                contentColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.primary
            )
        },
        content = { paddingValues ->
            AssetsScreenContent(
                modifier = Modifier.padding(paddingValues),
                assets = assets,
                diversification = diversification,
                assetsAnalytics = state.assetsAnalytics,
                categoryOrder = state.categoryOrder,
                expandedCategories = state.expandedCategories,
                baseCurrency = state.totalNetWorthCurrency,
                onEdit = {
                    editingAsset = it
                    showSheet = true
                },
                onDelete = { viewModel.deleteAsset(it.id) },
                onToggleCategory = { viewModel.toggleCategoryExpansion(it) }
            )

            if (showSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showSheet = false
                        editingAsset = null
                    },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.appColors.cardBackground
                ) {
                    AssetSheet(
                        editingAsset = editingAsset,
                        onAdd = { title, emoji, amount, currency, category ->
                            viewModel.upsertAsset(
                                editingAsset?.id ?: 0,
                                title,
                                emoji,
                                amount,
                                currency,
                                category
                            )
                            scope.launch { 
                                sheetState.hide()
                                showSheet = false 
                            }
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun AssetsScreenContent(
    modifier: Modifier,
    assets: List<UiAsset>,
    diversification: com.andriybobchuk.mooney.mooney.domain.usecase.assets.AssetDiversification?,
    assetsAnalytics: AssetsAnalytics?,
    categoryOrder: List<AssetCategory>,
    expandedCategories: Set<AssetCategory>,
    baseCurrency: Currency,
    onEdit: (UiAsset) -> Unit,
    onDelete: (UiAsset) -> Unit,
    onToggleCategory: (AssetCategory) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Analytics Card
        if (assetsAnalytics != null && assets.isNotEmpty()) {
            item {
                AnalyticsCard(
                    analytics = assetsAnalytics,
                    baseCurrency = baseCurrency
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Assets grouped by category
        val groupedAssets = assets.groupBy { it.assetCategory }
        categoryOrder.forEach { category ->
            val categoryAssets = groupedAssets[category]
            if (categoryAssets != null) {
                val isExpanded = expandedCategories.contains(category)
                
                item {
                    CollapsibleCategoryHeader(
                        category = category,
                        categoryInfo = diversification?.categoryBreakdown?.get(category),
                        totalAmount = categoryAssets.sumOf { it.baseCurrencyAmount },
                        currency = baseCurrency,
                        isExpanded = isExpanded,
                        onToggleExpand = { onToggleCategory(category) }
                    )
                }
                
                if (isExpanded) {
                    items(categoryAssets) { asset ->
                        AssetCard(
                            asset = asset,
                            onEdit = onEdit,
                            onDelete = onDelete
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                
                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        
        item {
            Spacer(Modifier.height(80.dp)) // Space for FAB
        }
    }
}

@Composable
private fun CollapsibleCategoryHeader(
    category: AssetCategory,
    categoryInfo: CategoryInfo?,
    totalAmount: Double,
    currency: Currency,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
        label = "arrow rotation"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggleExpand() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotationAngle),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.emoji,
                fontSize = 20.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Column {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (categoryInfo != null) {
                    Text(
                        text = "${categoryInfo.assetCount} asset${if (categoryInfo.assetCount != 1) "s" else ""} • ${categoryInfo.percentage.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        Box(
            modifier = Modifier
                .background(
                    Color(category.color).copy(alpha = 0.15f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${totalAmount.formatWithCommas()} ${currency.symbol}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssetCard(
    asset: UiAsset,
    onEdit: (UiAsset) -> Unit,
    onDelete: (UiAsset) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { onEdit(asset) },
                onLongClick = { expanded = true }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.appColors.cardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = asset.emoji,
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("${asset.baseCurrencyAmount.formatWithCommas()} ")
                        }
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                            append("${GlobalConfig.baseCurrency.symbol}")
                        }
                    },
                    fontSize = 16.sp
                )
                
                if (asset.originalCurrency != GlobalConfig.baseCurrency) {
                    Text(
                        text = "${asset.originalAmount.formatWithCommas()} ${asset.originalCurrency.symbol}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Edit") },
            onClick = {
                expanded = false
                onEdit(asset)
            }
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                expanded = false
                onDelete(asset)
            }
        )
    }
}

@Composable
private fun AssetSheet(
    editingAsset: UiAsset? = null,
    onAdd: (String, String, Double, Currency, AssetCategory) -> Unit
) {
    var title by remember { mutableStateOf(editingAsset?.title ?: "") }
    var emoji by remember { mutableStateOf(editingAsset?.emoji ?: "💰") }
    var amount by remember { mutableStateOf(editingAsset?.originalAmount?.formatWithCommas() ?: "") }
    var selectedCurrency by remember { mutableStateOf(editingAsset?.originalCurrency ?: GlobalConfig.baseCurrency) }
    var selectedCategory by remember { mutableStateOf(editingAsset?.assetCategory ?: AssetCategory.BANK_ACCOUNT) }

    val currencies = Currency.entries.toList()
    val categories = AssetCategory.entries.toList()

    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = if (editingAsset != null) "Edit Asset" else "Add New Asset",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                modifier = Modifier.width(80.dp),
                value = emoji,
                onValueChange = {
                    if (it.length <= 2) emoji = it
                },
                label = { Text("Icon") },
                singleLine = true
            )

            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = title,
                onValueChange = {
                    if (it.length <= 24) title = it
                },
                label = { Text("Title") },
                singleLine = true
            )
        }

        Spacer(Modifier.height(8.dp))

        // Asset Category Selection
        Text(
            text = "Category",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(category.emoji)
                            Text(category.displayName, fontSize = 12.sp)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(category.color).copy(alpha = 0.2f)
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        // Currency dropdown
        var currencyExpanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { currencyExpanded = true }
            ) {
                Text("Currency: ${selectedCurrency.name}")
            }

            DropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                currencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency.name) },
                        onClick = {
                            selectedCurrency = currency
                            currencyExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val amt = amount.replace(",", "").toDoubleOrNull() ?: 0.0
                onAdd(title, emoji, amt, selectedCurrency, selectedCategory)
            },
            enabled = title.isNotBlank() && amount.isNotBlank()
        ) {
            Text(if (editingAsset != null) "Update Asset" else "Create Asset")
        }
        
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun AnalyticsCard(
    analytics: AssetsAnalytics,
    baseCurrency: Currency
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.appColors.cardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Analytics",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Asset Analytics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.alpha(0.2f),
                thickness = 1.dp
            )
            
            // Bank vs Cash Section
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Liquidity Distribution",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Bank Account
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "🏦",
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${analytics.bankAccountPercentage.toInt()}%",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4285F4)
                            )
                        }
                        Text(
                            text = "Bank Account",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${analytics.bankAccountTotal.formatWithCommas()} ${baseCurrency.symbol}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(60.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    )
                    
                    // Cash
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "💵",
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${analytics.cashPercentage.toInt()}%",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34A853)
                            )
                        }
                        Text(
                            text = "Cash",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${analytics.cashTotal.formatWithCommas()} ${baseCurrency.symbol}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            if (analytics.currencyBreakdown.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.alpha(0.2f),
                    thickness = 1.dp
                )
                
                // Currency Breakdown Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Currency Distribution",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(analytics.currencyBreakdown.entries.sortedByDescending { it.value.percentage }.toList()) { (currency, breakdown) ->
                            CurrencyBreakdownItem(
                                currency = currency,
                                breakdown = breakdown,
                                baseCurrency = baseCurrency
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyBreakdownItem(
    currency: Currency,
    breakdown: CurrencyBreakdown,
    baseCurrency: Currency
) {
    Card(
        modifier = Modifier.width(140.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currency.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${breakdown.percentage.toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${breakdown.totalInCurrency.formatWithCommas()} ${currency.symbol}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            if (currency != baseCurrency) {
                Text(
                    text = "${breakdown.totalInBaseCurrency.formatWithCommas()} ${baseCurrency.symbol}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}