package com.andriybobchuk.mooney.mooney.presentation.assets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyButton
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyTextField
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyCard
import com.andriybobchuk.mooney.core.presentation.designsystem.components.ButtonVariant
import com.andriybobchuk.mooney.core.presentation.designsystem.components.FeedbackBottomSheet
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MeshGradientBackground
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

    val isEmptyState = assets.isEmpty()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        containerColor = if (isEmptyState) Color.Transparent else MaterialTheme.colorScheme.background,
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        topBar = {
            Toolbars.Primary(
                containerColor = if (isEmptyState) Color.Transparent else MaterialTheme.colorScheme.background,
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
            if (assets.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showSheet = true },
                    content = {
                        Icon(Icons.Default.Add, contentDescription = "Add Asset")
                    },
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
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
            .then(
                if (assets.isNotEmpty()) Modifier.padding(horizontal = 10.dp)
                else Modifier
            ),
        userScrollEnabled = assets.isNotEmpty()
    ) {
        if (assets.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize()) {
                    MeshGradientBackground()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "Welcome to Mooney",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add your first account to start tracking your finances. You can add bank accounts, cash, investments, and more.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        MooneyButton(
                            text = "Add Your First Account",
                            onClick = onAddAsset,
                            variant = ButtonVariant.PRIMARY,
                            fullWidth = true
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        } else {
            item {
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AssetCard(
    asset: UiAsset,
    onEdit: (UiAsset) -> Unit,
    onDelete: (UiAsset) -> Unit
) {
    var showActionSheet by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { onEdit(asset) },
                onLongClick = { showActionSheet = true }
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

    if (showActionSheet) {
        MooneyBottomSheet(onDismissRequest = { showActionSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = asset.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${asset.originalAmount.formatWithCommas()} ${asset.originalCurrency.symbol}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            showActionSheet = false
                            onEdit(asset)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edit", style = MaterialTheme.typography.bodyLarge)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .clickable {
                            showActionSheet = false
                            onDelete(asset)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Delete",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetSheet(
    editingAsset: UiAsset? = null,
    onAdd: (String, String, Double, Currency, AssetCategory) -> Unit
) {
    var title by remember { mutableStateOf(editingAsset?.title ?: "") }
    var amount by remember { mutableStateOf(editingAsset?.originalAmount?.formatWithCommas() ?: "") }
    var selectedCurrency by remember { mutableStateOf(editingAsset?.originalCurrency ?: GlobalConfig.baseCurrency) }
    var selectedCategory by remember { mutableStateOf(editingAsset?.assetCategory ?: AssetCategory.BANK_ACCOUNT) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (editingAsset != null) "Edit Asset" else "Add New Asset",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        MooneyTextField(
            modifier = Modifier.fillMaxWidth(),
            value = title,
            onValueChange = { if (it.length <= 24) title = it },
            label = "Title",
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        MooneyTextField(
            modifier = Modifier.fillMaxWidth(),
            value = amount,
            onValueChange = { amount = it },
            label = "Value",
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        // Category selector row
        Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showCategorySheet = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(selectedCategory.displayName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Currency selector row
        Text("Currency", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showCurrencySheet = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${selectedCurrency.symbol}  ${selectedCurrency.name}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        MooneyButton(
            text = if (editingAsset != null) "Update Asset" else "Add Asset",
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.PRIMARY,
            onClick = {
                val amt = amount.replace(",", "").toDoubleOrNull() ?: 0.0
                onAdd(title, selectedCategory.emoji, amt, selectedCurrency, selectedCategory)
            },
            enabled = title.isNotBlank() && amount.isNotBlank()
        )

        Spacer(Modifier.height(20.dp))
    }

    // Category bottom sheet
    if (showCategorySheet) {
        MooneyBottomSheet(onDismissRequest = { showCategorySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Select Category", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                AssetCategory.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { selectedCategory = category; showCategorySheet = false }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(category.emoji, fontSize = 20.sp, modifier = Modifier.padding(end = 12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(category.displayName, style = MaterialTheme.typography.bodyLarge, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            Text(category.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isSelected) {
                            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Currency bottom sheet
    if (showCurrencySheet) {
        MooneyBottomSheet(onDismissRequest = { showCurrencySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text("Select Currency", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                Currency.entries.forEach { currency ->
                    val isSelected = selectedCurrency == currency
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { selectedCurrency = currency; showCurrencySheet = false }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currency.symbol,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Text(
                            text = currency.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.weight(1f))
                        if (isSelected) {
                            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

