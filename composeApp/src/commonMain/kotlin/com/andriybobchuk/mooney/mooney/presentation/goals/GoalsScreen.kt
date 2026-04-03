package com.andriybobchuk.mooney.mooney.presentation.goals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MeshGradientBackground
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyButton
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyTextField
import com.andriybobchuk.mooney.core.presentation.designsystem.components.ButtonVariant
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Goal
import com.andriybobchuk.mooney.mooney.domain.GoalTrackingType
import com.andriybobchuk.mooney.mooney.domain.GoalWithProgress
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import com.andriybobchuk.mooney.mooney.domain.usecase.GoalCompletionEstimate
import com.andriybobchuk.mooney.mooney.domain.usecase.MonthProjection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    onBackClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(Color.Transparent),
        topBar = {
            Toolbars.Primary(
                title = "Goals",
                containerColor = Color.Transparent,
                scrollBehavior = scrollBehavior,
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAction(GoalsAction.ShowAddGoalSheet) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Goal")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (state.goals.isEmpty() && !state.isLoading) {
                MeshGradientBackground()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No goals yet",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Set financial goals and track your progress toward them.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.goals, key = { it.goal.id }) { goalWithProgress ->
                        GoalCard(
                            goalWithProgress = goalWithProgress,
                            accounts = state.accounts,
                            onEdit = { viewModel.onAction(GoalsAction.EditGoal(it)) },
                            onDelete = { viewModel.onAction(GoalsAction.ShowDeleteDialog(it)) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (state.showAddGoalSheet) {
        AddEditGoalSheet(
            editingGoal = state.editingGoal,
            accounts = state.accounts,
            onDismiss = { viewModel.onAction(GoalsAction.HideAddGoalSheet) },
            onSave = { title, targetAmount, currency, trackingType, accountId ->
                viewModel.onAction(
                    GoalsAction.SaveGoal(title, targetAmount, currency, trackingType, accountId)
                )
                keyboardController?.hide()
            }
        )
    }

    if (state.showDeleteDialog && state.goalToDelete != null) {
        val goalToDelete = state.goalToDelete!!
        AlertDialog(
            onDismissRequest = { viewModel.onAction(GoalsAction.HideDeleteDialog) },
            title = { Text("Delete Goal") },
            text = { Text("Delete \"${goalToDelete.title}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(GoalsAction.ConfirmDeleteGoal(goalToDelete.id)) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(GoalsAction.HideDeleteDialog) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GoalCard(
    goalWithProgress: GoalWithProgress,
    accounts: List<Account>,
    onEdit: (Goal) -> Unit,
    onDelete: (Goal) -> Unit
) {
    val goal = goalWithProgress.goal
    val progress = goalWithProgress.progress
    val estimate = goalWithProgress.completionEstimate
    val progressPct = progress?.progressPercentage ?: 0.0
    val baseCurrency = progress?.baseCurrency ?: GlobalConfig.baseCurrency
    val savedAmount = progress?.savedAmount ?: 0.0
    val targetAmount = progress?.targetAmount ?: goal.targetAmount

    var showContextMenu by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = (progressPct / 100f).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(600)
    )

    val trackingLabel = when (goal.trackingType) {
        GoalTrackingType.ACCOUNT -> accounts.find { it.id == goal.accountId }?.title ?: "Account"
        GoalTrackingType.NET_WORTH -> "Net Worth"
        GoalTrackingType.TOTAL_ASSETS -> "Total Assets"
    }

    val isCompleted = progressPct >= 100.0

    val accentColor = MaterialTheme.colorScheme.primary

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onEdit(goal) },
                    onLongClick = { showContextMenu = true }
                ),
            shape = RoundedCornerShape(16.dp),
            color = if (isCompleted) accentColor.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = if (isCompleted) androidx.compose.foundation.BorderStroke(
                1.5.dp, accentColor.copy(alpha = 0.3f)
            ) else null
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header: title + tracking type pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = trackingLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = accentColor,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Amount row: current / target + percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${savedAmount.formatWithCommas()} / ${targetAmount.formatWithCommas()} ${baseCurrency.symbol}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${progressPct.toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) accentColor else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Estimation info
                if (estimate is GoalCompletionEstimate.InProgress) {
                    Spacer(modifier = Modifier.height(12.dp))

                    if (estimate.estimatedMonths != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val label = if (estimate.estimatedCompletionLabel != null) {
                                "Estimated ${estimate.estimatedCompletionLabel}"
                            } else {
                                "~${estimate.estimatedMonths} months left"
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (estimate.monthlySavingsRate > 0) {
                                val pctPerMonth = if (targetAmount > 0) (estimate.monthlySavingsRate / targetAmount * 100).toInt() else 0
                                Text(
                                    text = "+${formatCompact(estimate.monthlySavingsRate)}/mo · ${pctPerMonth}% avg",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = accentColor
                                )
                            }
                        }
                    } else if (estimate.monthlySavingsRate <= 0) {
                        Text(
                            text = "Not enough data to estimate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    // Timeline — show only if <= 6 months to completion
                    if (estimate.timeline.isNotEmpty() && (estimate.estimatedMonths ?: 0) <= 6) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TimelineRow(
                            timeline = estimate.timeline,
                            targetAmount = targetAmount,
                            baseCurrency = baseCurrency
                        )
                    }
                } else if (estimate is GoalCompletionEstimate.AlreadyCompleted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Goal reached!",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = { onEdit(goal); showContextMenu = false },
                leadingIcon = { Icon(Icons.Outlined.Edit, null) }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { onDelete(goal); showContextMenu = false },
                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

private fun formatCompact(amount: Double): String {
    val abs = kotlin.math.abs(amount)
    return when {
        abs >= 1_000_000 -> "${((amount / 1_000_000 * 10).toLong() / 10.0)}M"
        abs >= 1_000 -> "${(amount / 1_000).toLong()}k"
        else -> "${amount.toLong()}"
    }
}

@Composable
private fun TimelineRow(
    timeline: List<MonthProjection>,
    targetAmount: Double,
    baseCurrency: Currency
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(timeline) { month ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(52.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (month.isCompletionMonth) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatCompact(month.projectedAmount),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = if (month.isCompletionMonth) FontWeight.Bold else FontWeight.Normal,
                        color = if (month.isCompletionMonth) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = month.monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = if (month.isCompletionMonth) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = if (month.isCompletionMonth) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditGoalSheet(
    editingGoal: Goal?,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (String, Double, Currency, GoalTrackingType, Int?) -> Unit
) {
    var title by remember(editingGoal) { mutableStateOf(editingGoal?.title ?: "") }
    var targetAmount by remember(editingGoal) { mutableStateOf(editingGoal?.targetAmount?.toString() ?: "") }
    var selectedCurrency by remember(editingGoal) { mutableStateOf(editingGoal?.currency ?: GlobalConfig.baseCurrency) }
    var trackingType by remember(editingGoal) { mutableStateOf(editingGoal?.trackingType ?: GoalTrackingType.NET_WORTH) }
    var selectedAccountId by remember(editingGoal) { mutableStateOf(editingGoal?.accountId) }
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }

    val isFormValid = title.isNotBlank() &&
            targetAmount.toDoubleOrNull()?.let { it > 0 } == true &&
            (trackingType != GoalTrackingType.ACCOUNT || selectedAccountId != null)

    MooneyBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (editingGoal != null) "Edit Goal" else "New Goal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            MooneyTextField(
                value = title,
                onValueChange = { if (it.length <= 40) title = it },
                label = "Title",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            MooneyTextField(
                value = targetAmount,
                onValueChange = { targetAmount = it },
                label = "Target amount",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Currency selector
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
                Icon(painter = com.andriybobchuk.mooney.core.presentation.Icons.ChevronRightIcon(), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tracking type
            Text("Track progress against", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "Account" to GoalTrackingType.ACCOUNT,
                    "Net Worth" to GoalTrackingType.NET_WORTH,
                    "Assets" to GoalTrackingType.TOTAL_ASSETS
                ).forEach { (label, type) ->
                    val isSelected = trackingType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.background else Color.Transparent)
                            .clickable { trackingType = type }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Account selector
            if (trackingType == GoalTrackingType.ACCOUNT) {
                Spacer(modifier = Modifier.height(12.dp))
                val selectedAccount = accounts.find { it.id == selectedAccountId }
                Text("Account", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showAccountSheet = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedAccount?.title ?: "Select account", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Icon(painter = com.andriybobchuk.mooney.core.presentation.Icons.ChevronRightIcon(), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            MooneyButton(
                text = if (editingGoal != null) "Save" else "Create Goal",
                variant = ButtonVariant.PRIMARY,
                onClick = {
                    val amount = targetAmount.toDoubleOrNull() ?: 0.0
                    onSave(title, amount, selectedCurrency, trackingType, selectedAccountId)
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth()
            )
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
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(Currency.entries) { currency ->
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
                            Text(currency.symbol, fontSize = 20.sp, modifier = Modifier.padding(end = 16.dp))
                            Text(currency.name, style = MaterialTheme.typography.bodyLarge, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.weight(1f))
                            if (isSelected) {
                                Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }

    // Account bottom sheet
    if (showAccountSheet) {
        MooneyBottomSheet(onDismissRequest = { showAccountSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text("Select Account", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(accounts.filter { !it.isLiability }) { account ->
                        val isSelected = selectedAccountId == account.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { selectedAccountId = account.id; showAccountSheet = false }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(account.title, style = MaterialTheme.typography.bodyLarge, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                Text("${account.amount.formatWithCommas()} ${account.currency.symbol}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isSelected) {
                                Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
