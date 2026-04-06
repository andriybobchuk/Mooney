package com.andriybobchuk.mooney.mooney.presentation.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.core.presentation.designsystem.components.FeedbackBottomSheet
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import com.andriybobchuk.mooney.core.premium.PaywallSheet
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.settings.ThemeMode
import com.andriybobchuk.mooney.core.platform.FileHandler
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val fileHandler = koinInject<FileHandler>()
    val coroutineScope = rememberCoroutineScope()

    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var importJsonData by remember { mutableStateOf<String?>(null) }
    var importPreview by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }

    var showThemeSheet by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showPinnedSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showUserCurrenciesSheet by remember { mutableStateOf(false) }
    var showCategoriesSheet by remember { mutableStateOf(false) }
    var showAddCategorySheet by remember { mutableStateOf(false) }
    var addCategoryParentId by remember { mutableStateOf<String?>(null) }
    var addCategoryType by remember { mutableStateOf("EXPENSE") }
    var deleteCategoryId by remember { mutableStateOf<String?>(null) }
    var deleteCategoryName by remember { mutableStateOf("") }
    var showAssetCategoriesSheet by remember { mutableStateOf(false) }
    var showAddAssetCategorySheet by remember { mutableStateOf(false) }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var deleteAssetCategoryId by remember { mutableStateOf<String?>(null) }
    var deleteAssetCategoryName by remember { mutableStateOf("") }
    var showDefaultExpenseCategorySheet by remember { mutableStateOf(false) }
    var showDefaultIncomeCategorySheet by remember { mutableStateOf(false) }
    var showPrimaryAccountSheet by remember { mutableStateOf(false) }

    // Handle events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SettingsEvent.ExportReady -> {
                    coroutineScope.launch {
                        fileHandler.saveTextFile(event.jsonData, "mooney_backup.json")
                            .onSuccess {
                                // Could show success toast
                            }
                            .onFailure { error ->
                                // Could show error
                            }
                    }
                }
                is SettingsEvent.ShowImportConfirmation -> {
                    importPreview = Triple(event.transactions, event.accounts, event.goals)
                    showImportConfirmDialog = true
                }
                is SettingsEvent.ImportSuccess -> {
                    // Could show success message
                    showImportConfirmDialog = false
                    importJsonData = null
                    importPreview = null
                }
                is SettingsEvent.ShowError -> {
                    // Handle error display
                }
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            // Show error - could be replaced with SnackbarHost
        }
    }

    // Import confirmation dialog
    val currentPreview = importPreview
    val currentJsonData = importJsonData
    if (showImportConfirmDialog && currentPreview != null && currentJsonData != null) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirmDialog = false
                importJsonData = null
                importPreview = null
            },
            title = { Text(stringResource(Res.string.confirm_import)) },
            text = {
                Column {
                    Text("The following data will be imported:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("\u2022 ${currentPreview.first} transactions")
                    Text("\u2022 ${currentPreview.second} accounts")
                    Text("\u2022 ${currentPreview.third} goals")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This will add to your existing data.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmImport(currentJsonData)
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportConfirmDialog = false
                        importJsonData = null
                        importPreview = null
                    }
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    // Theme bottom sheet
    if (showThemeSheet) {
        MooneyBottomSheet(onDismissRequest = { showThemeSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(Res.string.appearance), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                ThemeMode.entries.forEach { mode ->
                    val isSelected = state.currentThemeMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable {
                                viewModel.onAction(SettingsAction.OnThemeModeChange(mode))
                                showThemeSheet = false
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            when (mode) {
                                ThemeMode.LIGHT -> stringResource(Res.string.light)
                                ThemeMode.DARK -> stringResource(Res.string.dark)
                                ThemeMode.SYSTEM -> stringResource(Res.string.system)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.weight(1f))
                        if (isSelected) {
                            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Currency bottom sheet
    if (showCurrencySheet) {
        MooneyBottomSheet(onDismissRequest = { showCurrencySheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(Res.string.default_currency), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                state.availableCurrencies.forEach { currency ->
                    val isSelected = currency == state.defaultCurrency
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable {
                                viewModel.onAction(SettingsAction.OnDefaultCurrencyChange(currency.name))
                                showCurrencySheet = false
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${currency.symbol} ${currency.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.weight(1f))
                        if (isSelected) {
                            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Pinned categories bottom sheet
    if (showPinnedSheet) {
        MooneyBottomSheet(onDismissRequest = { showPinnedSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(Res.string.pinned_categories), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 4.dp))
                Text(
                    "Select up to ${state.maxPinnedCategories} categories",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(400.dp)
                ) {
                    items(state.allCategories.filter { it.isSubCategory() }) { category ->
                        val isSelected = state.pinnedCategoryIds.contains(category.id)
                        val canSelect = state.canAddMorePinned || isSelected
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                canSelect -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = canSelect) {
                                    viewModel.onAction(SettingsAction.OnCategorySelectionToggle(category))
                                }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = category.resolveEmoji(), fontSize = 24.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = category.title,
                                    fontSize = 12.sp,
                                    color = if (canSelect) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Language bottom sheet
    if (showLanguageSheet) {
        MooneyBottomSheet(onDismissRequest = { showLanguageSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    stringResource(Res.string.language),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    "App follows your device language. Set it in system settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                listOf(
                    "English" to "en",
                    "Українська" to "uk",
                    "Русский" to "ru",
                    "Polski" to "pl",
                    "Deutsch" to "de",
                    "Español" to "es",
                    "Français" to "fr",
                    "Português" to "pt",
                    "Italiano" to "it",
                    "Türkçe" to "tr",
                    "日本語" to "ja",
                    "中文" to "zh",
                ).forEach { (name, code) ->
                    val isCurrentLocale = state.appLanguage == code || (state.appLanguage == "system" && code == "en")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isCurrentLocale) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .clickable {
                                viewModel.onAction(SettingsAction.OnLanguageChange(code))
                                showLanguageSheet = false
                            }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isCurrentLocale) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                code.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isCurrentLocale) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // User currencies bottom sheet (fullscreen)
    if (showUserCurrenciesSheet) {
        MooneyBottomSheet(onDismissRequest = { showUserCurrenciesSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    stringResource(Res.string.manage_currencies),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
                Text(
                    "Select currencies you use. These appear when cycling the net worth display and when creating accounts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(Currency.entries.size) { index ->
                        val currency = Currency.entries[index]
                        val isEnabled = state.userCurrencies.any { it.code == currency.name }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isEnabled) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .clickable {
                                    viewModel.onAction(SettingsAction.OnToggleUserCurrency(currency.name))
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${currency.symbol} ${currency.name}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.weight(1f))
                            if (isEnabled) {
                                Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Delete category confirmation dialog
    val currentDeleteCatId = deleteCategoryId
    if (currentDeleteCatId != null) {
        AlertDialog(
            onDismissRequest = { deleteCategoryId = null },
            title = { Text(stringResource(Res.string.delete)) },
            text = {
                Text("Delete \"$deleteCategoryName\"? Transactions using this category will become unlinked.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAction(SettingsAction.OnDeleteCategory(currentDeleteCatId))
                    deleteCategoryId = null
                }) {
                    Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCategoryId = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    // Categories management bottom sheet (full screen)
    if (showCategoriesSheet) {
        val generalCategories = remember(state.allCategories) {
            state.allCategories.filter { it.isGeneralCategory() }
        }
        MooneyBottomSheet(onDismissRequest = { showCategoriesSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    stringResource(Res.string.manage_categories),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
                Text(
                    "Add, remove, or organize your transaction categories and subcategories.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Add main category button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .clickable {
                            addCategoryParentId = null
                            addCategoryType = "EXPENSE"
                            showAddCategorySheet = true
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "+ ${stringResource(Res.string.add_category)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(generalCategories.size) { index ->
                        val category = generalCategories[index]
                        val subcategories = remember(state.allCategories, category.id) {
                            state.allCategories.filter { it.parent?.id == category.id }
                        }
                        Column {
                            // General category header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(category.resolveEmoji(), fontSize = 18.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    category.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        addCategoryParentId = category.id
                                        addCategoryType = category.type.name
                                        showAddCategorySheet = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        "+ ${stringResource(Res.string.add_subcategory)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Subcategories — indented, no dots
                            subcategories.forEach { sub ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 40.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        sub.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            deleteCategoryId = sub.id
                                            deleteCategoryName = sub.title
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Text(
                                            "×",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Add category bottom sheet
    if (showAddCategorySheet) {
        val isAddingSubcategory = addCategoryParentId != null
        MooneyBottomSheet(onDismissRequest = { showAddCategorySheet = false }) {
            var newCategoryName by remember { mutableStateOf("") }
            var newCategoryEmoji by remember { mutableStateOf("") }
            var selectedType by remember { mutableStateOf(addCategoryType) }
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    if (isAddingSubcategory) stringResource(Res.string.add_subcategory)
                    else stringResource(Res.string.add_category),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Expense/Income toggle — only for parent categories
                if (!isAddingSubcategory) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("EXPENSE" to stringResource(Res.string.expense), "INCOME" to stringResource(Res.string.income)).forEach { (type, label) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedType == type) MaterialTheme.colorScheme.surface
                                        else Color.Transparent
                                    )
                                    .clickable { selectedType = type }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selectedType == type) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text(stringResource(Res.string.category_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isAddingSubcategory) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newCategoryEmoji,
                        onValueChange = { newCategoryEmoji = it },
                        label = { Text(stringResource(Res.string.emoji_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.onAction(
                                SettingsAction.OnAddCategory(
                                    title = newCategoryName.trim(),
                                    type = selectedType,
                                    emoji = if (isAddingSubcategory) null else newCategoryEmoji.ifBlank { null },
                                    parentId = addCategoryParentId
                                )
                            )
                            showAddCategorySheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Text(stringResource(Res.string.add))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Delete asset category confirmation dialog
    val currentDeleteAssetCatId = deleteAssetCategoryId
    if (currentDeleteAssetCatId != null) {
        AlertDialog(
            onDismissRequest = { deleteAssetCategoryId = null },
            text = {
                Text("Delete \"$deleteAssetCategoryName\"? Accounts using this category will keep their data but appear under \"Other\".")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAction(SettingsAction.OnDeleteAssetCategory(currentDeleteAssetCatId))
                    deleteAssetCategoryId = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteAssetCategoryId = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    // Asset categories management bottom sheet
    // Track which type (asset/liability) we're adding a new category for
    var addAssetCategoryIsLiability by remember { mutableStateOf(false) }

    if (showAssetCategoriesSheet) {
        var renamingCategoryId by remember { mutableStateOf<String?>(null) }
        var renameText by remember { mutableStateOf("") }

        val assetCats = state.assetCategories.filter { !it.isLiability }
        val liabilityCats = state.assetCategories.filter { it.isLiability }

        MooneyBottomSheet(onDismissRequest = { showAssetCategoriesSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Asset & Liability Categories",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    // Assets section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "ASSETS",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            TextButton(onClick = {
                                addAssetCategoryIsLiability = false
                                showAddAssetCategorySheet = true
                            }) {
                                Text("+ Add")
                            }
                        }
                    }
                    items(assetCats.size) { index ->
                        val category = assetCats[index]
                        val isRenaming = renamingCategoryId == category.id

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    renamingCategoryId = category.id
                                    renameText = category.title
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isRenaming) {
                                OutlinedTextField(
                                    value = renameText,
                                    onValueChange = { if (it.length <= 30) renameText = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = {
                                    if (renameText.isNotBlank()) {
                                        viewModel.onAction(SettingsAction.OnRenameAssetCategory(category.id, renameText.trim()))
                                    }
                                    renamingCategoryId = null
                                }) {
                                    Text("Save")
                                }
                            } else {
                                Text(
                                    category.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        deleteAssetCategoryId = category.id
                                        deleteAssetCategoryName = category.title
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        if (index < assetCats.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    // Liabilities section
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "LIABILITIES",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            TextButton(onClick = {
                                addAssetCategoryIsLiability = true
                                showAddAssetCategorySheet = true
                            }) {
                                Text("+ Add")
                            }
                        }
                    }
                    items(liabilityCats.size) { index ->
                        val category = liabilityCats[index]
                        val isRenaming = renamingCategoryId == category.id

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    renamingCategoryId = category.id
                                    renameText = category.title
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isRenaming) {
                                OutlinedTextField(
                                    value = renameText,
                                    onValueChange = { if (it.length <= 30) renameText = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = {
                                    if (renameText.isNotBlank()) {
                                        viewModel.onAction(SettingsAction.OnRenameAssetCategory(category.id, renameText.trim()))
                                    }
                                    renamingCategoryId = null
                                }) {
                                    Text("Save")
                                }
                            } else {
                                Text(
                                    category.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        deleteAssetCategoryId = category.id
                                        deleteAssetCategoryName = category.title
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        if (index < liabilityCats.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Add asset category bottom sheet
    if (showAddAssetCategorySheet) {
        MooneyBottomSheet(onDismissRequest = { showAddAssetCategorySheet = false }) {
            var title by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "New Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 30) title = it },
                    label = { Text("Category name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.onAction(SettingsAction.OnAddAssetCategory(title.trim(), addAssetCategoryIsLiability))
                        showAddAssetCategorySheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    )
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Default expense category bottom sheet
    if (showDefaultExpenseCategorySheet) {
        val expenseGenerals = remember(state.allCategories) {
            state.allCategories.filter { it.isGeneralCategory() && it.type == CategoryType.EXPENSE }
        }
        MooneyBottomSheet(onDismissRequest = { showDefaultExpenseCategorySheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Default Expense Category", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    expenseGenerals.forEach { general ->
                        val subcategories = state.allCategories.filter { it.parent?.id == general.id }
                        // General category header (selectable)
                        item(key = general.id) {
                            val isSelected = state.defaultExpenseCategoryId == general.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable {
                                        viewModel.onAction(SettingsAction.OnDefaultExpenseCategoryChange(general.id))
                                        showDefaultExpenseCategorySheet = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(general.resolveEmoji(), fontSize = 20.sp, modifier = Modifier.padding(end = 12.dp))
                                Text(general.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.weight(1f))
                                if (isSelected) { Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) }
                            }
                        }
                        // Subcategories (indented)
                        subcategories.forEach { sub ->
                            item(key = sub.id) {
                                val isSelected = state.defaultExpenseCategoryId == sub.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 32.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable {
                                            viewModel.onAction(SettingsAction.OnDefaultExpenseCategoryChange(sub.id))
                                            showDefaultExpenseCategorySheet = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sub.title, style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.weight(1f))
                                    if (isSelected) { Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Default income category bottom sheet
    if (showDefaultIncomeCategorySheet) {
        val incomeGenerals = remember(state.allCategories) {
            state.allCategories.filter { it.isGeneralCategory() && it.type == CategoryType.INCOME }
        }
        MooneyBottomSheet(onDismissRequest = { showDefaultIncomeCategorySheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Default Income Category", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    incomeGenerals.forEach { general ->
                        val subcategories = state.allCategories.filter { it.parent?.id == general.id }
                        item(key = general.id) {
                            val isSelected = state.defaultIncomeCategoryId == general.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable {
                                        viewModel.onAction(SettingsAction.OnDefaultIncomeCategoryChange(general.id))
                                        showDefaultIncomeCategorySheet = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(general.resolveEmoji(), fontSize = 20.sp, modifier = Modifier.padding(end = 12.dp))
                                Text(general.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.weight(1f))
                                if (isSelected) { Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) }
                            }
                        }
                        subcategories.forEach { sub ->
                            item(key = sub.id) {
                                val isSelected = state.defaultIncomeCategoryId == sub.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 32.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable {
                                            viewModel.onAction(SettingsAction.OnDefaultIncomeCategoryChange(sub.id))
                                            showDefaultIncomeCategorySheet = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sub.title, style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.weight(1f))
                                    if (isSelected) { Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Primary account bottom sheet
    if (showPrimaryAccountSheet) {
        MooneyBottomSheet(onDismissRequest = { showPrimaryAccountSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Primary Account", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 4.dp))
                Text(
                    "This account is pre-selected when adding transactions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                state.accounts.forEach { account ->
                    val isSelected = state.primaryAccountId == account.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable {
                                viewModel.onAction(SettingsAction.OnPrimaryAccountChange(account.id))
                                showPrimaryAccountSheet = false
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(account.emoji, fontSize = 20.sp, modifier = Modifier.padding(end = 12.dp))
                        Text(
                            account.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.weight(1f))
                        if (isSelected) {
                            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (state.showPaywall) {
        PaywallSheet(
            isLoading = state.isPurchasing,
            errorMessage = state.purchaseError,
            onDismiss = { viewModel.dismissPaywall() },
            onSubscribe = { viewModel.onSubscribe() },
            onRestore = { viewModel.onRestorePurchases() }
        )
    }

    Scaffold(
        topBar = {
            Toolbars.Primary(
                title = stringResource(Res.string.settings),
                showBackButton = true,
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Premium upsell banner
                item {
                    var showPaywallFromBanner by remember { mutableStateOf(false) }
                    PremiumBanner(
                        onClick = { showPaywallFromBanner = true }
                    )
                    if (showPaywallFromBanner) {
                        PaywallSheet(
                            isLoading = state.isPurchasing,
                            errorMessage = state.purchaseError,
                            onDismiss = { showPaywallFromBanner = false },
                            onSubscribe = { viewModel.onSubscribe() },
                            onRestore = { viewModel.onRestorePurchases() }
                        )
                    }
                }

                // PREFERENCES section
                item {
                    SettingsSectionHeader(stringResource(Res.string.preferences))
                }
                item {
                    SettingsGroup {
                        SettingsRow(
                            title = stringResource(Res.string.appearance),
                            value = when (state.currentThemeMode) {
                                ThemeMode.LIGHT -> stringResource(Res.string.light)
                                ThemeMode.DARK -> stringResource(Res.string.dark)
                                ThemeMode.SYSTEM -> stringResource(Res.string.system)
                            },
                            onClick = { showThemeSheet = true }
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = stringResource(Res.string.language),
                            value = when (state.appLanguage) {
                                "uk" -> "Українська"
                                "ru" -> "Русский"
                                "pl" -> "Polski"
                                "de" -> "Deutsch"
                                "es" -> "Español"
                                "fr" -> "Français"
                                "pt" -> "Português"
                                "it" -> "Italiano"
                                "tr" -> "Türkçe"
                                "ja" -> "日本語"
                                "zh" -> "中文"
                                "en" -> "English"
                                else -> "System"
                            },
                            onClick = { showLanguageSheet = true }
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = stringResource(Res.string.default_currency),
                            value = "${state.defaultCurrency.symbol} ${state.defaultCurrency.name}",
                            onClick = { showCurrencySheet = true }
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = stringResource(Res.string.currencies),
                            value = state.userCurrencies.joinToString(", ") { it.code },
                            onClick = { showUserCurrenciesSheet = true }
                        )
                    }
                }

                // TRANSACTIONS section
                item {
                    SettingsSectionHeader("Transactions")
                }
                item {
                    val defaultExpenseCat = state.allCategories.find { it.id == state.defaultExpenseCategoryId }
                    val defaultExpenseName = defaultExpenseCat?.let {
                        "${it.resolveEmoji()} ${it.title}"
                    } ?: state.defaultExpenseCategoryId
                    val defaultIncomeCat = state.allCategories.find { it.id == state.defaultIncomeCategoryId }
                    val defaultIncomeName = defaultIncomeCat?.let {
                        "${it.resolveEmoji()} ${it.title}"
                    } ?: state.defaultIncomeCategoryId
                    val primaryAccountName = state.accounts.find { it.id == state.primaryAccountId }?.let {
                        "${it.emoji} ${it.title}"
                    } ?: "None"

                    SettingsGroup {
                        SettingsRow(
                            title = "Transaction Categories",
                            value = "${state.allCategories.size}",
                            onClick = { showCategoriesSheet = true }
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = "Default Expense",
                            value = defaultExpenseName,
                            onClick = { showDefaultExpenseCategorySheet = true }
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = "Default Income",
                            value = defaultIncomeName,
                            onClick = { showDefaultIncomeCategorySheet = true }
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = "Primary Account",
                            value = primaryAccountName,
                            onClick = { showPrimaryAccountSheet = true }
                        )
                        SettingsDivider()
                        SettingsToggleRow(
                            title = "Exclude Taxes from Totals",
                            description = "Tax transactions won't count towards your spending totals",
                            checked = state.excludeTaxesFromTotals,
                            onCheckedChange = { viewModel.onAction(SettingsAction.OnExcludeTaxesToggle(it)) }
                        )
                    }
                }

                // ASSETS section
                item {
                    SettingsSectionHeader("Assets")
                }
                item {
                    SettingsGroup {
                        SettingsRow(
                            title = "Asset Categories",
                            value = "${state.assetCategories.size}",
                            onClick = { showAssetCategoriesSheet = true }
                        )
                    }
                }

                // DATA section
                item {
                    SettingsSectionHeader(stringResource(Res.string.data))
                }
                item {
                    SettingsGroup {
                        SettingsRow(
                            title = stringResource(Res.string.export_data),
                            onClick = { viewModel.onAction(SettingsAction.OnExportData) },
                            showLoading = state.isExporting
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = stringResource(Res.string.import_data),
                            onClick = {
                                coroutineScope.launch {
                                    fileHandler.pickAndReadTextFile()
                                        .onSuccess { content ->
                                            content?.let {
                                                importJsonData = it
                                                viewModel.onAction(SettingsAction.OnImportData(it))
                                            }
                                        }
                                        .onFailure { error ->
                                            // Handle error
                                        }
                                }
                            },
                            showLoading = state.isImporting
                        )
                    }
                }

                // ABOUT section
                item {
                    SettingsSectionHeader(stringResource(Res.string.about))
                }
                item {
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    SettingsGroup {
                        SettingsRow(
                            title = stringResource(Res.string.version),
                            value = com.andriybobchuk.mooney.APP_VERSION,
                            showChevron = false
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = "Chat with Developer",
                            onClick = { showFeedbackSheet = true }
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = "Privacy Policy",
                            onClick = { uriHandler.openUri("https://andriybobchuk.github.io/Mooney/privacy-policy.html") }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showFeedbackSheet) {
        FeedbackBottomSheet(onDismiss = { showFeedbackSheet = false })
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    description: String? = null,
    value: String = "",
    onClick: () -> Unit = {},
    showChevron: Boolean = true,
    showLoading: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !showLoading) { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (showLoading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            if (value.isNotEmpty()) {
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            if (showChevron) {
                Icon(
                    painter = com.andriybobchuk.mooney.core.presentation.Icons.ChevronRightIcon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        MooneyToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MooneyToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val trackColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        animationSpec = androidx.compose.animation.core.tween(200)
    )
    val thumbOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(200)
    )

    Box(
        modifier = Modifier
            .width(44.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(trackColor)
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onCheckedChange(!checked) }
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .offset(x = (18.dp * thumbOffset))
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        thickness = 0.5.dp
    )
}

@Composable
private fun PremiumBanner(onClick: () -> Unit) {
    val blue = Color(0xFF3562F6)
    val teal = Color(0xFF4DD0C8)

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                // Blue-to-teal diagonal gradient
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(blue, teal),
                        start = Offset(0f, h),
                        end = Offset(w, 0f)
                    )
                )
                // Soft white glow top-right for depth
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(w * 0.85f, h * 0.1f),
                        radius = w * 0.45f
                    ),
                    radius = w * 0.45f,
                    center = Offset(w * 0.85f, h * 0.1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Mooney",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "PRO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Unlimited accounts, categories & more",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White
                ) {
                    Text(
                        text = "Upgrade",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = blue,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
