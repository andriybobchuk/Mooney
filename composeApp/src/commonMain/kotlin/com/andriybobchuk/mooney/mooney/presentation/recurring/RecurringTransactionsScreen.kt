package com.andriybobchuk.mooney.mooney.presentation.recurring

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.app.appColors
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyButton
import com.andriybobchuk.mooney.core.presentation.designsystem.components.ButtonVariant
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.RecurringFrequency
import com.andriybobchuk.mooney.mooney.domain.RecurringSchedule
import com.andriybobchuk.mooney.mooney.domain.RecurringTransaction
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import com.andriybobchuk.mooney.mooney.presentation.transaction.TransactionBottomSheet
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.delete
import mooney.composeapp.generated.resources.delete_recurring_tx
import mooney.composeapp.generated.resources.frequency_label
import mooney.composeapp.generated.resources.month_label
import mooney.composeapp.generated.resources.monthly_label
import mooney.composeapp.generated.resources.no_recurring_yet
import mooney.composeapp.generated.resources.recurring_empty_hint
import mooney.composeapp.generated.resources.recurring_delete_warning
import mooney.composeapp.generated.resources.recurring_summary_daily
import mooney.composeapp.generated.resources.recurring_summary_monthly
import mooney.composeapp.generated.resources.recurring_summary_weekly
import mooney.composeapp.generated.resources.recurring_summary_yearly
import mooney.composeapp.generated.resources.recurring_transactions_title
import mooney.composeapp.generated.resources.remove
import mooney.composeapp.generated.resources.repeat_on
import mooney.composeapp.generated.resources.repeat_on_day
import mooney.composeapp.generated.resources.save
import mooney.composeapp.generated.resources.schedule_label
import mooney.composeapp.generated.resources.weekly_label
import mooney.composeapp.generated.resources.yearly_label
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecurringTransactionsScreen(
    viewModel: RecurringTransactionsViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingRecurring by remember { mutableStateOf<RecurringTransaction?>(null) }
    var deletingRecurringId by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.recurring_transactions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = com.andriybobchuk.mooney.core.presentation.Icons.BackIcon(),
                            contentDescription = "Back",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring")
            }
        }
    ) { paddingValues ->
        if (state.isInitialLoading) {
            // Cold start: row-shaped placeholders. Same shimmer cadence as the
            // other tabs so the loading feel is consistent across the app.
            RecurringTransactionsShimmer(modifier = Modifier.padding(paddingValues))
        } else if (state.recurringTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Text(
                        stringResource(Res.string.no_recurring_yet),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(Res.string.recurring_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(state.recurringTransactions, key = { it.id }) { recurring ->
                    RecurringTransactionItem(
                        recurring = recurring,
                        modifier = Modifier.combinedClickable(
                            onClick = { editingRecurring = recurring },
                            onLongClick = { deletingRecurringId = recurring.id }
                        )
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    // Add sheet
    if (showAddSheet || editingRecurring != null) {
        RecurringTransactionAddSheet(
            accounts = state.accounts,
            categories = state.categories,
            assetCategories = state.assetCategories,
            categoryOrder = state.categoryOrder,
            expandedCategories = state.expandedCategories,
            onToggleAccountCategory = viewModel::toggleAccountCategoryExpansion,
            selectedMonth = MonthKey.current(),
            editingRecurring = editingRecurring,
            onDismiss = {
                showAddSheet = false
                editingRecurring = null
            },
            onAddWithRecurring = { transaction, schedule ->
                showAddSheet = false
                editingRecurring = null
                viewModel.addWithRecurring(transaction, schedule)
            },
            onSaveRecurring = { recurring ->
                showAddSheet = false
                editingRecurring = null
                viewModel.saveRecurring(recurring)
            }
        )
    }

    // Delete confirmation
    deletingRecurringId?.let { id ->
        MooneyBottomSheet(onDismissRequest = { deletingRecurringId = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.delete_recurring_tx),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = stringResource(Res.string.recurring_delete_warning),
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
                            viewModel.deleteRecurring(id)
                            deletingRecurringId = null
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

@Composable
private fun RecurringTransactionItem(
    recurring: RecurringTransaction,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.appColors.transactionIcon),
            contentAlignment = Alignment.Center
        ) {
            Text(
                recurring.subcategory?.resolveEmoji() ?: "",
                fontSize = 25.sp
            )
        }

        Spacer(Modifier.width(11.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                recurring.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                recurring.schedule.toDisplayString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${recurring.amount.formatWithCommas()} ${recurring.account?.currency?.symbol ?: ""}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.5.sp
                ),
                color = when (recurring.subcategory?.type) {
                    CategoryType.INCOME -> MaterialTheme.appColors.incomeColor
                    CategoryType.EXPENSE -> MaterialTheme.appColors.expenseColor
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                recurring.account?.title ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringTransactionAddSheet(
    accounts: List<com.andriybobchuk.mooney.mooney.domain.AccountWithConversion?>,
    categories: List<com.andriybobchuk.mooney.mooney.domain.Category>,
    assetCategories: List<com.andriybobchuk.mooney.core.data.database.AssetCategoryEntity>,
    categoryOrder: List<String>,
    expandedCategories: Set<String>,
    onToggleAccountCategory: (String) -> Unit,
    selectedMonth: MonthKey,
    editingRecurring: RecurringTransaction?,
    onDismiss: () -> Unit,
    onAddWithRecurring: (com.andriybobchuk.mooney.mooney.domain.Transaction, RecurringSchedule) -> Unit,
    onSaveRecurring: (RecurringTransaction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Convert editing recurring to a Transaction for the bottom sheet
    val transactionToEdit = editingRecurring?.let { rec ->
        if (rec.account != null && rec.subcategory != null) {
            com.andriybobchuk.mooney.mooney.domain.Transaction(
                id = 0,
                subcategory = rec.subcategory,
                amount = rec.amount,
                account = rec.account
            )
        } else null
    }

    TransactionBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        transactionToEdit = transactionToEdit,
        accounts = accounts,
        categories = categories,
        // Pass through so the account picker can render real category titles
        // and the user can expand/collapse groups (#29).
        assetCategories = assetCategories,
        categoryOrder = categoryOrder,
        expandedCategories = expandedCategories,
        onToggleAccountCategory = onToggleAccountCategory,
        selectedMonth = selectedMonth,
        forceRecurringEnabled = true,
        initialRecurringSchedule = editingRecurring?.schedule,
        onAdd = { transaction, schedule ->
            if (schedule != null) {
                if (editingRecurring != null) {
                    onSaveRecurring(
                        editingRecurring.copy(
                            title = transaction.subcategory.title,
                            subcategory = transaction.subcategory,
                            amount = transaction.amount,
                            account = transaction.account,
                            schedule = schedule
                        )
                    )
                } else {
                    onAddWithRecurring(transaction, schedule)
                }
            }
        },
        onUpdate = { transaction, schedule ->
            if (schedule != null && editingRecurring != null) {
                onSaveRecurring(
                    editingRecurring.copy(
                        title = transaction.subcategory.title,
                        subcategory = transaction.subcategory,
                        amount = transaction.amount,
                        account = transaction.account,
                        schedule = schedule
                    )
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScheduleSheet(
    onDismiss: () -> Unit,
    schedule: RecurringSchedule,
    onScheduleSelected: (RecurringSchedule) -> Unit,
    onRemove: (() -> Unit)? = null
) {
    var frequency by remember { mutableStateOf(schedule.frequency) }
    var dayOfMonth by remember { mutableStateOf(schedule.dayOfMonth) }
    var weekDay by remember { mutableStateOf(schedule.weekDay ?: 0) }
    var monthOfYear by remember { mutableStateOf(schedule.monthOfYear ?: 1) }

    MooneyBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                stringResource(Res.string.schedule_label),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            // Frequency chips
            Text(
                stringResource(Res.string.frequency_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    RecurringFrequency.MONTHLY to stringResource(Res.string.monthly_label),
                    RecurringFrequency.WEEKLY to stringResource(Res.string.weekly_label),
                    RecurringFrequency.YEARLY to stringResource(Res.string.yearly_label)
                ).forEach { (freq, label) ->
                    FilterChip(
                        selected = frequency == freq,
                        onClick = { frequency = freq },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Day picker — which day of the cycle the next transaction will be created
            when (frequency) {
                RecurringFrequency.MONTHLY -> {
                    Text(
                        stringResource(Res.string.repeat_on_day),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    DayOfMonthGrid(
                        selectedDay = dayOfMonth,
                        onDaySelected = { dayOfMonth = it }
                    )
                }
                RecurringFrequency.WEEKLY -> {
                    Text(
                        stringResource(Res.string.repeat_on),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    WeekDayChips(
                        selectedDay = weekDay,
                        onDaySelected = { weekDay = it }
                    )
                }
                RecurringFrequency.YEARLY -> {
                    Text(
                        stringResource(Res.string.month_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    MonthChips(
                        selectedMonth = monthOfYear,
                        onMonthSelected = { monthOfYear = it }
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Day",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    DayOfMonthGrid(
                        selectedDay = dayOfMonth,
                        onDaySelected = { dayOfMonth = it }
                    )
                }
                RecurringFrequency.DAILY -> {
                    // No additional configuration needed
                }
            }

            Spacer(Modifier.height(16.dp))

            // Preview of what will happen
            val summaryText = when (frequency) {
                RecurringFrequency.MONTHLY -> stringResource(Res.string.recurring_summary_monthly, dayOfMonth)
                RecurringFrequency.WEEKLY -> stringResource(Res.string.recurring_summary_weekly)
                RecurringFrequency.YEARLY -> stringResource(Res.string.recurring_summary_yearly, monthOfYear, dayOfMonth)
                RecurringFrequency.DAILY -> stringResource(Res.string.recurring_summary_daily)
            }
            Text(
                summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onRemove != null) {
                    MooneyButton(
                        text = stringResource(Res.string.remove),
                        onClick = onRemove,
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.SECONDARY
                    )
                }
                MooneyButton(
                    text = stringResource(Res.string.save),
                    onClick = {
                        onScheduleSelected(
                            RecurringSchedule(
                                frequency = frequency,
                                dayOfMonth = dayOfMonth,
                                weekDay = if (frequency == RecurringFrequency.WEEKLY) weekDay else null,
                                monthOfYear = if (frequency == RecurringFrequency.YEARLY) monthOfYear else null
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    variant = ButtonVariant.PRIMARY
                )
            }
        }
    }
}

@Composable
private fun DayOfMonthGrid(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    val days = (1..28).toList()
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.height(172.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(days.size) { index ->
            val day = days[index]
            val isSelected = day == selectedDay
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onDaySelected(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    day.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekDayChips(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEachIndexed { index, label ->
            FilterChip(
                selected = selectedDay == index,
                onClick = { onDaySelected(index) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthChips(
    selectedMonth: Int,
    onMonthSelected: (Int) -> Unit
) {
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        months.forEachIndexed { index, label ->
            FilterChip(
                selected = selectedMonth == index + 1,
                onClick = { onMonthSelected(index + 1) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

/** Cold-start placeholder for the recurring-transactions list. */
@Composable
private fun RecurringTransactionsShimmer(modifier: Modifier = Modifier) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "recShimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "recShimmerAlpha"
    )
    val barColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = 0.08f * (alpha * 2f).coerceAtMost(1f)
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .background(barColor)
            )
        }
    }
}
