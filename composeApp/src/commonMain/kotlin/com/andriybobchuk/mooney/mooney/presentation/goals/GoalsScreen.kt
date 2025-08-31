package com.andriybobchuk.mooney.mooney.presentation.goals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Goal
import com.andriybobchuk.mooney.mooney.domain.usecase.GoalCompletionEstimate
import com.andriybobchuk.mooney.mooney.presentation.formatWithCommas

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    bottomNavbar: @Composable () -> Unit,
    onSettingsClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.primary),
        topBar = {
            Toolbars.Primary(
                title = "Goals",
                scrollBehavior = scrollBehavior,
                actions = listOf(
                    Toolbars.ToolBarAction(
                        icon = Icons.Default.Settings,
                        contentDescription = "Settings",
                        onClick = onSettingsClick
                    )
                )
            )
        },
        bottomBar = bottomNavbar,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAction(GoalsAction.ShowAddGoalSheet) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        }
    ) { paddingValues ->
        // Main content card with rounded top corners
        Column(Modifier.background(MaterialTheme.colorScheme.primary)) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                if (state.goals.isEmpty() && !state.isLoading) {
                    EmptyGoalsState(
                        onAddGoalClick = { viewModel.onAction(GoalsAction.ShowAddGoalSheet) }
                    )
                } else {
                    GoalsContent(
                        goals = state.goals,
                        currentIndex = state.currentGoalIndex,
                        onSwipeToGoal = { index -> viewModel.onAction(GoalsAction.SwipeToGoal(index)) },
                        onEditGoal = { goal -> viewModel.onAction(GoalsAction.EditGoal(goal)) },
                        onDeleteGoal = { goal -> viewModel.onAction(GoalsAction.ShowDeleteDialog(goal)) }
                    )
                }

                // Add/Edit Goal Bottom Sheet
                if (state.showAddGoalSheet) {
                    AddEditGoalBottomSheet(
                        editingGoal = state.editingGoal,
                        onDismiss = { viewModel.onAction(GoalsAction.HideAddGoalSheet) },
                        onSave = { emoji, title, description, amount, currency ->
                            viewModel.onAction(
                                GoalsAction.SaveGoal(emoji, title, description, amount, currency)
                            )
                            keyboardController?.hide()
                        }
                    )
                }

                // Delete Confirmation Dialog
                if (state.showDeleteDialog && state.goalToDelete != null) {
                    val goalToDelete = state.goalToDelete!!
                    DeleteGoalDialog(
                        goal = goalToDelete,
                        onConfirm = { viewModel.onAction(GoalsAction.ConfirmDeleteGoal(goalToDelete.id)) },
                        onDismiss = { viewModel.onAction(GoalsAction.HideDeleteDialog) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GoalsContent(
    goals: List<GoalWithProgress>,
    currentIndex: Int,
    onSwipeToGoal: (Int) -> Unit,
    onEditGoal: (Goal) -> Unit,
    onDeleteGoal: (Goal) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { goals.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        onSwipeToGoal(pagerState.currentPage)
    }

    LaunchedEffect(currentIndex) {
        if (currentIndex != pagerState.currentPage && currentIndex < goals.size) {
            pagerState.animateScrollToPage(currentIndex)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 0.dp, top = 16.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.weight(1f)
        ) { page ->
            GoalCard(
                goalWithProgress = goals[page],
                onEditGoal = onEditGoal,
                onDeleteGoal = onDeleteGoal
            )
        }

        // Page indicator
        if (goals.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(goals.size) { index ->
                    val color = if (index == currentIndex) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    
                    if (index < goals.size - 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalCard(
    goalWithProgress: GoalWithProgress,
    onEditGoal: (Goal) -> Unit,
    onDeleteGoal: (Goal) -> Unit
) {
    val goal = goalWithProgress.goal
    val progress = goalWithProgress.progress
    val estimate = goalWithProgress.completionEstimate

    var showContextMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { showContextMenu = true },
                    onTap = { }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background gradient and decorative elements
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Subtle gradient background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
                                    Color.Transparent
                                ),
                                center = Offset(0.7f, 0.3f),
                                radius = 1000f
                            )
                        )
                )
                
                // Decorative circles
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 40.dp, y = (-20).dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            CircleShape
                        )
                )
                
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-30).dp, y = 30.dp)
                        .background(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f),
                            CircleShape
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Goal Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = goal.emoji,
                        fontSize = 64.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = goal.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Goal Price Tag
                    Text(
                        text = "${goal.targetAmount.toInt().toDouble().formatWithCommas().replace(".0", "")} ${goal.currency.symbol}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Progress Section
                if (progress != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "Progress",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                // Monthly Progress under Progress text, aligned left
                                val monthlyProgressText = if (progress.monthlyProgressPercentage >= 0) {
                                    "+${progress.monthlyProgressPercentage.toString().take(4)}% this month"
                                } else {
                                    "${progress.monthlyProgressPercentage.toString().take(5)}% this month"
                                }
                                
                                Text(
                                    text = monthlyProgressText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (progress.monthlyProgressPercentage >= 0) {
                                        Color(0xFF4CAF50)
                                    } else {
                                        Color(0xFFEF5350)
                                    },
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "${progress.progressPercentage.toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Animated Progress Bar
                        val animatedProgress by animateFloatAsState(
                            targetValue = (progress.progressPercentage / 100).toFloat(),
                            animationSpec = tween(durationMillis = 1000),
                            label = "progress"
                        )

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress Text
                        Text(
                            text = "${progress.savedAmount.toInt().toDouble().formatWithCommas().replace(".0", "")} ${progress.baseCurrency.symbol} saved • " +
                                    "${progress.remainingAmount.toInt().toDouble().formatWithCommas().replace(".0", "")} ${progress.baseCurrency.symbol} to go",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Completion Estimate Card
                estimate?.let { goalEstimate ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(17.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Estimated completion",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            when (goalEstimate) {
                                is GoalCompletionEstimate.EstimatedCompletion -> {
                                    // Format months/years display
                                    val monthsText = when {
                                        goalEstimate.months <= 1 -> "1 month"
                                        goalEstimate.months < 12 -> "${goalEstimate.months} months"
                                        goalEstimate.months == 12 -> "1 year"
                                        goalEstimate.months < 24 -> "1 year ${goalEstimate.months - 12} months"
                                        else -> {
                                            val years = goalEstimate.months / 12
                                            val remainingMonths = goalEstimate.months % 12
                                            if (remainingMonths == 0) "$years years"
                                            else "$years years $remainingMonths months"
                                        }
                                    }
                                    
                                    Text(
                                        text = monthsText,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        text = "${goalEstimate.targetDate.month.name.take(3)} ${goalEstimate.targetDate.year}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = "at current rate of ${goalEstimate.monthlySavingsRate.toInt().toDouble().formatWithCommas
                                            ().replace(".0", "")}/mo",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                is GoalCompletionEstimate.AlreadyCompleted -> {
                                    Text(
                                        text = "Completed! 🎉",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                is GoalCompletionEstimate.CannotEstimate -> {
                                    Text(
                                        text = "Cannot estimate",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        text = "Add more transactions to get an estimate",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Context Menu
            Box(
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            onEditGoal(goal)
                            showContextMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDeleteGoal(goal)
                            showContextMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyGoalsState(
    onAddGoalClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎯",
            fontSize = 72.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "No goals yet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Set your first financial goal and start tracking your progress towards achieving it!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        
        Button(
            onClick = onAddGoalClick,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Create your first goal")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditGoalBottomSheet(
    editingGoal: Goal?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, Currency) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var emoji by remember(editingGoal) { mutableStateOf(editingGoal?.emoji ?: "") }
    var title by remember(editingGoal) { mutableStateOf(editingGoal?.title ?: "") }
    var description by remember(editingGoal) { mutableStateOf(editingGoal?.description ?: "") }
    var targetAmount by remember(editingGoal) { mutableStateOf(editingGoal?.targetAmount?.toString() ?: "") }
    var selectedCurrency by remember(editingGoal) { mutableStateOf(editingGoal?.currency ?: Currency.PLN) }
    var showCurrencyDropdown by remember { mutableStateOf(false) }

    val isFormValid = emoji.isNotBlank() && 
                     title.isNotBlank() && 
                     description.isNotBlank() && 
                     targetAmount.toDoubleOrNull()?.let { it > 0 } == true

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (editingGoal != null) "Edit Goal" else "Create New Goal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = emoji,
                onValueChange = { emoji = it },
                label = { Text("Goal Emoji") },
                placeholder = { Text("🏠 🚗 💻 ✈️") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Goal Title") },
                placeholder = { Text("e.g., New House") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("e.g., Save for a down payment on our dream home") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it },
                    label = { Text("Target Amount") },
                    placeholder = { Text("50000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Box {
                    OutlinedButton(
                        onClick = { showCurrencyDropdown = true },
                        modifier = Modifier.width(100.dp)
                    ) {
                        Text(selectedCurrency.name)
                    }
                    
                    DropdownMenu(
                        expanded = showCurrencyDropdown,
                        onDismissRequest = { showCurrencyDropdown = false }
                    ) {
                        Currency.entries.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text("${currency.name} (${currency.symbol})") },
                                onClick = {
                                    selectedCurrency = currency
                                    showCurrencyDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        val amount = targetAmount.toDoubleOrNull() ?: 0.0
                        onSave(emoji, title, description, amount, selectedCurrency)
                    },
                    enabled = isFormValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (editingGoal != null) "Update" else "Create")
                }
            }
        }
    }
}

@Composable
private fun DeleteGoalDialog(
    goal: Goal,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Goal") },
        text = { 
            Text("Are you sure you want to delete \"${goal.title}\"? This action cannot be undone.") 
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}