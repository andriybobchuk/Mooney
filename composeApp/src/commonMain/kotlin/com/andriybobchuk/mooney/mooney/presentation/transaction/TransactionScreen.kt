@file:Suppress("TooManyFunctions")

package com.andriybobchuk.mooney.mooney.presentation.transaction

import kotlin.coroutines.cancellation.CancellationException
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableFloatStateOf
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.data.database.PendingTransactionEntity
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.app.appColors
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.AssetCategory
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.RecurringFrequency
import com.andriybobchuk.mooney.mooney.domain.RecurringSchedule
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.presentation.recurring.RecurringScheduleSheet
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateDailyTotalsMapUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetTransactionsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.TransactionValidation
import com.andriybobchuk.mooney.mooney.domain.usecase.ValidateTransactionUseCase
import org.koin.compose.koinInject
import com.andriybobchuk.mooney.mooney.presentation.account.UiAccount
import com.andriybobchuk.mooney.mooney.presentation.account.toAccounts
import com.andriybobchuk.mooney.mooney.presentation.analytics.MonthPicker
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyButton
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyTextField
import com.andriybobchuk.mooney.core.presentation.designsystem.components.ButtonVariant
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MeshGradientBackground
import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import com.andriybobchuk.mooney.mooney.domain.parseAmountInput
import com.andriybobchuk.mooney.mooney.domain.formatToShortString
import com.andriybobchuk.mooney.mooney.domain.formatToPlainString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.*
import androidx.compose.foundation.border
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.datastore.preferences.core.edit
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionViewModel = koinViewModel(),
    bottomNavbar: @Composable () -> Unit,
    onSettingsClick: () -> Unit = {},
    onNavigateToAssets: () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    onNavigateToTransactionCategories: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val transactions = state.transactions
    val total = state.total
    val totalCurrency = state.totalCurrency
    val frequentCategories by viewModel.frequentCategories.collectAsStateWithLifecycle()

    // Dev-only widget pager flag. Default false so production users see only
    // the spending calendar — no pager, no dots indicator stealing space.
    val transactionsDataStore = org.koin.compose.koinInject<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>>()
    val widgetPagerEnabled by transactionsDataStore.data
        .map { it[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.WIDGET_PAGER_ENABLED] ?: false }
        .collectAsStateWithLifecycle(initialValue = false)

    // Cold-start shimmer can vanish in well under a frame if the cache emits
    // fast — wrap the flag to guarantee a visible minimum duration so the
    // user actually sees the loading state.
    val showShimmer by com.andriybobchuk.mooney.core.presentation.rememberMinDisplayShimmer(state.isInitialLoading)
    // Sheet
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isBottomSheetOpen by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var preselectedCategory by remember { mutableStateOf<Category?>(null) }

    // Review pre-prompt state (driven by milestone events from the ViewModel).
    var showReviewPrePrompt by remember { mutableStateOf(false) }
    var showReviewFeedback by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.reviewPrePromptRequests.collect { showReviewPrePrompt = true }
    }
    if (showReviewPrePrompt) {
        com.andriybobchuk.mooney.core.review.ReviewPrePromptDialog(
            onPositive = {
                showReviewPrePrompt = false
                viewModel.onReviewPrePromptPositive()
            },
            onNegative = {
                showReviewPrePrompt = false
                viewModel.onReviewPrePromptNegative()
                showReviewFeedback = true
            },
            onDismiss = {
                showReviewPrePrompt = false
                viewModel.onReviewPrePromptDismissed()
            },
            source = "transaction_milestone"
        )
    }
    if (showReviewFeedback) {
        com.andriybobchuk.mooney.core.feedback.FeedbackSheet(
            onDismiss = { showReviewFeedback = false }
        )
    }

    val hasTransactions = transactions.filterNotNull().isNotEmpty()
    val hasPendingTransactions = state.pendingTransactions.isNotEmpty()
    val hasAccounts = state.accounts.filterNotNull().isNotEmpty()
    // Don't treat "still loading" as empty — otherwise we'd flash the empty-state
    // CTA for half a second on cold start before the DB query lands.
    // Gated on showShimmer (not the raw flag) so an instantly-resolved cache
    // can't paint the empty-state CTA while the shimmer is still required.
    val isEmptyState = !hasTransactions && !hasPendingTransactions && !showShimmer

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(Color.Transparent),
        topBar = {
            Toolbars.Primary(
                containerColor = Color.Transparent,
                titleContent = {
                    // Amount + "Spent in <Month>" subtitle. Date stepper sits
                    // in the actions slot to the right of the title — Settings
                    // moved out of the toolbar entirely (now a bottom-nav tab).
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.onTotalCurrencyClick() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${total.formatWithCommas()} ${totalCurrency.symbol}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(Res.string.spent_in, state.selectedMonth.toDisplayString().substringBeforeLast(' ')),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                customContent = {
                    // Compact date stepper replaces the Settings icon in the
                    // right-side band: ◀ [pill ▾] ▶. The pill opens the
                    // year/month picker.
                    com.andriybobchuk.mooney.mooney.presentation.components.MonthSelector(
                        selectedMonth = state.selectedMonth,
                        onMonthSelected = viewModel::onMonthSelected,
                        monthlyCounts = state.monthlyTransactionCounts
                    )
                },
                actions = emptyList()
            )
        },
        bottomBar = { bottomNavbar() },
        floatingActionButton = {
            val hasTransactions = transactions.filterNotNull().isNotEmpty()
            val hasAccounts = state.accounts.filterNotNull().isNotEmpty()
            if (hasTransactions && hasAccounts) {
                FloatingActionButton(
                    onClick = {
                        // Clear edit context — without this a stale transactionToEdit
                        // from a previous edit/dismiss can cause "+" to open in edit mode.
                        transactionToEdit = null
                        preselectedCategory = null
                        isBottomSheetOpen = true
                    },
                    content = {
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(Res.string.cd_add_transaction))
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

                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    // Quick-action chips: Recurring, Exchange, Goals,
                    // Categories. Date stepper is back in the toolbar so the
                    // chip row is the only thing between toolbar and calendar.
                    if (!showShimmer) {
                        QuickActionChipsRow(
                            onRecurringClick = onNavigateToRecurring,
                            onGoalsClick = onNavigateToGoals,
                            onCategoriesClick = onNavigateToTransactionCategories
                        )
                    }
                    if (showShimmer) {
                        TransactionsScreenShimmer(modifier = Modifier.fillMaxSize())
                    } else
                    TransactionsScreenContent(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            transactions = transactions,
                            accounts = state.accounts,
                            total = total,
                            currency = totalCurrency,
                            selectedMonth = state.selectedMonth,
                            dailyTotals = state.dailyTotals,
                            hasAccounts = state.accounts.filterNotNull().isNotEmpty(),
                            onCurrencyClick = viewModel::onTotalCurrencyClick,
                            onEdit = {
                                transactionToEdit = it
                                isBottomSheetOpen = true
                            },
                            onDelete = viewModel::deleteTransaction,
                            onDailyTotal = viewModel::getDailyTotal,
                            onAddTransaction = {
                                transactionToEdit = null
                                preselectedCategory = null
                                isBottomSheetOpen = true
                            },
                            onNavigateToAssets = onNavigateToAssets,
                            pendingTransactions = state.pendingTransactions,
                            categories = state.categories,
                            onAcceptPending = viewModel::acceptPendingTransaction,
                            onSkipPending = viewModel::skipPendingTransaction,
                            showAllWidgets = widgetPagerEnabled,
                        )
            }
            }
        }
    )

    if (isBottomSheetOpen) {
                TransactionBottomSheet(
                    onDismiss = {
                        isBottomSheetOpen = false
                        transactionToEdit = null
                        preselectedCategory = null
                    },
                    sheetState = bottomSheetState,
                    transactionToEdit = transactionToEdit,
                    accounts = state.accounts,
                    categories = state.categories,
                    selectedMonth = state.selectedMonth,
                    assetCategories = state.assetCategories,
                    categoryOrder = state.categoryOrder,
                    expandedCategories = state.expandedCategories,
                    onToggleAccountCategory = viewModel::toggleAccountCategoryExpansion,
                    preselectedCategory = preselectedCategory,
                    transactionCategoryOrder = state.transactionCategoryOrder,
                    onTransactionCategoryReorder = viewModel::updateTransactionCategoryOrder,
                    onAdd = { transaction, schedule ->
                        isBottomSheetOpen = false
                        transactionToEdit = null
                        if (schedule != null) {
                            viewModel.createRecurringFromTransaction(transaction, schedule)
                        } else {
                            viewModel.upsertTransaction(transaction)
                        }
                    },
                    onUpdate = { transaction, _ ->
                        isBottomSheetOpen = false
                        transactionToEdit = null
                        viewModel.upsertTransaction(transaction)
                    },
                    onEditCategories = onNavigateToTransactionCategories
                )
            }

}

fun LocalDate.formatForDisplay(): String {
    val dayOfWeek = this.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val month = this.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val day = this.dayOfMonth
    return "$dayOfWeek, $month $day" // e.g., "Sun, Aug 23"
}

