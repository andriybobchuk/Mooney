package com.andriybobchuk.mooney.mooney.presentation.assets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyButton
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyTextField
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyCard
import com.andriybobchuk.mooney.core.presentation.designsystem.components.ButtonVariant
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MeshGradientBackground
import com.andriybobchuk.mooney.core.premium.PaywallSheet
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.data.database.AssetCategoryEntity
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    viewModel: AssetsViewModel = koinViewModel(),
    bottomNavbar: @Composable () -> Unit,
    onSettingsClick: () -> Unit = {},
    onGoalsClick: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsState()
    val assets = state.assets
    val totalNetWorth = state.totalNetWorth

    // Achieved goals count for badge
    var achievedGoalsCount by remember { mutableStateOf(0) }
    if (onGoalsClick != null) {
        val getGoalsUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.GetGoalsUseCase = org.koin.compose.koinInject()
        val enrichGoalsUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.EnrichGoalsWithProgressUseCase = org.koin.compose.koinInject()
        LaunchedEffect(Unit) {
            getGoalsUseCase().collect { goals ->
                val enriched = enrichGoalsUseCase(goals)
                achievedGoalsCount = enriched.count { gwp ->
                    gwp.progress?.progressPercentage?.let { it >= 100.0 } == true
                }
            }
        }
    }

    // Sheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<UiAsset?>(null) }
    val isEmptyState = assets.isEmpty()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(Color.Transparent),
        topBar = {
            Toolbars.Primary(
                containerColor = Color.Transparent,
                titleContent = {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.onNetWorthLabelClick() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${totalNetWorth.formatWithCommas()} ${state.totalNetWorthCurrency.symbol}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(Res.string.total_net_worth),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                customContent = if (onGoalsClick != null) {
                    {
                        IconButton(onClick = onGoalsClick) {
                            BadgedBox(
                                badge = {
                                    if (achievedGoalsCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text(achievedGoalsCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    painter = com.andriybobchuk.mooney.core.presentation.Icons.GoalsIcon(),
                                    contentDescription = "Goals",
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                } else null,
                actions = listOf(
                    Toolbars.ToolBarAction(
                        painter = com.andriybobchuk.mooney.core.presentation.Icons.SettingsIcon(),
                        contentDescription = stringResource(Res.string.settings),
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
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(Res.string.add_account))
                    },
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (isEmptyState) {
                    MeshGradientBackground()
                }
                // Filter assets by selected tab
                val filteredAssets = assets.filter {
                    it.isLiability == (state.selectedTab == AssetsTab.LIABILITIES)
                }

                AssetsScreenContent(
                    modifier = Modifier.padding(paddingValues),
                assets = filteredAssets,
                hasLiabilities = state.hasLiabilities,
                selectedTab = state.selectedTab,
                onTabSelected = { viewModel.selectTab(it) },
                totalAssetsBase = state.totalAssetsBase,
                totalLiabilitiesBase = state.totalLiabilitiesBase,
                assetCategories = state.assetCategories,
                categoryOrder = state.categoryOrder,
                expandedCategories = state.expandedCategories,
                baseCurrency = state.totalNetWorthCurrency,
                totalNetWorth = totalNetWorth,
                baseNetWorth = if (state.selectedTab == AssetsTab.ASSETS) state.totalAssetsBase else state.totalLiabilitiesBase,
                onEdit = {
                    editingAsset = it
                    showSheet = true
                },
                onDelete = { viewModel.deleteAsset(it.id) },
                onSetPrimary = { viewModel.setPrimaryAccount(it.id) },
                onToggleCategory = { viewModel.toggleCategoryExpansion(it) },
                onUpdateCategoryOrder = { viewModel.updateCategoryOrder(it) },
                onAddAsset = { showSheet = true }
            )
            }

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
                        assetCategories = state.assetCategories,
                        onEditCategories = onSettingsClick,
                        onAdd = { title, emoji, amount, currency, categoryId, isLiability ->
                            viewModel.upsertAsset(
                                editingAsset?.id ?: 0,
                                title,
                                emoji,
                                amount,
                                currency,
                                categoryId,
                                isLiability
                            )
                            // Switch to the matching tab so user sees the new account
                            viewModel.selectTab(
                                if (isLiability) AssetsTab.LIABILITIES else AssetsTab.ASSETS
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

    if (state.showPaywall) {
        PaywallSheet(
            isLoading = state.isPurchasing,
            errorMessage = state.purchaseError,
            onDismiss = { viewModel.dismissPaywall() },
            onSubscribe = { viewModel.onSubscribe() },
            onRestore = { viewModel.onRestorePurchases() }
        )
    }
}

@Composable
private fun AssetsScreenContent(
    modifier: Modifier,
    assets: List<UiAsset>,
    hasLiabilities: Boolean = false,
    selectedTab: AssetsTab = AssetsTab.ASSETS,
    onTabSelected: (AssetsTab) -> Unit = {},
    totalAssetsBase: Double = 0.0,
    totalLiabilitiesBase: Double = 0.0,
    assetCategories: List<AssetCategoryEntity>,
    categoryOrder: List<String>,
    expandedCategories: Set<String>,
    baseCurrency: Currency,
    totalNetWorth: Double = 0.0,
    baseNetWorth: Double = 0.0,
    onEdit: (UiAsset) -> Unit,
    onDelete: (UiAsset) -> Unit,
    onSetPrimary: (UiAsset) -> Unit,
    onToggleCategory: (String) -> Unit,
    onUpdateCategoryOrder: (List<String>) -> Unit = {},
    onAddAsset: () -> Unit = {}
) {
    // Assets grouped by category
    val groupedAssets = remember(assets) { assets.groupBy { it.assetCategoryId } }
    val allCategoryIds = remember(categoryOrder, groupedAssets) {
        categoryOrder + groupedAssets.keys.filter { it !in categoryOrder }
    }
    val visibleCategoryIds = remember(allCategoryIds, groupedAssets) {
        allCategoryIds.filter { groupedAssets.containsKey(it) }.toMutableStateList()
    }
    // Sync when external order changes
    LaunchedEffect(categoryOrder, groupedAssets) {
        val newVisible = (categoryOrder + groupedAssets.keys.filter { it !in categoryOrder })
            .filter { groupedAssets.containsKey(it) }
        if (newVisible != visibleCategoryIds.toList()) {
            visibleCategoryIds.clear()
            visibleCategoryIds.addAll(newVisible)
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Use keys to find category indices — keys are "category_XXXX"
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey = to.key as? String ?: return@rememberReorderableLazyListState
        val fromCatId = fromKey.removePrefix("category_")
        val toCatId = toKey.removePrefix("category_")
        val fromIdx = visibleCategoryIds.indexOf(fromCatId)
        val toIdx = visibleCategoryIds.indexOf(toCatId)
        if (fromIdx >= 0 && toIdx >= 0) {
            visibleCategoryIds.apply {
                add(toIdx, removeAt(fromIdx))
            }
            onUpdateCategoryOrder(visibleCategoryIds.toList())
        }
    }

    LazyColumn(
        state = lazyListState,
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = stringResource(Res.string.welcome_to_mooney),
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
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
                            text = stringResource(Res.string.add_first_account),
                            onClick = onAddAsset,
                            variant = ButtonVariant.PRIMARY,
                            fullWidth = true
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        } else {
            if (hasLiabilities) {
                // Tab bar flush against toolbar
                item {
                    val selectedIndex = AssetsTab.entries.indexOf(selectedTab)
                    TabRow(
                        selectedTabIndex = selectedIndex,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        indicator = { tabPositions ->
                            if (selectedIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                                    height = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        divider = {}
                    ) {
                        AssetsTab.entries.forEachIndexed { index, tab ->
                            val tabTotal = if (tab == AssetsTab.ASSETS) totalAssetsBase else totalLiabilitiesBase
                            Tab(
                                selected = selectedIndex == index,
                                onClick = { onTabSelected(tab) },
                                selectedContentColor = MaterialTheme.colorScheme.onBackground,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = tab.name.lowercase()
                                            .replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${tabTotal.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selectedIndex == index)
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        visibleCategoryIds.forEach { categoryId ->
            val categoryAssets = groupedAssets[categoryId] ?: return@forEach
            val isExpanded = expandedCategories.contains(categoryId)
            val categoryInfo = assetCategories.find { it.id == categoryId }

            item(key = "category_$categoryId") {
                ReorderableItem(reorderableLazyListState, key = "category_$categoryId") { isDragging ->
                    val elevation by animateFloatAsState(if (isDragging) 8f else 0f)
                    Column(
                        modifier = Modifier
                            .shadow(elevation.dp, RoundedCornerShape(8.dp))
                            .background(
                                MaterialTheme.colorScheme.background,
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        CollapsibleCategoryHeader(
                            title = categoryInfo?.title ?: categoryId,
                            color = categoryInfo?.color ?: 0xFF9E9E9E,
                            assetCount = categoryAssets.size,
                            totalAmount = categoryAssets.sumOf { it.baseCurrencyAmount },
                            currency = baseCurrency,
                            isExpanded = isExpanded,
                            onToggleExpand = { onToggleCategory(categoryId) },
                            dragModifier = Modifier
                        )

                        if (isExpanded) {
                            Spacer(Modifier.height(4.dp))
                            categoryAssets.forEach { asset ->
                                val pct = if (baseNetWorth > 0) (asset.baseCurrencyAmount / baseNetWorth).toFloat().coerceIn(0f, 1f) else 0f
                                Box(modifier = Modifier.padding(start = 24.dp)) {
                                    AssetCard(
                                        asset = asset,
                                        percentage = pct,
                                        onEdit = onEdit,
                                        onDelete = onDelete,
                                        onSetPrimary = onSetPrimary
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
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
    title: String,
    color: Long,
    assetCount: Int,
    totalAmount: Double,
    currency: Currency,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    dragModifier: Modifier = Modifier
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
        label = "arrow rotation"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(dragModifier)
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
                painter = com.andriybobchuk.mooney.core.presentation.Icons.ChevronRightIcon(),
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotationAngle),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
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
                    Color(color).copy(alpha = 0.15f),
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
    percentage: Float,
    onEdit: (UiAsset) -> Unit,
    onDelete: (UiAsset) -> Unit,
    onSetPrimary: (UiAsset) -> Unit
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Percentage fill — accent color from left to right
            if (percentage > 0.001f) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawRect(
                        color = androidx.compose.ui.graphics.Color(0xFF3562F6).copy(alpha = 0.08f),
                        size = androidx.compose.ui.geometry.Size(
                            width = size.width * percentage,
                            height = size.height
                        )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = asset.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (asset.originalCurrency != GlobalConfig.baseCurrency) {
                            Box(
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = asset.originalCurrency.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    }
                    if (asset.isPrimary) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.primary_account).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
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
                                append(GlobalConfig.baseCurrency.symbol)
                            }
                        },
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (asset.originalCurrency != GlobalConfig.baseCurrency) {
                        Text(
                            text = "≈ ${asset.originalAmount.formatWithCommas()} ${asset.originalCurrency.symbol}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    Text(stringResource(Res.string.edit), style = MaterialTheme.typography.bodyLarge)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (asset.isPrimary) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable {
                            showActionSheet = false
                            if (!asset.isPrimary) onSetPrimary(asset)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (asset.isPrimary) "✓ ${stringResource(Res.string.primary_account)}"
                        else stringResource(Res.string.set_as_primary),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (asset.isPrimary) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
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
                        stringResource(Res.string.delete),
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
    assetCategories: List<AssetCategoryEntity>,
    onAdd: (String, String, Double, Currency, String, Boolean) -> Unit,
    onEditCategories: () -> Unit = {}
) {
    var title by remember { mutableStateOf(editingAsset?.title ?: "") }
    var amount by remember { mutableStateOf(editingAsset?.originalAmount?.formatWithCommas() ?: "") }
    var selectedCurrency by remember { mutableStateOf(editingAsset?.originalCurrency ?: GlobalConfig.baseCurrency) }
    var selectedCategoryId by remember { mutableStateOf(editingAsset?.assetCategoryId ?: "BANK_ACCOUNT") }
    var isLiability by remember { mutableStateOf(editingAsset?.isLiability ?: false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }

    // Filter categories by asset/liability type
    val filteredCategories = assetCategories.filter { it.isLiability == isLiability }
    // Auto-select first matching category when toggle changes
    LaunchedEffect(isLiability) {
        if (filteredCategories.isNotEmpty() && filteredCategories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = filteredCategories.first().id
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (editingAsset != null) stringResource(Res.string.edit_asset) else stringResource(Res.string.add_new_asset),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Asset / Liability toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Asset" to false, "Liability" to true).forEach { (label, liability) ->
                val isSelected = isLiability == liability
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.background
                            else Color.Transparent
                        )
                        .clickable { isLiability = liability }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        MooneyTextField(
            modifier = Modifier.fillMaxWidth(),
            value = title,
            onValueChange = { if (it.length <= 24) title = it },
            label = stringResource(Res.string.title),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        MooneyTextField(
            modifier = Modifier.fillMaxWidth(),
            value = amount,
            onValueChange = { amount = it },
            label = stringResource(Res.string.value),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        // Category selector row
        val selectedCategoryInfo = filteredCategories.find { it.id == selectedCategoryId }
        Text(stringResource(Res.string.category), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showCategorySheet = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(selectedCategoryInfo?.title ?: selectedCategoryId, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Icon(
                painter = com.andriybobchuk.mooney.core.presentation.Icons.ChevronRightIcon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Currency selector row
        Text(stringResource(Res.string.currency), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
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
                painter = com.andriybobchuk.mooney.core.presentation.Icons.ChevronRightIcon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        MooneyButton(
            text = if (editingAsset != null) stringResource(Res.string.update_asset) else stringResource(Res.string.add_account),
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.PRIMARY,
            onClick = {
                val amt = amount.replace(",", "").toDoubleOrNull() ?: 0.0
                onAdd(title, "", amt, selectedCurrency, selectedCategoryId, isLiability)
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(Res.string.select_category), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { showCategorySheet = false; onEditCategories() }) {
                        Text("Edit")
                    }
                }
                filteredCategories.forEach { category ->
                    val isSelected = selectedCategoryId == category.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { selectedCategoryId = category.id; showCategorySheet = false }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            category.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
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
                Text(stringResource(Res.string.select_currency), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(Currency.entries.size) { index ->
                    val currency = Currency.entries[index]
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
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

