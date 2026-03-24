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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyButton
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyTextField
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyCard
import com.andriybobchuk.mooney.core.presentation.designsystem.components.ButtonVariant
import com.andriybobchuk.mooney.core.presentation.designsystem.components.FeedbackBottomSheet
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.app.appColors
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.AssetCategory
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import kotlinx.coroutines.launch
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

    // Sheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<UiAsset?>(null) }
    var showFeedbackSheet by remember { mutableStateOf(false) }

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
                        icon = Icons.Default.Email,
                        contentDescription = "Feedback",
                        onClick = { showFeedbackSheet = true }
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
                categoryOrder = state.categoryOrder,
                expandedCategories = state.expandedCategories,
                baseCurrency = state.totalNetWorthCurrency,
                onEdit = {
                    editingAsset = it
                    showSheet = true
                },
                onDelete = { viewModel.deleteAsset(it.id) },
                onToggleCategory = { viewModel.toggleCategoryExpansion(it) },
                onAddAsset = { showSheet = true }
            )

            if (showSheet) {
                MooneyBottomSheet(
                    onDismissRequest = {
                        showSheet = false
                        editingAsset = null
                    },
                    sheetState = sheetState
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

    if (showFeedbackSheet) {
        FeedbackBottomSheet(onDismiss = { showFeedbackSheet = false })
    }
}

@Composable
private fun AssetsScreenContent(
    modifier: Modifier,
    assets: List<UiAsset>,
    categoryOrder: List<AssetCategory>,
    expandedCategories: Set<AssetCategory>,
    baseCurrency: Currency,
    onEdit: (UiAsset) -> Unit,
    onDelete: (UiAsset) -> Unit,
    onToggleCategory: (AssetCategory) -> Unit,
    onAddAsset: () -> Unit = {}
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

        if (assets.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp, horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "\uD83D\uDCB0", fontSize = 64.sp, modifier = Modifier.padding(bottom = 16.dp))
                    Text(
                        text = "No accounts yet",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add your bank accounts, cash, investments and other assets to track your net worth.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    MooneyButton(
                        text = "Add Account",
                        onClick = onAddAsset,
                        variant = ButtonVariant.PRIMARY
                    )
                }
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
                        assetCount = categoryAssets.size,
                        totalAmount = categoryAssets.sumOf { it.baseCurrencyAmount },
                        currency = baseCurrency,
                        isExpanded = isExpanded,
                        onToggleExpand = { onToggleCategory(category) }
                    )
                }
                
                if (isExpanded) {
                    items(categoryAssets) { asset ->
                        Box(modifier = Modifier.padding(start = 24.dp)) {
                            AssetCard(
                                asset = asset,
                                onEdit = onEdit,
                                onDelete = onDelete
                            )
                        }
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
    assetCount: Int,
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
                    fontWeight = FontWeight.Medium
                )
                if (assetCount > 0) {
                    Text(
                        text = "$assetCount asset${if (assetCount != 1) "s" else ""}",
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
                fontWeight = FontWeight.Medium
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
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                            append("${asset.baseCurrencyAmount.formatWithCommas()} ")
                        }
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                            append("${GlobalConfig.baseCurrency.symbol}")
                        }
                    },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
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
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp
        )
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MooneyTextField(
                modifier = Modifier.width(80.dp),
                value = emoji,
                onValueChange = {
                    if (it.length <= 2) emoji = it
                },
                label = "Icon",
                singleLine = true
            )

            MooneyTextField(
                modifier = Modifier.weight(1f),
                value = title,
                onValueChange = {
                    if (it.length <= 24) title = it
                },
                label = "Title",
                singleLine = true
            )
        }

        Spacer(Modifier.height(12.dp))

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

        Spacer(Modifier.height(12.dp))

        MooneyTextField(
            modifier = Modifier.fillMaxWidth(),
            value = amount,
            onValueChange = { amount = it },
            label = "Amount",
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        // Currency dropdown
        var currencyExpanded by remember { mutableStateOf(false) }
        Box {
            MooneyButton(
                text = "Currency: ${selectedCurrency.name}",
                modifier = Modifier.fillMaxWidth(),
                onClick = { currencyExpanded = true },
                variant = ButtonVariant.SECONDARY
            )

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

        MooneyButton(
            text = if (editingAsset != null) "Update Asset" else "Add Asset",
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.PRIMARY,
            onClick = {
                val amt = amount.replace(",", "").toDoubleOrNull() ?: 0.0
                onAdd(title, emoji, amt, selectedCurrency, selectedCategory)
            },
            enabled = title.isNotBlank() && amount.isNotBlank()
        )
        
        Spacer(Modifier.height(20.dp))
    }
}