@Composable
private fun PendingTransactionItem(
    pending: PendingTransactionEntity,
    accounts: List<UiAccount?>,
    categories: List<Category>,
    onAccept: () -> Unit,
    onSkip: () -> Unit
) {
    val account = accounts.filterNotNull().find { it.id == pending.accountId }
    val category = categories.find { it.id == pending.subcategoryId }
    val contentAlpha = 0.45f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Emoji icon — ghosted
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                category?.resolveEmoji() ?: "",
                fontSize = 25.sp,
                modifier = Modifier.alpha(contentAlpha)
            )
        }

        Spacer(Modifier.width(11.dp))

        // Title + amount
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "↻ ",
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    category?.title ?: pending.subcategoryId,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
            }
            Text(
                "${pending.amount.formatWithCommas()} ${account?.originalCurrency?.symbol ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Skip button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .clickable(onClick = onSkip)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Skip",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Confirm button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .clickable(onClick = onAccept)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(Res.string.confirm),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TransactionsScreenShimmer(modifier: Modifier = Modifier) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "txShimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "txShimmerAlpha"
    )
    val barColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f * (alpha * 2f).coerceAtMost(1f))

    Column(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Calendar/heatmap placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(barColor)
        )
        Spacer(Modifier.height(16.dp))

        // 4 day-group shimmer placeholders
        repeat(4) { groupIdx ->
            // Day label
            Box(
                modifier = Modifier
                    .padding(start = 6.dp, top = 6.dp, bottom = 8.dp)
                    .height(12.dp)
                    .width((90 + (groupIdx * 12)).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
            // 2-3 transaction rows per group, inlined to keep file function count low
            val rows = if (groupIdx == 0) 3 else 2
            repeat(rows) { rowIdx ->
                val titleFraction = 0.40f + (rowIdx % 3) * 0.10f
                val amountFraction = 0.18f + (rowIdx % 2) * 0.05f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(barColor)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(titleFraction)
                                .height(13.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(barColor)
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.22f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(barColor.copy(alpha = barColor.alpha * 0.6f))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(amountFraction)
                            .height(13.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor)
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreenContent(
    modifier: Modifier,
    transactions: List<Transaction?>,
    accounts: List<UiAccount?>,
    total: Double,
    currency: Currency,
    selectedMonth: MonthKey,
    dailyTotals: Map<Int, Double> = emptyMap(),
    hasAccounts: Boolean = true,
    onCurrencyClick: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Int) -> Unit,
    onDailyTotal: (LocalDate) -> Double = { 0.0 },
    onAddTransaction: () -> Unit = {},
    onNavigateToAssets: () -> Unit = {},
    pendingTransactions: List<PendingTransactionEntity> = emptyList(),
    categories: List<Category> = emptyList(),
    onAcceptPending: (PendingTransactionEntity) -> Unit = {},
    onSkipPending: (Int) -> Unit = {},
    showAllWidgets: Boolean = false,
) {
    // Group and sort transactions by date (descending), then by ID (most recent first)
    val grouped by remember(transactions) {
        derivedStateOf {
            transactions.filterNotNull().groupBy { it.date }
        }
    }
    val sortedGroups by remember(grouped) {
        derivedStateOf {
            grouped.entries
                .sortedByDescending { it.key }
                .map { (date, txs) -> date to txs.sortedByDescending { it.id } }
        }
    }

    // Group pending transactions by scheduled date
    val pendingGrouped by remember(pendingTransactions) {
        derivedStateOf {
            pendingTransactions.groupBy { pending ->
                try { LocalDate.parse(pending.scheduledDate) } catch (_: Exception) { null }
            }.mapNotNull { (key, value) -> key?.let { it to value } }.toMap()
        }
    }

    // Merge all dates from both real and pending transactions
    val allDates by remember(sortedGroups, pendingGrouped) {
        derivedStateOf {
            (sortedGroups.map { it.first }.toSet() + pendingGrouped.keys).sortedDescending()
        }
    }

    // The most recent day that actually has confirmed transactions — anchor
    // for the single native ad row we let inside the feed.
    val latestFilledDay by remember(sortedGroups) {
        derivedStateOf {
            sortedGroups.firstOrNull { it.second.isNotEmpty() }?.first
        }
    }

    val isEmpty = sortedGroups.isEmpty() && pendingTransactions.isEmpty()

    // Track item indices per-date so a click on the calendar heatmap can
    // animate-scroll the list straight to that day (#30). The +1 accounts for
    // the leading TransactionPagerView item; the loop adds 1 for each
    // sticky header plus one per child row (real tx + pending).
    val dateToIndex by remember(allDates, sortedGroups, pendingGrouped) {
        derivedStateOf {
            val map = mutableMapOf<LocalDate, Int>()
            var index = 1  // index 0 is TransactionPagerView
            allDates.forEach { date ->
                map[date] = index
                val txCount = sortedGroups.firstOrNull { it.first == date }?.second?.size ?: 0
                val pendingCount = pendingGrouped[date]?.size ?: 0
                index += 1 + txCount + pendingCount  // header + items
            }
            map
        }
    }

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val scrollToTopBus: com.andriybobchuk.mooney.app.ScrollToTopBus = org.koin.compose.koinInject()
    LaunchedEffect(scrollToTopBus) {
        scrollToTopBus.events.collect { tab ->
            if (tab == com.andriybobchuk.mooney.app.ScrollToTopBus.Tab.TRANSACTIONS) {
                lazyListState.animateScrollToItem(0)
            }
        }
    }
    val onDayClick: (LocalDate) -> Unit = { date ->
        // If the day has transactions, scroll to its header. If not, scroll to
        // the nearest date with entries (the first allDates entry whose date
        // is <= the tapped day).
        val target = dateToIndex[date] ?: allDates.firstOrNull { it <= date }?.let { dateToIndex[it] }
        if (target != null) {
            coroutineScope.launch { lazyListState.animateScrollToItem(target) }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isEmpty) Color.Transparent else MaterialTheme.colorScheme.background),
        state = lazyListState,
        userScrollEnabled = !isEmpty
    ) {
        if (sortedGroups.isNotEmpty() || pendingTransactions.isNotEmpty()) {
            // Normal: show calendar + transactions
            item {
                TransactionPagerView(
                    selectedMonth = selectedMonth,
                    dailyTotals = dailyTotals,
                    onDayClick = onDayClick,
                    showAllWidgets = showAllWidgets,
                    // Side padding only — the bottom (and top) bands of
                    // dead space around the calendar are gone. The first
                    // date header sits flush against the calendar grid.
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else if (!hasAccounts) {
            // No accounts: show onboarding guide
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
                            text = stringResource(Res.string.lets_get_started),
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Step 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("1", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(stringResource(Res.string.add_account_step), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                                Text(
                                    stringResource(Res.string.add_account_step_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Connector line
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(24.dp)
                                .background(MaterialTheme.colorScheme.outline)
                        )

                        // Step 2
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("2", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(stringResource(Res.string.track_spending_step), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    stringResource(Res.string.track_spending_step_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        MooneyButton(
                            text = stringResource(Res.string.add_first_account),
                            onClick = onNavigateToAssets,
                            variant = ButtonVariant.PRIMARY,
                            fullWidth = true
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        } else {
            // Has accounts but no transactions this month: simple empty state
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
                            text = stringResource(Res.string.no_transactions),
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.start_tracking_first_tx),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        MooneyButton(
                            text = stringResource(Res.string.add_transaction),
                            onClick = onAddTransaction,
                            variant = ButtonVariant.PRIMARY,
                            fullWidth = true
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }

        allDates.forEach { date ->
            val txList = grouped[date]?.sortedByDescending { it.id } ?: emptyList()
            val pendingList = pendingGrouped[date] ?: emptyList()

            stickyHeader {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.background,
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = date.formatForDisplay(),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val dailyTotal = onDailyTotal(date)
                    if (dailyTotal > 0) {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.background,
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${dailyTotal.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Pending (proposed) transactions — ghosted style
            items(pendingList, key = { "pending_${it.id}" }) { pending ->
                PendingTransactionItem(
                    pending = pending,
                    accounts = accounts,
                    categories = categories,
                    onAccept = { onAcceptPending(pending) },
                    onSkip = { onSkipPending(pending.id) }
                )
            }

            // Real transactions
            items(txList) { tx ->
                var showActionSheet by remember { mutableStateOf(false) }
                var showDeleteConfirm by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier.combinedClickable(
                        onClick = { onEdit(tx) },
                        onLongClick = { showActionSheet = true }
                    )
                ) {
                    TransactionItem(tx, accounts)
                }

                if (showActionSheet) {
                    MooneyBottomSheet(onDismissRequest = { showActionSheet = false }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = tx.subcategory.title,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "${tx.amount.formatWithCommas()} ${tx.account.currency.symbol}",
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
                                        onEdit(tx)
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
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .clickable {
                                        showActionSheet = false
                                        showDeleteConfirm = true
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

                if (showDeleteConfirm) {
                    MooneyBottomSheet(onDismissRequest = { showDeleteConfirm = false }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.delete),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = stringResource(Res.string.delete_tx_confirm, tx.subcategory.title, tx.amount.formatWithCommas(), tx.account.currency.symbol),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .clickable {
                                        showDeleteConfirm = false
                                        onDelete(tx.id)
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

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { showDeleteConfirm = false }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(Res.string.cancel), style = MaterialTheme.typography.bodyLarge)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            // Single sponsored "transaction-style" row — sits at the bottom of
            // the latest filled day so it lands inside the user's actual feed
            // rhythm rather than at the tail of an empty list. AdBannerSlot
            // self-gates: hidden for premium / grace-period / cooled-down users.
            if (
                com.andriybobchuk.mooney.mooney.domain.FeatureFlags.adsOnTransactionsEnabled &&
                date == latestFilledDay
            ) {
                item(key = "native_ad_$date") {
                    NativeTransactionAdRow()
                }
            }
        }
    }

    // Scroll-to-top — appears once the list has been scrolled past the first
    // item. Sits at the bottom-end above the bottom nav, slightly inset
    // so it doesn't crowd the add-transaction FAB at the top-level Scaffold.
    val showScrollToTop = remember {
        androidx.compose.runtime.derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = showScrollToTop.value,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 88.dp),
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
    ) {
        androidx.compose.material3.SmallFloatingActionButton(
            onClick = {
                coroutineScope.launch { lazyListState.animateScrollToItem(0) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                defaultElevation = 4.dp,
                pressedElevation = 6.dp
            )
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Outlined.KeyboardArrowDown,
                contentDescription = stringResource(Res.string.cd_scroll_to_top),
                modifier = Modifier.rotate(180f)
            )
        }
    }
    } // end Box
}


@Composable
fun TransactionItem(transaction: Transaction, accounts: List<UiAccount?>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Text(transaction.subcategory.resolveEmoji(), fontSize = 25.sp)
        }

        Spacer(Modifier.width(11.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (transaction.subcategory.type == CategoryType.TRANSFER) {
                // For transfers: show "Internal Transfer" as title
                Text(
                    stringResource(Res.string.internal_transfer),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal, fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Extract destination account from category ID and show "[Account1] to [Account2]"
                val destinationAccountId = transaction.subcategory.id.removePrefix("transfer_to_").toIntOrNull()
                val destinationAccount = accounts.find { it?.id == destinationAccountId }
                val destinationAccountTitle = destinationAccount?.title ?: "Unknown"
                Text(
                    stringResource(Res.string.transfer_from_to, transaction.account.title, destinationAccountTitle),
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            } else {
                // For regular transactions: show category title
                Text(
                    transaction.subcategory.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal, fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (transaction.subcategory.isSubCategory()) {
                    Text(
                        transaction.subcategory.parent?.title ?: "???",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${transaction.amount.formatWithCommas()} ${transaction.account.currency.symbol}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 14.5.sp),
                color = when (transaction.subcategory.type) {
                    CategoryType.INCOME -> MaterialTheme.appColors.incomeColor
                    CategoryType.EXPENSE -> MaterialTheme.appColors.expenseColor
                    CategoryType.TRANSFER -> MaterialTheme.colorScheme.onSurface // White in dark mode, black in light mode
                }
            )
            // Cross-currency transfer: show "→ X DST" under the source amount so
            // both legs are visible at a glance.
            if (transaction.subcategory.type == CategoryType.TRANSFER && transaction.destinationAmount != null) {
                val destinationAccountId = transaction.subcategory.id.removePrefix("transfer_to_").toIntOrNull()
                val destinationCurrency = accounts.find { it?.id == destinationAccountId }?.let { Currency.valueOf(it.originalCurrency.name) }
                if (destinationCurrency != null && destinationCurrency != transaction.account.currency) {
                    Text(
                        "→ ${transaction.destinationAmount.formatWithCommas()} ${destinationCurrency.symbol}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            Text(
                transaction.account.title,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
//            if (transaction.exchangeRate != null) {
//                Text(
//                    "*${transaction.exchangeRate.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}",
//                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
//                )
//            }
        }
    }
}

/**
 * Bare banner ad slot dropped into the transaction feed at the bottom of the
 * latest filled day. No custom chrome — just the raw AdMob banner so it reads
 * as standard-issue rather than pretending to be a transaction. Self-hides
 * for premium / grace-period / cooled-down users via the eligibility gate.
 */
@Composable
fun NativeTransactionAdRow() {
    val eligibility: com.andriybobchuk.mooney.core.ads.AdEligibilityUseCase = org.koin.compose.koinInject()
    val session = com.andriybobchuk.mooney.core.ads.LocalAdSession.current
    var eligible by remember { mutableStateOf(false) }

    LaunchedEffect(session) {
        eligible = eligibility.isEligible(
            placement = com.andriybobchuk.mooney.core.ads.AdPlacement.TRANSACTIONS_NATIVE_ROW,
            sessionTapCount = session.tapCount,
            sessionCount = session.sessionCount
        )
        if (eligible) {
            eligibility.markShown(com.andriybobchuk.mooney.core.ads.AdPlacement.TRANSACTIONS_NATIVE_ROW)
        }
    }

    if (!eligible) return

    com.andriybobchuk.mooney.core.ads.MooneyBannerAdView(
        adUnitId = com.andriybobchuk.mooney.core.ads.AdUnitIds.banner,
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState,
    transactionToEdit: Transaction?,
    accounts: List<UiAccount?>,
    categories: List<Category>,
    selectedMonth: MonthKey,
    assetCategories: List<com.andriybobchuk.mooney.core.data.database.AssetCategoryEntity> = emptyList(),
    categoryOrder: List<String> = emptyList(),
    expandedCategories: Set<String> = emptySet(),
    onToggleAccountCategory: (String) -> Unit = {},
    preselectedCategory: Category? = null,
    forceRecurringEnabled: Boolean = false,
    initialRecurringSchedule: RecurringSchedule? = null,
    transactionCategoryOrder: List<String> = emptyList(),
    onTransactionCategoryReorder: (List<String>) -> Unit = {},
    onAdd: (Transaction, RecurringSchedule?) -> Unit = { _, _ -> },
    onUpdate: (Transaction, RecurringSchedule?) -> Unit = { _, _ -> },
    onEditCategories: () -> Unit = {},
    validateUseCase: ValidateTransactionUseCase = koinInject(),
    preferencesRepository: com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository = koinInject(),
    currencyManagerUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.CurrencyManagerUseCase = koinInject(),
) {
    val isEditMode = transactionToEdit != null

    var amount by remember { mutableStateOf(transactionToEdit?.amount?.formatToPlainString()) }
    var newAccountValue by remember { mutableStateOf("") }
    var description by remember { mutableStateOf(transactionToEdit?.description ?: "") }

    // Default account picker priority:
    //   1. Edit mode — existing transaction's account
    //   2. User's primary account if marked
    //   3. The only account they have — saves a tap for single-account users
    val realAccounts = accounts.filterNotNull().toAccounts()
    val defaultAccount = realAccounts.find { it.isPrimary }
        ?: realAccounts.singleOrNull()
    var selectedAccount by remember { mutableStateOf(transactionToEdit?.account ?: defaultAccount) }
    
    // For edit mode transfers, extract destination account from category ID
    val initialDestinationAccount = if (isEditMode && transactionToEdit?.subcategory?.type == CategoryType.TRANSFER) {
        val destinationAccountId = transactionToEdit.subcategory.id.removePrefix("transfer_to_").toIntOrNull()
        destinationAccountId?.let { id ->
            accounts.filterNotNull().toAccounts().find { it.id == id }
        }
    } else null
    
    var destinationAccount by remember { mutableStateOf(initialDestinationAccount) }

    // Cross-currency transfer state.
    // - destinationAmountText: what's shown in the editable destination-amount field.
    // - userOverrodeDestination: flips to true the first time the user types in
    //   the destination field, so we stop auto-recomputing from the source amount
    //   and respect their manual rate.
    val initialDestText = transactionToEdit?.destinationAmount?.formatToPlainString() ?: ""
    var destinationAmountText by remember { mutableStateOf(initialDestText) }
    var userOverrodeDestination by remember { mutableStateOf(transactionToEdit?.destinationAmount != null) }

    // Transaction type state (Expense, Income, or Transfer)
    var selectedTransactionType by remember { 
        mutableStateOf(
            if (isEditMode) {
                transactionToEdit?.subcategory?.type ?: CategoryType.EXPENSE
            } else {
                preselectedCategory?.type ?: CategoryType.EXPENSE
            }
        )
    }
    
    // Load saved default categories from preferences
    val userPrefs by preferencesRepository.getUserPreferences()
        .collectAsStateWithLifecycle(initialValue = com.andriybobchuk.mooney.mooney.domain.settings.UserPreferences())

    // Auto-select default categories based on transaction type (uses saved preferences)
    val getDefaultCategoryForType: (CategoryType) -> Category? = { type ->
        when (type) {
            CategoryType.EXPENSE -> {
                val savedId = userPrefs.defaultExpenseCategory
                val saved = categories.find { it.id == savedId }
                // If saved is a subcategory, return its parent as "general" (the sheet tracks sub separately)
                if (saved?.isSubCategory() == true) saved.parent else saved
                    ?: categories.find { it.title.contains("Groceries") }
            }
            CategoryType.INCOME -> {
                val savedId = userPrefs.defaultIncomeCategory
                val saved = categories.find { it.id == savedId }
                if (saved?.isSubCategory() == true) saved.parent else saved
                    ?: categories.find { it.id == "salary" }
            }
            CategoryType.TRANSFER -> categories.find { it.id == "internal_transfer" }
        }
    }

    // Also resolve default subcategory from preferences
    val getDefaultSubCategoryForType: (CategoryType) -> Category? = { type ->
        when (type) {
            CategoryType.EXPENSE -> {
                val saved = categories.find { it.id == userPrefs.defaultExpenseCategory }
                if (saved?.isSubCategory() == true) saved else null
            }
            CategoryType.INCOME -> {
                val saved = categories.find { it.id == userPrefs.defaultIncomeCategory }
                if (saved?.isSubCategory() == true) saved else null
            }
            CategoryType.TRANSFER -> null
        }
    }
    
    // Category state management
    val selectedCategory: Category? = if (isEditMode) {
        when {
            transactionToEdit?.subcategory?.isGeneralCategory() == true -> transactionToEdit.subcategory
            transactionToEdit?.subcategory?.isSubCategory() == true -> transactionToEdit.subcategory.parent
            else -> getDefaultCategoryForType(selectedTransactionType)
        }
    } else {
        preselectedCategory ?: getDefaultCategoryForType(selectedTransactionType)
    }
    val selectedSubCategory: Category? = if (transactionToEdit?.subcategory?.isSubCategory() == true) {
        transactionToEdit.subcategory
    } else if (!isEditMode) {
        getDefaultSubCategoryForType(selectedTransactionType)
    } else null
    
    var currentSelectedCategory by remember { mutableStateOf(selectedCategory) }
    var currentSelectedSubCategory by remember { mutableStateOf(selectedSubCategory) }
    var showCategorySheet by remember { mutableStateOf(false) }

    // When preferences load asynchronously, update defaults if user hasn't manually changed them
    var hasUserChangedCategory by remember { mutableStateOf(false) }
    LaunchedEffect(userPrefs.defaultExpenseCategory, userPrefs.defaultIncomeCategory) {
        if (!isEditMode && !hasUserChangedCategory && preselectedCategory == null) {
            currentSelectedCategory = getDefaultCategoryForType(selectedTransactionType)
            currentSelectedSubCategory = getDefaultSubCategoryForType(selectedTransactionType)
        }
    }


    var selectedDate by remember {
        mutableStateOf(
            transactionToEdit?.date ?: run {
                val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                if (currentDate.year == selectedMonth.year && currentDate.monthNumber == selectedMonth.month) {
                    currentDate
                } else {
                    LocalDate(selectedMonth.year, selectedMonth.month, 1)
                }
            }
        )
    }
    var showDateSheet by remember { mutableStateOf(false) }

    // Recurring state
    var isRecurringEnabled by remember { mutableStateOf(forceRecurringEnabled) }
    var recurringSchedule by remember {
        mutableStateOf(
            initialRecurringSchedule ?: RecurringSchedule(
                frequency = RecurringFrequency.MONTHLY,
                dayOfMonth = selectedDate.dayOfMonth
            )
        )
    }
    var showScheduleSheet by remember { mutableStateOf(false) }


    MooneyBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        // Wait for the sheet's slide-in to settle before raising the keyboard.
        // ModalBottomSheet animates ~300ms; raising the keyboard at 100ms means
        // the keyboard's own slide-up animation collides with the sheet's,
        // causing the perceptible jitter when the "+" button is tapped.
        LaunchedEffect(Unit) {
            delay(350)
            focusRequester.requestFocus()
            keyboardController?.show()
        }

        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp)
                .fillMaxSize()
        ) {
            // Scrollable form content
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
            // Triple switch for transaction type
            TransactionTypeSwitch(
                selectedType = selectedTransactionType,
                onTypeSelected = { type ->
                    selectedTransactionType = type
                    // Auto-select appropriate category when type changes
                    currentSelectedCategory = getDefaultCategoryForType(type)
                    currentSelectedSubCategory = getDefaultSubCategoryForType(type)
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            // Detect if reconciliation category is selected
            val isReconciliation = currentSelectedSubCategory?.id?.contains("reconciliation") == true ||
                                   currentSelectedCategory?.id?.contains("reconciliation") == true

            val currencySymbol = selectedAccount?.currency?.symbol ?: ""

            if (isReconciliation && selectedAccount != null) {
                // For reconciliation: show new account value field instead of amount
                AmountHeroField(
                    value = newAccountValue,
                    onValueChange = { newAccountValue = it },
                    currencySymbol = currencySymbol,
                    placeholder = selectedAccount?.amount?.formatWithCommas() ?: "0.00",
                    focusRequester = focusRequester
                )

                // Show current account value and calculated difference
                val reconAccount = selectedAccount
                val currentValue = reconAccount?.amount ?: 0.0
                val newValue = newAccountValue.parseAmountInput() ?: currentValue
                val difference = newValue - currentValue

                if (difference != 0.0 && reconAccount != null) {
                    Text(
                        text = stringResource(Res.string.recon_current_diff, currentValue.formatWithCommas(), currencySymbol, "${if (difference > 0) "+" else ""}${difference.formatWithCommas()}", currencySymbol),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (difference > 0) MaterialTheme.appColors.incomeColor else MaterialTheme.appColors.expenseColor,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Normal amount — hero field
                AmountHeroField(
                    value = amount ?: "",
                    onValueChange = { amount = it },
                    currencySymbol = currencySymbol,
                    placeholder = "0.00",
                    focusRequester = focusRequester
                )
                // Cross-currency transfer — editable destination amount.
                // Auto-fills from today's rate unless the user overrides it
                // (e.g., to enter the bank's actual real-world rate).
                CrossCurrencyTransferField(
                    visible = selectedTransactionType == CategoryType.TRANSFER,
                    sourceCurrency = selectedAccount?.currency,
                    destinationCurrency = destinationAccount?.currency,
                    sourceAmountText = amount,
                    destinationAmountText = destinationAmountText,
                    onDestinationAmountChange = { destinationAmountText = it },
                    userOverrode = userOverrodeDestination,
                    onUserOverrideChange = { userOverrodeDestination = it },
                    rates = currencyManagerUseCase.getCurrentExchangeRates()
                )
            }

            Spacer(Modifier.height(12.dp))

            // For transfers, show both source and destination accounts
            if (selectedTransactionType == CategoryType.TRANSFER) {
                Text(
                    text = stringResource(Res.string.from_account),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                AccountField(selectedAccount, accounts.filterNotNull(), assetCategories, categoryOrder, expandedCategories, onToggleAccountCategory, { selectedAccount = it })

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(Res.string.to_account),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                AccountField(
                    initialSelectedAccount = destinationAccount,
                    accounts = accounts.filterNotNull().filter {
                        accounts.filterNotNull().toAccounts().find { acc -> acc.id == it.id }?.id != selectedAccount?.id
                    },
                    assetCategories = assetCategories,
                    categoryOrder = categoryOrder,
                    expandedCategories = expandedCategories,
                    onToggleCategory = onToggleAccountCategory,
                    onAccountSelected = { destinationAccount = it }
                )
            } else {
                // For expense/income, show single account
                AccountField(selectedAccount, accounts.filterNotNull(), assetCategories, categoryOrder, expandedCategories, onToggleAccountCategory, { selectedAccount = it })
            }

            Spacer(Modifier.height(12.dp))

            // Category display - only show for expense/income
            if (selectedTransactionType != CategoryType.TRANSFER) {
                // For expense/income, allow category selection
                val subCat = currentSelectedSubCategory
                val mainCat = currentSelectedCategory
                val categoryText = when {
                    subCat != null -> "${subCat.resolveEmoji()} ${subCat.title}"
                    mainCat != null -> "${mainCat.emoji ?: ""} ${mainCat.title}"
                    else -> when (selectedTransactionType) {
                        CategoryType.EXPENSE -> "🛒 Groceries & Household"
                        CategoryType.INCOME -> "💸 Salary"
                        else -> stringResource(Res.string.select_category)
                    }
                }
                MooneyButton(
                    text = categoryText,
                    onClick = { showCategorySheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.SECONDARY
                )
            }

            Spacer(Modifier.height(12.dp))

            // Optional description — placed right after Category so a quick
            // note ("Costco run") sits next to what it tags. Single line.
            MooneyTextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(Res.string.tx_description_label),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(12.dp))

            // Date selector + Repeat toggle — compact single row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!forceRecurringEnabled) {
                    // Previous day button
                    IconButton(
                        onClick = {
                            selectedDate = selectedDate.minus(1, DateTimeUnit.DAY)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = com.andriybobchuk.mooney.core.presentation.Icons.ChevronLeftIcon(),
                            contentDescription = stringResource(Res.string.cd_previous_day),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Date button
                    val dayName = selectedDate.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                    val dayNumber = selectedDate.dayOfMonth
                    MooneyButton(
                        text = "$dayName $dayNumber",
                        onClick = { showDateSheet = true },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.SECONDARY
                    )

                    // Next day button
                    IconButton(
                        onClick = {
                            selectedDate = selectedDate.plus(1, DateTimeUnit.DAY)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = com.andriybobchuk.mooney.core.presentation.Icons.ChevronRightIcon(),
                            contentDescription = stringResource(Res.string.cd_next_day),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Repeat/Schedule button — opens schedule sheet
                val repeatLabel = if (isRecurringEnabled) {
                    recurringSchedule.toDisplayString()
                } else {
                    stringResource(Res.string.repeat_question)
                }
                MooneyButton(
                    text = repeatLabel,
                    iconPainter = com.andriybobchuk.mooney.core.presentation.Icons.RefreshIcon(),
                    onClick = { showScheduleSheet = true },
                    modifier = if (forceRecurringEnabled) Modifier.fillMaxWidth() else Modifier,
                    variant = if (isRecurringEnabled) ButtonVariant.PRIMARY else ButtonVariant.SECONDARY
                )
            }

            } // end scrollable Column

            // Validation
            val parsedAmt = (amount ?: "").parseAmountInput() ?: 0.0
            val validation = remember(parsedAmt, selectedAccount, destinationAccount, selectedTransactionType) {
                validateUseCase(parsedAmt, selectedTransactionType, selectedAccount, destinationAccount)
            }

            // Warning: yellow text centered above CTA
            if (validation is TransactionValidation.Warning) {
                Text(
                    text = validation.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD4A017),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
            // Error: red text centered above disabled CTA (only for business rule errors, not missing selections)
            if (validation is TransactionValidation.Error && !validation.message.startsWith("Select") && !validation.message.startsWith("Enter")) {
                Text(
                    text = validation.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }

            // Pinned button — always visible above keyboard
            Spacer(Modifier.height(4.dp))
            MooneyButton(
                text = if (transactionToEdit != null) stringResource(Res.string.update_transaction) else stringResource(Res.string.add_transaction),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                variant = ButtonVariant.PRIMARY,
                enabled = validation !is TransactionValidation.Error,
                onClick = {
                    // Detect if this is a reconciliation transaction
                    val isReconciliation = currentSelectedSubCategory?.id?.contains("reconciliation") == true ||
                                           currentSelectedCategory?.id?.contains("reconciliation") == true
                    
                    val parsedAmount: Double?
                    val finalAmount: Double?
                    
                    val reconAcct = selectedAccount
                    if (isReconciliation && reconAcct != null) {
                        // For reconciliation: use the difference between old and new account values
                        val currentValue = reconAcct.amount
                        val newValue = newAccountValue.parseAmountInput()
                        parsedAmount = newValue
                        finalAmount = if (newValue != null) kotlin.math.abs(newValue - currentValue) else null
                    } else {
                        // For regular transactions: use the amount field
                        parsedAmount = amount?.parseAmountInput()
                        finalAmount = parsedAmount
                    }
                    
                    val finalCategory = if (selectedTransactionType == CategoryType.TRANSFER) {
                        categories.find { it.id == "internal_transfer" }
                    } else {
                        currentSelectedCategory ?: getDefaultCategoryForType(selectedTransactionType)
                    }
                    
                    // Capture locals for smart casting
                    val acct = selectedAccount
                    val destAcct = destinationAccount
                    val fCat = finalCategory
                    val fAmt = finalAmount

                    // Validation logic
                    val isValid = if (selectedTransactionType == CategoryType.TRANSFER) {
                        parsedAmount != null && acct != null && destAcct != null &&
                        fCat != null && acct.id != destAcct.id
                    } else if (isReconciliation) {
                        parsedAmount != null && acct != null && fCat != null &&
                        fAmt != null && fAmt > 0.01
                    } else {
                        parsedAmount != null && acct != null && fCat != null
                    }

                    @Suppress("ComplexCondition")
                    if (isValid && acct != null && fCat != null && fAmt != null) {
                        // Cross-currency transfer: use the destination-amount the
                        // user actually saw in the editable field (either today's
                        // auto-converted rate or their manual override). Falls
                        // back to a fresh conversion if the field happens to be
                        // empty for any reason.
                        val computedDestinationAmount: Double? = if (
                            selectedTransactionType == CategoryType.TRANSFER &&
                            destAcct != null &&
                            destAcct.currency != acct.currency
                        ) {
                            destinationAmountText.parseAmountInput()
                                ?: currencyManagerUseCase.getCurrentExchangeRates()
                                    .convert(fAmt, acct.currency, destAcct.currency)
                        } else null

                        val transaction = Transaction(
                            id = transactionToEdit?.id ?: 0,
                            amount = fAmt,
                            account = if (isReconciliation && parsedAmount != null) {
                                acct.copy(amount = parsedAmount)
                            } else {
                                acct
                            },
                            subcategory = if (selectedTransactionType == CategoryType.TRANSFER && destAcct != null) {
                                fCat.copy(
                                    id = "transfer_to_${destAcct.id}",
                                    title = "Transfer to ${destAcct.title}"
                                )
                            } else {
                                currentSelectedSubCategory ?: fCat
                            },
                            date = selectedDate,
                            destinationAmount = computedDestinationAmount,
                            description = description.trim().takeIf { it.isNotEmpty() }
                        )
                        
                        val schedule = if (isRecurringEnabled) recurringSchedule else null
                        if (isEditMode) {
                            onUpdate(transaction, schedule)
                        } else {
                            onAdd(transaction, schedule)
                        }
                    }
                }
            )
        }
    }

    if (showScheduleSheet) {
        RecurringScheduleSheet(
            onDismiss = { showScheduleSheet = false },
            schedule = recurringSchedule,
            onScheduleSelected = {
                recurringSchedule = it
                isRecurringEnabled = true
                showScheduleSheet = false
            },
            onRemove = if (isRecurringEnabled) {
                {
                    isRecurringEnabled = false
                    showScheduleSheet = false
                }
            } else null
        )
    }

    if (showCategorySheet) {
        CategorySelectionBottomSheet(
            onDismiss = { showCategorySheet = false },
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { true }
            ),
            categories = categories,
            initialSelectedCategory = currentSelectedCategory,
            initialSelectedSubCategory = currentSelectedSubCategory,
            categoryOrder = transactionCategoryOrder,
            onReorder = onTransactionCategoryReorder,
            onCategorySelected = { category, subCategory ->
                currentSelectedCategory = category
                currentSelectedSubCategory = subCategory
                hasUserChangedCategory = true
                showCategorySheet = false
            },
            onEditCategories = onEditCategories
        )
    }

    if (showDateSheet) {
        DateSelectionBottomSheet(
            onDismiss = { showDateSheet = false },
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { true }
            ),
            selectedDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
                showDateSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectionBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    var currentYear by remember { mutableStateOf(selectedDate.year) }
    var currentMonth by remember { mutableStateOf(selectedDate.monthNumber) }

    MooneyBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        // Wrap content rather than fillMaxSize so the sheet sizes to its
        // intrinsic content height. Previously this was fullscreen even though
        // the actual controls only need ~40% of the screen.
        Column(Modifier.padding(20.dp).fillMaxWidth().padding(bottom = 16.dp)) {
            Text(
                text = stringResource(Res.string.select_date),
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Year selector (dropdown as requested)
            YearDropdownSelector(
                selectedYear = currentYear,
                onYearSelected = { 
                    currentYear = it
                    // Adjust day if it's invalid for the new year/month
                    val maxDay = getDaysInMonth(currentYear, currentMonth)
                    if (selectedDate.dayOfMonth > maxDay) {
                        onDateSelected(LocalDate(currentYear, currentMonth, maxDay))
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            // Creative month selector
            MonthSelector(
                selectedMonth = currentMonth,
                onMonthSelected = { 
                    currentMonth = it
                    // Adjust day if it's invalid for the new month
                    val maxDay = getDaysInMonth(currentYear, currentMonth)
                    if (selectedDate.dayOfMonth > maxDay) {
                        onDateSelected(LocalDate(currentYear, currentMonth, maxDay))
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            // Visual calendar day selector
            CalendarDaySelector(
                year = currentYear,
                month = currentMonth,
                selectedDay = if (selectedDate.year == currentYear && selectedDate.monthNumber == currentMonth) 
                    selectedDate.dayOfMonth else null,
                onDaySelected = { day ->
                    onDateSelected(LocalDate(currentYear, currentMonth, day))
                }
            )
        }
    }
}

@Composable
fun YearDropdownSelector(
    selectedYear: Int,
    onYearSelected: (Int) -> Unit
) {
    val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val years = (currentDate.year downTo 2020).toList()

    Text(stringResource(Res.string.year), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(years.size) { index ->
            val year = years[index]
            val isSelected = year == selectedYear
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onYearSelected(year) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MonthSelector(
    selectedMonth: Int,
    onMonthSelected: (Int) -> Unit
) {
    val monthAbbreviations = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    Text(stringResource(Res.string.month), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(12) { index ->
            val month = index + 1
            val isSelected = month == selectedMonth
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onMonthSelected(month) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = monthAbbreviations[index],
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CalendarDaySelector(
    year: Int,
    month: Int,
    selectedDay: Int?,
    onDaySelected: (Int) -> Unit
) {
    // Use remember to cache expensive calculations
    val calendarData = remember(year, month) {
        val daysInMonth = getDaysInMonth(year, month)
        val firstDayOfMonth = LocalDate(year, month, 1)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.ordinal
        val startOffset = firstDayOfWeek
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7
        
        CalendarData(daysInMonth, startOffset, rows)
    }
    
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    Column {
        Text(
            text = stringResource(Res.string.day_label),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Week day headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Calendar grid using cached data
        repeat(calendarData.rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val day = cellIndex - calendarData.startOffset + 1
                    
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..calendarData.daysInMonth) {
                            val isSelected = day == selectedDay
                            val isToday = LocalDate(year, month, day) == today
                            
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onDaySelected(day) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}


fun Int.padZero(): String = this.toString().padStart(2, '0')

fun getDaysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 30 // fallback, should never hit
    }
}

fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

fun LocalDate.getDayOfWeekName(): String {
    return this.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
}

fun formatDayWithWeekday(day: Int, year: Int, month: Int): String {
    val date = LocalDate(year, month, day)
    val dayName = date.getDayOfWeekName()
    return "${day.padZero()} ($dayName)"
}

data class CalendarData(
    val daysInMonth: Int,
    val startOffset: Int,
    val rows: Int
)


/**
 * Editable destination amount for cross-currency transfers. Auto-fills from the
 * live rate whenever the user hasn't manually overridden it. Once the user
 * edits the field, we stop overwriting their value.
 *
 * Renders nothing unless: visible is true, both currencies known and different.
 */
@Composable
private fun CrossCurrencyTransferField(
    visible: Boolean,
    sourceCurrency: Currency?,
    destinationCurrency: Currency?,
    sourceAmountText: String?,
    destinationAmountText: String,
    onDestinationAmountChange: (String) -> Unit,
    userOverrode: Boolean,
    onUserOverrideChange: (Boolean) -> Unit,
    rates: com.andriybobchuk.mooney.mooney.domain.ExchangeRates
) {
    if (!visible) return
    if (sourceCurrency == null || destinationCurrency == null) return
    if (sourceCurrency == destinationCurrency) return

    val sourceAmount = sourceAmountText?.parseAmountInput()
    val perUnit = rates.convert(1.0, sourceCurrency, destinationCurrency)

    // Auto-recompute the destination value while user hasn't typed in it.
    androidx.compose.runtime.LaunchedEffect(sourceAmountText, sourceCurrency, destinationCurrency, userOverrode) {
        if (!userOverrode) {
            val computed = if (sourceAmount != null && sourceAmount > 0.0) {
                rates.convert(sourceAmount, sourceCurrency, destinationCurrency).formatToPlainString()
            } else ""
            if (computed != destinationAmountText) onDestinationAmountChange(computed)
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = stringResource(Res.string.destination_amount_label, destinationCurrency.symbol),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.text.BasicTextField(
                value = destinationAmountText,
                onValueChange = { newValue ->
                    onUserOverrideChange(true)
                    onDestinationAmountChange(newValue)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = destinationCurrency.symbol,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            if (userOverrode) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.reset),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        onUserOverrideChange(false)
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        val hintLabel = if (userOverrode) stringResource(Res.string.custom_rate) else stringResource(Res.string.todays_rate)
        Text(
            text = "$hintLabel · 1 ${sourceCurrency.symbol} = ${perUnit.formatWithCommas()} ${destinationCurrency.symbol}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun AmountHeroField(
    value: String,
    onValueChange: (String) -> Unit,
    currencySymbol: String,
    placeholder: String = "0.00",
    focusRequester: FocusRequester
) {
    val hasValue = value.isNotEmpty()

    // Use TextFieldValue so we can place the caret at the end on first composition
    // (matters when editing an existing transaction — the field opens with the
    // current value, and the user expects to append/edit from the end).
    var textFieldValue by remember {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                text = value,
                selection = androidx.compose.ui.text.TextRange(value.length)
            )
        )
    }
    // Keep the inner state in sync if `value` is reset externally (e.g., parent clears it).
    androidx.compose.runtime.LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = androidx.compose.ui.text.input.TextFieldValue(
                text = value,
                selection = androidx.compose.ui.text.TextRange(value.length)
            )
        }
    }

    val textStyle = MaterialTheme.typography.headlineLarge.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp
    )

    // Auto-size the visible field by measuring the text width and assigning it
    // explicitly. This avoids the BasicTextField + Row(IntrinsicSize)/weight
    // interaction that previously cropped digits as the value grew.
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val measured = remember(value, placeholder, textStyle, hasValue) {
        textMeasurer.measure(
            text = if (hasValue) value else placeholder,
            style = textStyle
        ).size.width
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val fieldWidth = with(density) { (measured + 2).toDp() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.text.BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    if (newValue.text != value) onValueChange(newValue.text)
                },
                textStyle = textStyle.copy(
                    color = if (hasValue) MaterialTheme.colorScheme.onSurface
                    else Color.Transparent,
                    textAlign = TextAlign.End
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .width(fieldWidth),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (!hasValue) {
                            Text(
                                text = placeholder,
                                style = textStyle.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                    textAlign = TextAlign.End
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (currencySymbol.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = currencySymbol,
                    style = textStyle.copy(
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountField(
    initialSelectedAccount: Account?,
    accounts: List<UiAccount>,
    assetCategories: List<com.andriybobchuk.mooney.core.data.database.AssetCategoryEntity>,
    categoryOrder: List<String>,
    expandedCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    onAccountSelected: (Account) -> Unit
) {
    var showAccountSheet by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { showAccountSheet = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = initialSelectedAccount?.title ?: stringResource(Res.string.select_account),
                style = MaterialTheme.typography.bodyLarge
            )
            initialSelectedAccount?.let {
                Text(
                    text = "${it.amount.formatWithCommas()} ${it.currency.symbol}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            painter = com.andriybobchuk.mooney.core.presentation.Icons.ChevronRightIcon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }

    if (showAccountSheet) {
        MooneyBottomSheet(onDismissRequest = { showAccountSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.select_account),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val grouped = remember(accounts) { accounts.groupBy { it.assetCategoryId } }
                val orderedCategoryIds = remember(categoryOrder, grouped) {
                    (categoryOrder + grouped.keys.filter { it !in categoryOrder })
                        .filter { grouped.containsKey(it) }
                }

                LazyColumn {
                    orderedCategoryIds.forEach { categoryId ->
                        val categoryAccounts = grouped[categoryId] ?: return@forEach
                        val categoryInfo = assetCategories.find { it.id == categoryId }
                        val isExpanded = expandedCategories.contains(categoryId)

                        item(key = "header_$categoryId") {
                            AccountCategoryHeader(
                                title = categoryInfo?.title ?: categoryId,
                                color = categoryInfo?.color ?: 0xFF9E9E9E,
                                accountCount = categoryAccounts.size,
                                isExpanded = isExpanded,
                                onToggle = { onToggleCategory(categoryId) }
                            )
                        }

                        if (isExpanded) {
                            items(
                                count = categoryAccounts.size,
                                key = { categoryAccounts[it].id }
                            ) { index ->
                                val account = categoryAccounts[index].toAccount()
                                val isSelected = account.id == initialSelectedAccount?.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 28.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            onAccountSelected(account)
                                            showAccountSheet = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = account.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${account.amount.formatWithCommas()} ${account.currency.symbol}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AccountCategoryHeader(
    title: String,
    color: Long,
    accountCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "arrow rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
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
            Text(
                text = "$accountCount account${if (accountCount != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState,
    categories: List<Category>,
    initialSelectedCategory: Category?,
    initialSelectedSubCategory: Category?,
    onCategorySelected: (Category, Category?) -> Unit,
    categoryOrder: List<String> = emptyList(),
    onReorder: (List<String>) -> Unit = {},
    onEditCategories: () -> Unit = {}
) {
    var selectedTabIndex by remember { 
        mutableStateOf(
            if (initialSelectedCategory?.type == CategoryType.INCOME) 1 else 0
        ) 
    }
    var showSubCategorySheet by remember { mutableStateOf(false) }
    var selectedParentCategory by remember { mutableStateOf<Category?>(null) }

    MooneyBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(Modifier.padding(20.dp).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.select_category),
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp
                )
                TextButton(onClick = { onDismiss(); onEditCategories() }) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(Res.string.add_category))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(stringResource(Res.string.expense) to 0, stringResource(Res.string.income) to 1).forEach { (label, index) ->
                    val isSelected = selectedTabIndex == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surface
                                else Color.Transparent
                            )
                            .clickable { selectedTabIndex = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Filtered + ordered list. Categories in `categoryOrder` come first
            // (in saved order); the rest preserve their natural schema order.
            val filteredCategories by remember(selectedTabIndex, categories, categoryOrder) {
                derivedStateOf {
                    val categoryType = if (selectedTabIndex == 0) CategoryType.EXPENSE else CategoryType.INCOME
                    val pool = categories.filter { it.isGeneralCategory() && it.type == categoryType }
                    val orderIndex = categoryOrder.withIndex().associate { (i, id) -> id to i }
                    pool.sortedWith(
                        compareBy<Category>(
                            { orderIndex[it.id] ?: Int.MAX_VALUE },
                            { pool.indexOf(it) }
                        )
                    )
                }
            }

            // Local mutable list for drag interactions; persists upstream on drop.
            val orderedIds = remember(filteredCategories) {
                androidx.compose.runtime.mutableStateListOf<String>().apply {
                    addAll(filteredCategories.map { it.id })
                }
            }

            val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
                val toKey = to.key as? String ?: return@rememberReorderableLazyListState
                val fromIdx = orderedIds.indexOf(fromKey)
                val toIdx = orderedIds.indexOf(toKey)
                if (fromIdx >= 0 && toIdx >= 0) {
                    val moved = orderedIds.removeAt(fromIdx)
                    orderedIds.add(toIdx, moved)
                }
            }

            LazyColumn(
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items = orderedIds, key = { it }) { id ->
                    val category = filteredCategories.find { it.id == id } ?: return@items
                    ReorderableItem(reorderableState, key = id) { isDragging ->
                        val hasSubCategories = categories.any {
                            it.isSubCategory() && it.parent?.id == category.id
                        }
                        ReorderableCategoryRow(
                            category = category,
                            isSelected = category.id == initialSelectedCategory?.id,
                            isDragging = isDragging,
                            dragHandleModifier = Modifier.draggableHandle(
                                onDragStopped = { onReorder(orderedIds.toList()) }
                            ),
                            onClick = {
                                if (hasSubCategories) {
                                    selectedParentCategory = category
                                    showSubCategorySheet = true
                                } else {
                                    onCategorySelected(category, null)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    val parentCat = selectedParentCategory
    if (showSubCategorySheet && parentCat != null) {
        SubCategorySelectionBottomSheet(
            onDismiss = { showSubCategorySheet = false },
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { true }
            ),
            parentCategory = parentCat,
            categories = categories,
            initialSelectedSubCategory = initialSelectedSubCategory,
            onSubCategorySelected = { subCategory ->
                onCategorySelected(parentCat, subCategory)
                showSubCategorySheet = false
            },
            onParentSelected = {
                onCategorySelected(parentCat, null)
                showSubCategorySheet = false
            }
        )
    }
}

@Composable
fun CategoryCard(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category.emoji ?: "",
            fontSize = 22.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = category.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

/**
 * Reorderable variant of [CategoryCard]. Drag handle on the right; tap anywhere
 * else still selects the category. The handle uses the `draggableHandle` modifier
 * from sh.calvin.reorderable.
 */
@Composable
private fun ReorderableCollectionItemScope.ReorderableCategoryRow(
    category: Category,
    isSelected: Boolean,
    isDragging: Boolean,
    dragHandleModifier: Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category.emoji ?: "",
            fontSize = 22.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = category.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        // Drag handle — three horizontal lines, only this region triggers drag.
        Box(
            modifier = dragHandleModifier
                .size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(2.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubCategorySelectionBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState,
    parentCategory: Category,
    categories: List<Category>,
    initialSelectedSubCategory: Category?,
    onSubCategorySelected: (Category) -> Unit,
    onParentSelected: () -> Unit
) {
    MooneyBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(Modifier.padding(20.dp).fillMaxSize()) {
            Text(
                text = stringResource(Res.string.select_parent_format, parentCategory.title),
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Parent category option
            SubCategoryItem(
                title = "${parentCategory.emoji} ${parentCategory.title}",
                subtitle = stringResource(Res.string.general_label),
                isSelected = initialSelectedSubCategory == null,
                onClick = onParentSelected
            )

            Spacer(Modifier.height(8.dp))

            // Subcategories  
            val subCategories = remember(parentCategory.id) {
                categories.filter { 
                    it.isSubCategory() && it.parent?.id == parentCategory.id 
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(subCategories) { subCategory ->
                    SubCategoryItem(
                        title = "${subCategory.resolveEmoji()} ${subCategory.title}",
                        subtitle = null,
                        isSelected = subCategory.id == initialSelectedSubCategory?.id,
                        onClick = { onSubCategorySelected(subCategory) }
                    )
                }
            }
        }
    }
}

@Composable
fun SubCategoryItem(
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@Composable
fun FrequentCategoriesSection(
    onCategorySelected: (Category) -> Unit
) {
    var frequentCategories by remember { mutableStateOf<List<Category>>(emptyList()) }
    val getMostUsedCategoriesUseCase = org.koin.compose.koinInject<com.andriybobchuk.mooney.mooney.domain.usecase.GetMostUsedCategoriesUseCase>()
    val getCategoriesUseCase = org.koin.compose.koinInject<com.andriybobchuk.mooney.mooney.domain.usecase.GetCategoriesUseCase>()

    LaunchedEffect(Unit) {
        try {
            val categories = getMostUsedCategoriesUseCase(8)
            frequentCategories = if (categories.isEmpty()) {
                getCategoriesUseCase()
                    .filter { !it.isSubCategory() }
                    .take(8)
            } else {
                categories
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            frequentCategories = getCategoriesUseCase()
                .filter { !it.isSubCategory() }
                .take(8)
        }
    }
    
    if (frequentCategories.isNotEmpty()) {
        Column {
            Text(
                text = stringResource(Res.string.frequently_used),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            // Split into 2 rows of 4 categories each
            val firstRow = frequentCategories.take(4)
            val secondRow = frequentCategories.drop(4).take(4)
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // First row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    firstRow.forEach { category ->
                        CategoryChip(
                            category = category,
                            onClick = { onCategorySelected(category) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining space if less than 4 items
                    repeat(4 - firstRow.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                
                // Second row
                if (secondRow.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        secondRow.forEach { category ->
                            CategoryChip(
                                category = category,
                                onClick = { onCategorySelected(category) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if less than 4 items
                        repeat(4 - secondRow.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionPagerView(
    selectedMonth: MonthKey,
    dailyTotals: Map<Int, Double>,
    modifier: Modifier = Modifier,
    onDayClick: (LocalDate) -> Unit = {},
    /**
     * When false (the production default), only the spending-calendar widget
     * is rendered — no pager, no dots indicator, no extra widgets. The other
     * widgets (line chart, currency rates, suggest card) are dev-only and
     * gated behind the Developer Options "Transactions Widget Pager" toggle.
     */
    showAllWidgets: Boolean = false
) {
    // Calendar-only path: render the heatmap at its natural intrinsic
    // height. The pager-mode `.height(200.dp)` was forcing extra vertical
    // space below the grid when the calendar's content rows were shorter
    // than 200dp — exactly the dead band the user wants gone.
    if (!showAllWidgets) {
        SpendingCalendarView(
            selectedMonth = selectedMonth,
            dailyTotals = dailyTotals,
            modifier = modifier.fillMaxWidth(),
            onDayClick = onDayClick
        )
        return
    }

    val pageCount = 4  // calendar, line chart, currency rates, suggest card
    val dataStore = org.koin.compose.koinInject<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>>()

    // Initial page comes from DataStore so we restore the user's last selection
    // across app launches. We need a NULLABLE produceState here — rememberPagerState
    // captures initialPage only at first composition, so if produceState started
    // at 0, the pager would lock at 0 and never honor the saved value once it
    // arrived. Gating the pager on a non-null resolved value fixes that.
    val initialPage by produceState<Int?>(initialValue = null, dataStore) {
        value = try {
            val prefs = dataStore.data.first()
            (prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.LAST_WIDGET_PAGE] ?: 0)
                .coerceIn(0, pageCount - 1)
        } catch (_: Exception) { 0 }
    }

    val resolvedInitialPage = initialPage ?: return Spacer(modifier = modifier.height(200.dp))

    val pagerState = rememberPagerState(initialPage = resolvedInitialPage, pageCount = { pageCount })

    // Persist whenever the user swipes to a different page. drop(1) skips the
    // initial currentPage emission so we don't write the same value we just read.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .drop(1)
            .collect { page ->
                runCatching {
                    dataStore.edit { prefs ->
                        prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.LAST_WIDGET_PAGE] = page
                    }
                }
            }
    }

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(200.dp)
        ) { page ->
            when (page) {
                0 -> SpendingCalendarView(
                    selectedMonth = selectedMonth,
                    onDayClick = onDayClick,
                    dailyTotals = dailyTotals,
                    modifier = Modifier.fillMaxSize()
                )
                1 -> SpendingLineChart(
                    selectedMonth = selectedMonth,
                    dailyTotals = dailyTotals,
                    modifier = Modifier.fillMaxSize()
                )
                2 -> CurrencyRatesWidget(modifier = Modifier.fillMaxSize())
                3 -> SuggestWidgetCard(modifier = Modifier.fillMaxSize())
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .padding(horizontal = 2.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

/**
 * Live exchange-rate snapshot for the user's tracked currencies (excluding the
 * base). One row per currency showing "1 X = Y BASE", typeset large enough to
 * read at a glance. Minimal — no card chrome, no background. Useful for users
 * with foreign-currency accounts who like to time conversions.
 *
 * Falls back to the hardcoded approximations from [GlobalConfig] if the network
 * fetch fails so the widget always renders something rather than a spinner.
 */
@Composable
private fun CurrencyRatesWidget(modifier: Modifier = Modifier) {
    val exchangeRateProvider: com.andriybobchuk.mooney.mooney.domain.ExchangeRateProvider = org.koin.compose.koinInject()
    val getUserCurrenciesUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.GetUserCurrenciesUseCase = org.koin.compose.koinInject()
    val baseCurrency = com.andriybobchuk.mooney.mooney.data.GlobalConfig.baseCurrency

    val userCurrencies by getUserCurrenciesUseCase()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var rates by remember { mutableStateOf<Map<com.andriybobchuk.mooney.mooney.domain.Currency, Double>>(emptyMap()) }

    // Re-fetch when base changes; one-shot per change is enough for a glance widget.
    LaunchedEffect(baseCurrency) {
        val result = exchangeRateProvider.getExchangeRates(baseCurrency)
        rates = when (result) {
            is com.andriybobchuk.mooney.core.domain.Result.Success -> result.data.rates
            is com.andriybobchuk.mooney.core.domain.Result.Error -> emptyMap()
        }
    }

    // Foreign currencies the user actually cares about, in their chosen order.
    val foreignCodes = userCurrencies
        .sortedBy { it.sortOrder }
        .mapNotNull { uc ->
            runCatching { com.andriybobchuk.mooney.mooney.domain.Currency.valueOf(uc.code) }.getOrNull()
        }
        .filter { it != baseCurrency }
        .take(4)

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(Res.string.exchange_rates),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        if (foreignCodes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.add_more_currencies_in_settings),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        } else {
            foreignCodes.forEach { currency ->
                CurrencyRateRow(
                    from = currency,
                    base = baseCurrency,
                    rateToBase = rates[currency]?.let { 1.0 / it }
                )
            }
        }
    }
}

@Composable
private fun CurrencyRateRow(
    from: com.andriybobchuk.mooney.mooney.domain.Currency,
    base: com.andriybobchuk.mooney.mooney.domain.Currency,
    rateToBase: Double?
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compact symbol "chip" — colored text only, no surface behind it.
        Text(
            text = from.symbol,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(28.dp)
        )
        Text(
            text = "1 ${from.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (rateToBase != null && rateToBase.isFinite())
                "${rateToBase.formatRate()} ${base.symbol}"
            else "—",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

private fun Double.formatRate(): String {
    // 4 sig figs for tiny rates (0.04123), 2 decimals for normal ones.
    return when {
        this >= 100.0 -> ((this * 10).toLong() / 10.0).toString()
        this >= 1.0 -> ((this * 100).toLong() / 100.0).toString()
        else -> ((this * 10000).toLong() / 10000.0).toString()
    }
}

/**
 * The last "page" isn't a widget — it's a soft CTA inviting the user to
 * suggest the next widget. Opens the unified FeedbackSheet pre-selected to
 * the WIDGET category so responses land in Firestore with no email round-trip.
 */
@Composable
private fun SuggestWidgetCard(modifier: Modifier = Modifier) {
    var showSheet by remember { mutableStateOf(false) }
    if (showSheet) {
        com.andriybobchuk.mooney.core.feedback.FeedbackSheet(
            onDismiss = { showSheet = false }
        )
    }
    Column(
        modifier = modifier
            .clickable { showSheet = true }
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "💡", fontSize = 28.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.want_more_widgets),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tell me what you'd find useful — I'll build the best ideas in the next release.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(Res.string.tap_to_suggest),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SpendingLineChart(
    selectedMonth: MonthKey,
    dailyTotals: Map<Int, Double>,
    modifier: Modifier = Modifier
) {
    val year = selectedMonth.year
    val month = selectedMonth.month

    val daysInMonth = remember(year, month) {
        getDaysInMonth(year, month)
    }

    val today = remember {
        val now = kotlinx.datetime.Clock.System.now()
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        now
    }
    val todayDay = if (today.year == year && today.monthNumber == month) today.dayOfMonth else null

    val getTransactionsUseCase: GetTransactionsUseCase = org.koin.compose.koinInject()
    val calculateDailyTotalsMapUseCase: CalculateDailyTotalsMapUseCase = org.koin.compose.koinInject()
    val preferencesRepository: com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository = org.koin.compose.koinInject()

    val previousMonthTotals = remember { mutableStateOf(emptyMap<Int, Double>()) }
    var avgTotal by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(selectedMonth) {
        isLoading = true
        try {
            val prefs = preferencesRepository.getCurrentPreferences()
            val excludeTaxes = prefs.excludeTaxesFromTotals
            val allTransactions = getTransactionsUseCase().first().filterNotNull()
            val monthsToAverage = 6
            val allMonthTotals = mutableListOf<Map<Int, Double>>()
            var iterMonth = selectedMonth

            repeat(monthsToAverage) { _ ->
                iterMonth = iterMonth.previousMonth()
                val monthTransactions = allTransactions.filter { tx ->
                    tx.date.year == iterMonth.year && tx.date.monthNumber == iterMonth.month
                }
                allMonthTotals.add(calculateDailyTotalsMapUseCase(monthTransactions, iterMonth, excludeTaxes))
            }

            // Filter outlier months: exclude months where total is > 2x the median
            val monthlyTotals = allMonthTotals.map { it.values.sum() }.sorted()
            val median = if (monthlyTotals.isNotEmpty()) {
                val mid = monthlyTotals.size / 2
                if (monthlyTotals.size % 2 == 0) (monthlyTotals[mid - 1] + monthlyTotals[mid]) / 2
                else monthlyTotals[mid]
            } else 0.0
            val outlierThreshold = median * 2.0

            val filteredMonthTotals = allMonthTotals.filter { monthData ->
                val total = monthData.values.sum()
                total <= outlierThreshold || median == 0.0
            }

            val avgTotals = mutableMapOf<Int, Double>()
            for (day in 1..daysInMonth) {
                val values = filteredMonthTotals.mapNotNull { it[day] }.filter { it > 0 }
                avgTotals[day] = if (values.isNotEmpty()) values.average() else 0.0
            }

            previousMonthTotals.value = avgTotals
            avgTotal = avgTotals.values.sum()
        } catch (_: Exception) {
            previousMonthTotals.value = emptyMap()
            avgTotal = 0.0
        }
        isLoading = false
    }

    val baseCurrency = com.andriybobchuk.mooney.mooney.data.GlobalConfig.baseCurrency

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(Res.string.spending_comparison),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(Res.string.current),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(12.dp, 2.dp)) {
                    drawLine(
                        color = Color.Gray,
                        start = androidx.compose.ui.geometry.Offset(0f, center.y),
                        end = androidx.compose.ui.geometry.Offset(size.width, center.y),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (!isLoading && avgTotal > 0)
                        "${stringResource(Res.string.six_month_avg)} · ${avgTotal.formatToShortString()} ${baseCurrency.symbol}"
                    else
                        stringResource(Res.string.six_month_avg),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(top = 8.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                val currentTotal = dailyTotals.values.sum()
                val previousTotal = previousMonthTotals.value.values.sum()

                if (currentTotal == 0.0 && previousTotal == 0.0) {
                    Text(
                        text = stringResource(Res.string.no_spending_data),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                } else {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val todayMarkerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxTotal = maxOf(currentTotal, previousTotal)

                        if (maxTotal <= 0) return@Canvas

                        val width = size.width
                        val height = size.height
                        val dayWidth = width / daysInMonth

                        // Draw today marker
                        if (todayDay != null) {
                            val todayX = (todayDay - 1) * dayWidth + dayWidth / 2
                            drawLine(
                                color = todayMarkerColor,
                                start = androidx.compose.ui.geometry.Offset(todayX, 0f),
                                end = androidx.compose.ui.geometry.Offset(todayX, height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        val currentPath = Path()
                        val previousPath = Path()

                        var currentCumulative = 0.0
                        var previousCumulative = 0.0
                        var currentStarted = false
                        var previousStarted = false

                        for (day in 1..daysInMonth) {
                            val x = (day - 1) * dayWidth + dayWidth / 2

                            currentCumulative += dailyTotals[day] ?: 0.0
                            val currentY = height - (currentCumulative / maxTotal) * height * 0.8f

                            if (!currentStarted) {
                                currentPath.moveTo(x, currentY.toFloat())
                                currentStarted = true
                            } else {
                                currentPath.lineTo(x, currentY.toFloat())
                            }
                        }

                        for (day in 1..daysInMonth) {
                            val x = (day - 1) * dayWidth + dayWidth / 2

                            previousCumulative += previousMonthTotals.value[day] ?: 0.0
                            val previousY = height - (previousCumulative / maxTotal) * height * 0.8f

                            if (!previousStarted) {
                                previousPath.moveTo(x, previousY.toFloat())
                                previousStarted = true
                            } else {
                                previousPath.lineTo(x, previousY.toFloat())
                            }
                        }

                        drawPath(
                            path = currentPath,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        drawPath(
                            path = previousPath,
                            color = Color.Gray,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpendingCalendarView(
    selectedMonth: MonthKey,
    dailyTotals: Map<Int, Double>,
    modifier: Modifier = Modifier,
    onDayClick: (LocalDate) -> Unit = {}
) {
    val year = selectedMonth.year
    val month = selectedMonth.month
    
    val calendarData = remember(year, month) {
        val daysInMonth = getDaysInMonth(year, month)
        val firstDayOfMonth = LocalDate(year, month, 1)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.ordinal
        val startOffset = firstDayOfWeek
        CalendarData(daysInMonth, startOffset, 0)
    }
    
    // Calculate weekly totals
    val weeklyTotals = remember(dailyTotals, calendarData) {
        val weeks = (calendarData.daysInMonth + calendarData.startOffset + 6) / 7
        (0 until weeks).map { weekIndex ->
            var weekTotal = 0.0
            repeat(7) { dayInWeek ->
                val cellIndex = weekIndex * 7 + dayInWeek
                val day = cellIndex - calendarData.startOffset + 1
                if (day in 1..calendarData.daysInMonth) {
                    weekTotal += dailyTotals[day] ?: 0.0
                }
            }
            weekTotal
        }
    }
    
    // Remove outliers using IQR method for better color distribution
    val maxAmount = remember(dailyTotals) {
        val nonZeroAmounts = dailyTotals.values.filter { it > 0 }.sorted()
        if (nonZeroAmounts.isEmpty()) {
            0.0
        } else if (nonZeroAmounts.size < 4) {
            // Not enough data for IQR, use max
            nonZeroAmounts.maxOrNull() ?: 0.0
        } else {
            val q1Index = (nonZeroAmounts.size * 0.25).toInt()
            val q3Index = (nonZeroAmounts.size * 0.75).toInt()
            val q1 = nonZeroAmounts[q1Index]
            val q3 = nonZeroAmounts[q3Index]
            val iqr = q3 - q1
            val upperBound = q3 + (1.5 * iqr)
            
            // Use the upper bound as max, but ensure it's at least the 90th percentile
            val p90Index = (nonZeroAmounts.size * 0.9).toInt().coerceAtMost(nonZeroAmounts.size - 1)
            val p90 = nonZeroAmounts[p90Index]
            maxOf(upperBound, p90).coerceAtLeast(q3)
        }
    }
    
    Column(modifier = modifier) {
        // Header row with day labels and weekly total header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
            
            // Weekly total header
            Text(
                text = stringResource(Res.string.week_label),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(Modifier.height(4.dp))
        
        val weeks = (calendarData.daysInMonth + calendarData.startOffset + 6) / 7
        repeat(weeks) { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Calendar days
                repeat(7) { col ->
                    val cellIndex = week * 7 + col
                    val day = cellIndex - calendarData.startOffset + 1
                    
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..calendarData.daysInMonth) {
                            val amount = dailyTotals[day] ?: 0.0
                            val intensity = if (maxAmount > 0) (amount / maxAmount).coerceAtMost(1.0) else 0.0

                            // Cell becomes clickable so the user can jump the
                            // transactions list straight to this day's entries
                            // (#30).
                            Card(
                                modifier = Modifier
                                    .aspectRatio(1.5f)
                                    .fillMaxSize()
                                    // clip BEFORE clickable so the ripple
                                    // honors the Card's rounded corners.
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onDayClick(LocalDate(year, month, day)) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(
                                        alpha = 0.3f + (intensity.toFloat() * 0.7f)
                                    ).let { baseColor ->
                                        if (intensity > 0) {
                                            lerp(
                                                baseColor,
                                                MaterialTheme.colorScheme.primary,
                                                intensity.toFloat()
                                            )
                                        } else baseColor
                                    }
                                ),
                                shape = RoundedCornerShape(6.dp),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (intensity > 0.5) 
                                                MaterialTheme.colorScheme.onPrimary 
                                            else 
                                                MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 9.sp
                                        )
                                        if (amount > 0) {
                                            Text(
                                                text = "${amount.toInt()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 7.sp,
                                                lineHeight = 7.sp,
                                                color = if (intensity > 0.5) 
                                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                                else 
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Weekly total column
                val weekTotal = weeklyTotals[week]
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                        if (weekTotal > 0) {
                            Text(
                                text = weekTotal.formatToShortString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "-",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
            }
            if (week < weeks - 1) {
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .padding(2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = category.resolveEmoji(),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = category.title.take(7),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                fontSize = 10.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun TransactionTypeSwitch(
    selectedType: CategoryType,
    onTypeSelected: (CategoryType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            CategoryType.EXPENSE to stringResource(Res.string.expense),
            CategoryType.INCOME to stringResource(Res.string.income),
            CategoryType.TRANSFER to stringResource(Res.string.transfer)
        ).forEach { (type, label) ->
            val isSelected = selectedType == type
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surface
                        else Color.Transparent
                    )
                    .clickable { onTypeSelected(type) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Top-bar tweaks: month selector with arrows + dropdown pill, quick-action
// chips row, year/month picker sheet. All scoped to the Transactions screen
// — the older inline MonthPicker still serves the Analytics screen.
// ──────────────────────────────────────────────────────────────────────────

/**
 * Horizontal row of quick-action chips shown directly below the top bar.
 * Provides one-tap access to the auxiliary screens previously buried behind
 * toolbar icons or the bottom nav (Recurring, Exchange, Goals, Categories).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionChipsRow(
    onRecurringClick: () -> Unit,
    onGoalsClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            QuickActionChip(
                label = stringResource(Res.string.quick_recurring),
                icon = com.andriybobchuk.mooney.core.presentation.Icons.RecurringIcon(),
                onClick = onRecurringClick
            )
        }
        item {
            QuickActionChip(
                label = stringResource(Res.string.goals_chip),
                icon = com.andriybobchuk.mooney.core.presentation.Icons.GoalsIcon(),
                onClick = onGoalsClick
            )
        }
        item {
            QuickActionChip(
                label = stringResource(Res.string.quick_categories),
                icon = com.andriybobchuk.mooney.core.presentation.Icons.CategoriesIcon(),
                onClick = onCategoriesClick
            )
        }
    }
}

/** Single chip — leading icon comes from a project SVG (Painter). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionChip(
    label: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.painter.Painter
) {
    androidx.compose.material3.AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        leadingIcon = {
            // Sized to roughly match the chip label cap-height so the icon
            // reads as part of the text rather than overpowering it.
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        shape = RoundedCornerShape(50),
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = null
    )
}

// MonthSelectorBar / YearMonthPickerSheet / MonthGridCell were extracted to
// com.andriybobchuk.mooney.mooney.presentation.components.MonthSelector so the
// Analytics screen can reuse the exact same affordance.

