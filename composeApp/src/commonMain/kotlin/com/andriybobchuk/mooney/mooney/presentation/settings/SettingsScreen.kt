package com.andriybobchuk.mooney.mooney.presentation.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import com.andriybobchuk.mooney.core.premium.PaywallSheet
import com.andriybobchuk.mooney.core.premium.isBillingEnabled
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.settings.ExchangeRateSource
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
    onBackClick: () -> Unit,
    onNavigateToTransactionCategories: () -> Unit = {},
    onNavigateToAssetCategories: () -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
    // Settings is a top-level tab — when reached from the bottom nav, this
    // renders the persistent nav bar. Null means we're reached from a
    // detail screen (e.g. older flows) and should show a back button instead.
    bottomNavbar: (@Composable () -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val fileHandler = koinInject<FileHandler>()
    val coroutineScope = rememberCoroutineScope()
    val failedToReadFile = stringResource(Res.string.failed_to_read_file)

    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var showUpdateCategoriesConfirm by remember { mutableStateOf(false) }
    var importJsonData by remember { mutableStateOf<String?>(null) }
    var importPreview by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }

    var showThemeSheet by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showPinnedSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showUserCurrenciesSheet by remember { mutableStateOf(false) }
    var showExchangeRateSourceSheet by remember { mutableStateOf(false) }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var versionTapCount by remember { mutableStateOf(0) }
    // Rate Mooney pre-prompt flow.
    var showRatePrePrompt by remember { mutableStateOf(false) }
    var showRateFeedback by remember { mutableStateOf(false) }
    val rateScope = rememberCoroutineScope()
    val rateReview = org.koin.compose.koinInject<com.andriybobchuk.mooney.core.review.RequestReviewUseCase>()
    if (showRatePrePrompt) {
        com.andriybobchuk.mooney.core.review.ReviewPrePromptDialog(
            onPositive = {
                showRatePrePrompt = false
                rateScope.launch { rateReview.confirmReviewRequested() }
            },
            onNegative = {
                showRatePrePrompt = false
                rateScope.launch { rateReview.markPromptShown() }
                showRateFeedback = true
            },
            onDismiss = {
                showRatePrePrompt = false
                rateScope.launch { rateReview.markPromptShown() }
            },
            source = "settings_rate_row"
        )
    }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(state.restoreMessage) {
        state.restoreMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearRestoreMessage()
        }
    }
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
                is SettingsEvent.ReplayOnboarding -> {
                    onReplayOnboarding()
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
                    Text(stringResource(Res.string.following_will_be_imported))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("\u2022 ${currentPreview.first} ${stringResource(Res.string.nav_transactions).lowercase()}")
                    Text("\u2022 ${currentPreview.second} ${stringResource(Res.string.nav_assets).lowercase()}")
                    Text("\u2022 ${currentPreview.third} ${stringResource(Res.string.nav_goals).lowercase()}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(Res.string.will_add_to_existing), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmImport(currentJsonData)
                    }
                ) {
                    Text(stringResource(Res.string.import_action))
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

    // Update Categories confirmation dialog
    if (showUpdateCategoriesConfirm) {
        AlertDialog(
            onDismissRequest = { showUpdateCategoriesConfirm = false },
            title = { Text(stringResource(Res.string.update_categories_title)) },
            text = {
                Column {
                    Text("Get the latest categories without losing any data:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• New categories from the latest version will be added", style = MaterialTheme.typography.bodySmall)
                    Text("• Existing categories get refreshed titles/emojis", style = MaterialTheme.typography.bodySmall)
                    Text("• Nothing is ever deleted — your transactions, accounts, and custom categories stay exactly as they are", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateCategoriesConfirm = false
                    viewModel.updateTransactionCategories()
                }) {
                    Text(stringResource(Res.string.update))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateCategoriesConfirm = false }) {
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
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
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

    // Exchange rate source bottom sheet
    if (showExchangeRateSourceSheet) {
        MooneyBottomSheet(onDismissRequest = { showExchangeRateSourceSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    stringResource(Res.string.exchange_rate_source),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    stringResource(Res.string.exchange_rate_source_choose),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                ExchangeRateSource.entries.forEach { source ->
                    val isSelected = state.exchangeRateSource == source
                    val title = when (source) {
                        ExchangeRateSource.EXTENDED -> stringResource(Res.string.all_currencies)
                        ExchangeRateSource.HISTORICAL -> stringResource(Res.string.historical_data)
                    }
                    val description = when (source) {
                        ExchangeRateSource.EXTENDED ->
                            "Supports every currency the app offers (incl. UAH, RUB, AED). No historical charts."
                        ExchangeRateSource.HISTORICAL ->
                            "Includes historical rate charts, but only supports major currencies. " +
                                "Others (UAH, RUB, AED) use approximate hardcoded rates."
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable {
                                viewModel.onAction(SettingsAction.OnExchangeRateSourceChange(source))
                                showExchangeRateSourceSheet = false
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(8.dp))
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

    // Categories moved to standalone screens (TransactionCategoriesScreen, AssetCategoriesScreen)
    // Default expense category bottom sheet
    if (showDefaultExpenseCategorySheet) {
        val expenseGenerals = remember(state.allCategories) {
            state.allCategories.filter { it.isGeneralCategory() && it.type == CategoryType.EXPENSE }
        }
        MooneyBottomSheet(onDismissRequest = { showDefaultExpenseCategorySheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(Res.string.default_expense_category), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
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
                Text(stringResource(Res.string.default_income_category), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
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
                Text(stringResource(Res.string.primary_account), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 4.dp))
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
            trigger = com.andriybobchuk.mooney.core.premium.PaywallTrigger.SETTINGS_BANNER,
            onDismiss = { viewModel.dismissPaywall() },
            onSubscribe = { viewModel.onSubscribe() },
            onRestore = { viewModel.onRestorePurchases() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Toolbars.Primary(
                title = stringResource(Res.string.settings),
                // No back button when reached as a primary tab — the user
                // moves between tabs via the bottom nav instead.
                showBackButton = bottomNavbar == null,
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            // Banner above the nav: ineligible-state renders nothing, so the
            // bottom nav stays flush with the screen edge for premium / new
            // users. The slot guards itself via AdEligibilityUseCase.
            androidx.compose.foundation.layout.Column {
                com.andriybobchuk.mooney.core.ads.AdBannerSlot(
                    placement = com.andriybobchuk.mooney.core.ads.AdPlacement.SETTINGS_BANNER
                )
                bottomNavbar?.invoke()
            }
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
                // Premium upsell banner — hidden when the platform doesn't sell
                // anything (Android ships fully free).
                if (isBillingEnabled) {
                    item {
                        var showPaywallFromBanner by remember { mutableStateOf(false) }
                        PremiumBanner(
                            onClick = { showPaywallFromBanner = true }
                        )
                        if (showPaywallFromBanner) {
                            PaywallSheet(
                                isLoading = state.isPurchasing,
                                errorMessage = state.purchaseError,
                                trigger = com.andriybobchuk.mooney.core.premium.PaywallTrigger.SETTINGS_BANNER,
                                onDismiss = { showPaywallFromBanner = false },
                                onSubscribe = { viewModel.onSubscribe() },
                                onRestore = { viewModel.onRestorePurchases() }
                            )
                        }
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
                        // TODO: Re-enable language selector once translations are verified
                        // SettingsDivider()
                        // SettingsRow(title = stringResource(Res.string.language), onClick = { showLanguageSheet = true })
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

                // TRANSACTIONS section — categories management and the
                // default expense / default income / primary-account
                // pickers moved out. Transaction Categories live in the
                // dedicated Categories screen; Primary Account is set on
                // the Assets screen. Only the genuine setting (tax toggle)
                // remains here.
                item {
                    SettingsSectionHeader(stringResource(Res.string.section_transactions))
                }
                item {
                    SettingsGroup {
                        SettingsToggleRow(
                            title = stringResource(Res.string.exclude_taxes_title),
                            description = stringResource(Res.string.exclude_taxes_desc),
                            checked = state.excludeTaxesFromTotals,
                            onCheckedChange = { viewModel.onAction(SettingsAction.OnExcludeTaxesToggle(it)) }
                        )
                    }
                }

                // DEVELOPER OPTIONS section — only visible after 5 taps on Version
                if (state.developerOptionsEnabled) {
                    item { SettingsSectionHeader("Developer Options") }
                    item {
                        SettingsGroup {
                            SettingsRow(
                                title = "Exchange Rate Source",
                                value = when (state.exchangeRateSource) {
                                    ExchangeRateSource.EXTENDED -> "All currencies"
                                    ExchangeRateSource.HISTORICAL -> "Historical data"
                                },
                                onClick = { showExchangeRateSourceSheet = true }
                            )
                            SettingsDivider()
                            SettingsRow(
                                title = "Update Categories",
                                value = if (state.isUpdatingCategories) "Updating…" else "",
                                onClick = { if (!state.isUpdatingCategories) showUpdateCategoriesConfirm = true }
                            )
                            SettingsDivider()
                            SettingsToggleRow(
                                title = "Currency Insights",
                                description = "Show exchange rate trends on foreign currency accounts",
                                checked = state.currencyInsightsEnabled,
                                onCheckedChange = { viewModel.toggleCurrencyInsights(it) }
                            )
                            SettingsDivider()
                            SettingsToggleRow(
                                title = "Transactions Widget Pager",
                                description = "Show the multi-widget pager (spending trend, currency rates, suggest) under the spending calendar",
                                checked = state.widgetPagerEnabled,
                                onCheckedChange = { viewModel.toggleWidgetPager(it) }
                            )
                            SettingsDivider()
                            SettingsToggleRow(
                                title = stringResource(Res.string.disable_ads_dev),
                                description = stringResource(Res.string.disable_ads_dev_desc),
                                checked = state.adsDisabled,
                                onCheckedChange = { viewModel.toggleAdsDisabled(it) }
                            )
                            SettingsDivider()
                            SettingsRow(
                                title = stringResource(Res.string.replay_onboarding),
                                value = stringResource(Res.string.reset_view),
                                onClick = { viewModel.replayOnboarding() }
                            )
                            SettingsDivider()
                            SettingsRow(
                                title = stringResource(Res.string.plan_label),
                                value = if (state.devForcePremium) stringResource(Res.string.pro_label) else stringResource(Res.string.free_label),
                                onClick = { viewModel.setDevPlanPro(!state.devForcePremium) }
                            )
                            SettingsDivider()
                            SettingsRow(
                                title = "Force test crash",
                                value = "Tap to crash",
                                onClick = { viewModel.forceTestCrash() }
                            )
                        }
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
                                            snackbarHostState.showSnackbar(
                                                error.message ?: failedToReadFile
                                            )
                                        }
                                }
                            },
                            showLoading = state.isImporting
                        )
                        SettingsDivider()
                        // Universal CSV import — works with any finance app's
                        // CSV export (Mint, YNAB, Wallet, etc.). Auto-detects
                        // date / amount / description columns; if it can't,
                        // the snackbar tells the user what to rename.
                        SettingsRow(
                            title = stringResource(Res.string.import_csv),
                            description = stringResource(Res.string.import_csv_desc),
                            onClick = {
                                coroutineScope.launch {
                                    fileHandler.pickAndReadTextFile()
                                        .onSuccess { content ->
                                            content?.let {
                                                viewModel.onAction(SettingsAction.OnImportCsv(it))
                                            }
                                        }
                                        .onFailure { error ->
                                            snackbarHostState.showSnackbar(
                                                error.message ?: failedToReadFile
                                            )
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
                            showChevron = false,
                            onClick = {
                                if (!state.developerOptionsEnabled) {
                                    versionTapCount += 1
                                    if (versionTapCount >= 5) {
                                        viewModel.enableDeveloperOptions()
                                        versionTapCount = 0
                                    }
                                }
                            }
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = stringResource(Res.string.share_feedback),
                            onClick = { showFeedbackSheet = true }
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = stringResource(Res.string.rate_mooney),
                            onClick = { showRatePrePrompt = true }
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = stringResource(Res.string.privacy_policy),
                            onClick = { uriHandler.openUri("https://andriybobchuk.github.io/Mooney/privacy-policy.html") }
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = stringResource(Res.string.restore_purchases),
                            onClick = { viewModel.onRestorePurchases() }
                        )
                    }
                }

                // SUPPORT US — rewarded ad. Free-tier + post-grace-period
                // users only; the eligibility check inside this item hides
                // the WHOLE section (header + row) for everyone else, so
                // Premium users never see an empty "Support Mooney" header.
                item {
                    var rewardedStatus by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf<String?>(null)
                    }
                    val rewardedSession = com.andriybobchuk.mooney.core.ads.LocalAdSession.current
                    val eligibility: com.andriybobchuk.mooney.core.ads.AdEligibilityUseCase = org.koin.compose.koinInject()
                    var rewardedEligible by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(false)
                    }
                    androidx.compose.runtime.LaunchedEffect(rewardedSession) {
                        rewardedEligible = eligibility.isEligible(
                            placement = com.andriybobchuk.mooney.core.ads.AdPlacement.REWARDED_FEATURE_UNLOCK,
                            sessionTapCount = rewardedSession.tapCount,
                            sessionCount = rewardedSession.sessionCount
                        )
                    }
                    if (rewardedEligible) {
                        androidx.compose.foundation.layout.Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            SettingsSectionHeader(stringResource(Res.string.support_mooney))
                            SettingsGroup {
                                SettingsRow(
                                    title = stringResource(Res.string.watch_ad_support),
                                    value = rewardedStatus.orEmpty(),
                                    onClick = {
                                        com.andriybobchuk.mooney.core.ads.Ads.showRewarded(
                                            onReward = {
                                                rewardedStatus = "Thanks! 🎉"
                                                com.andriybobchuk.mooney.core.ads.Ads.preloadRewarded(
                                                    com.andriybobchuk.mooney.core.ads.AdUnitIds.rewarded
                                                )
                                            },
                                            onDismissed = {
                                                // Quiet on dismissal — never shame the user.
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showFeedbackSheet) {
        com.andriybobchuk.mooney.core.feedback.FeedbackSheet(
            onDismiss = { showFeedbackSheet = false },
            initialKind = com.andriybobchuk.mooney.mooney.domain.feedback.FeedbackKind.GENERAL
        )
    }
    if (showRateFeedback) {
        com.andriybobchuk.mooney.core.feedback.FeedbackSheet(
            onDismiss = { showRateFeedback = false },
            initialKind = com.andriybobchuk.mooney.mooney.domain.feedback.FeedbackKind.GENERAL
        )
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
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(max = 120.dp).padding(start = 12.dp, end = 4.dp),
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
            .padding(vertical = 12.dp),
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
                        text = stringResource(Res.string.unlimited_accounts_categories),
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
                        text = stringResource(Res.string.upgrade_to_pro_cta),
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
