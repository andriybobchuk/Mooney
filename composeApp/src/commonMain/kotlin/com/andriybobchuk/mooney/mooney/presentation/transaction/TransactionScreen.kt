package com.andriybobchuk.mooney.mooney.presentation.transaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.app.appColors
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.AssetCategory
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.presentation.account.UiAccount
import com.andriybobchuk.mooney.mooney.presentation.account.toAccounts
import com.andriybobchuk.mooney.mooney.presentation.analytics.MonthPicker
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyButton
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyTextField
import com.andriybobchuk.mooney.core.presentation.designsystem.components.ButtonVariant
import com.andriybobchuk.mooney.core.presentation.designsystem.components.FeedbackBottomSheet
import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import com.andriybobchuk.mooney.mooney.domain.formatToShortString
import com.andriybobchuk.mooney.mooney.domain.formatToPlainString
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionViewModel = koinViewModel(),
    bottomNavbar: @Composable () -> Unit,
    onSettingsClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val transactions = state.transactions
    val total = state.total
    val totalCurrency = state.totalCurrency
    val frequentCategories by viewModel.frequentCategories.collectAsState()
    var showFeedbackSheet by remember { mutableStateOf(false) }
    // Sheet
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isBottomSheetOpen by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var preselectedCategory by remember { mutableStateOf<Category?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        topBar = {
            Toolbars.Primary(
                titleContent = {
                    Column(
                        modifier = Modifier.clickable { viewModel.onTotalCurrencyClick() }
                    ) {
                        Text(
                            text = "${total.formatWithCommas()} ${totalCurrency.symbol}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        )
                        Text(
                            text = "Spent in ${state.selectedMonth.toDisplayString().substringBeforeLast(' ')}",
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
                    MonthPicker(
                        selectedMonth = state.selectedMonth,
                        onMonthSelected = viewModel::onMonthSelected,
                    )
                },
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
        bottomBar = { bottomNavbar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    preselectedCategory = null
                    isBottomSheetOpen = true
                },
                content = {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                },
                contentColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.primary
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
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
                            onCurrencyClick = viewModel::onTotalCurrencyClick,
                            onEdit = {
                                transactionToEdit = it
                                isBottomSheetOpen = true
                            },
                            onDelete = viewModel::deleteTransaction,
                            onDailyTotal = viewModel::getDailyTotal,
                            onAddTransaction = {
                                preselectedCategory = null
                                isBottomSheetOpen = true
                            },
                        )
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
                    preselectedCategory = preselectedCategory,
                    onAdd = {
                        isBottomSheetOpen = false
                        transactionToEdit = null
                        viewModel.upsertTransaction(it)
                    },
                    onUpdate = {
                        isBottomSheetOpen = false
                        viewModel.upsertTransaction(it)
                    }
                )
            }

            /* Pending transactions not implemented yet
            if (false && state.showAddTransactionDialog && state.editingPendingTransaction != null) {
                val pending = state.editingPendingTransaction!!
                // Create a fake transaction object with pending transaction data for pre-filling
                val pendingAccount = state.accounts.filterNotNull().find { it.id == pending.accountId }
                val pendingCategory = state.categories.find { it.id == pending.subcategoryId }
                
                val fakeTransactionForEdit = if (pendingAccount != null && pendingCategory != null) {
                    Transaction(
                        id = 0, // Temporary ID
                        amount = pending.amount,
                        account = Account(
                            id = pendingAccount.id,
                            title = pendingAccount.title,
                            amount = pendingAccount.originalAmount,
                            currency = pendingAccount.originalCurrency,
                            emoji = pendingAccount.emoji,
                            assetCategory = AssetCategory.BANK_ACCOUNT // Default since UiAccount doesn't have this
                        ),
                        subcategory = pendingCategory,
                        date = pending.scheduledDate
                    )
                } else null
                
                TransactionBottomSheet(
                    onDismiss = {
                        // Clear the pending transaction edit state
                        // viewModel.hideAddTransactionDialog() // Not implemented
                    },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    transactionToEdit = fakeTransactionForEdit,
                    accounts = state.accounts,
                    categories = state.categories,
                    selectedMonth = state.selectedMonth,
                    preselectedCategory = null, // Let the transaction data pre-fill everything
                    onAdd = { transaction ->
                        // Reject the original pending transaction and add the new modified one
                        // viewModel.rejectPendingTransaction(pending.id) // Not implemented
                        viewModel.upsertTransaction(transaction)
                        // viewModel.hideAddTransactionDialog() // Not implemented
                    },
                    onUpdate = { transaction ->
                        // Treat update same as add for pending transactions
                        // viewModel.rejectPendingTransaction(pending.id) // Not implemented
                        viewModel.upsertTransaction(transaction)
                        // viewModel.hideAddTransactionDialog() // Not implemented
                    }
                )
            }
            */

    if (showFeedbackSheet) {
        FeedbackBottomSheet(onDismiss = { showFeedbackSheet = false })
    }
}

fun LocalDate.formatForDisplay(): String {
    val dayOfWeek = this.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val month = this.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val day = this.dayOfMonth
    return "$dayOfWeek, $month $day" // e.g., "Sun, Aug 23"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreenContent(
    modifier: Modifier,
    transactions: List<Transaction?>,
    accounts: List<UiAccount?>,
    total: Double,
    currency: Currency,
    selectedMonth: MonthKey,
    dailyTotals: Map<Int, Double> = emptyMap(),
    onCurrencyClick: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Int) -> Unit,
    onDailyTotal: (LocalDate) -> Double = { 0.0 },
    onAddTransaction: () -> Unit = {}
) {
    // Group and sort transactions by date (descending), then by ID (most recent first)
    val grouped = transactions.filterNotNull().groupBy { it.date }
    val sortedGroups = grouped.entries
        .sortedByDescending { it.key }
        .map { (date, transactions) ->
            date to transactions.sortedByDescending { it.id }
        }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            TransactionPagerView(
                selectedMonth = selectedMonth,
                dailyTotals = dailyTotals,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        if (sortedGroups.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp, horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "\uD83D\uDCDD", fontSize = 64.sp, modifier = Modifier.padding(bottom = 16.dp))
                    Text(
                        text = "No transactions yet",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start tracking your spending by adding your first transaction for this month.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    MooneyButton(
                        text = "Add Transaction",
                        onClick = onAddTransaction,
                        variant = ButtonVariant.PRIMARY
                    )
                }
            }
        }

        sortedGroups.forEach { (date, txList) ->
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

            items(txList) { tx ->
                var expanded by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier.combinedClickable(
                        onClick = { onEdit(tx) },
                        onLongClick = { expanded = true }
                    )) {
                    TransactionItem(tx, accounts)
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                expanded = false
                                onEdit(tx)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                expanded = false
                                onDelete(tx.id)
                            }
                        )
                    }
                }
            }
        }
    }
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
                .background(MaterialTheme.appColors.transactionIcon),
            contentAlignment = Alignment.Center
        ) {
            Text(transaction.subcategory.resolveEmoji(), fontSize = 25.sp)
        }

        Spacer(Modifier.width(11.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (transaction.subcategory.type == CategoryType.TRANSFER) {
                // For transfers: show "Internal Transfer" as title
                Text(
                    "Internal Transfer",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal, fontSize = 15.sp)
                )
                // Extract destination account from category ID and show "[Account1] to [Account2]"
                val destinationAccountId = transaction.subcategory.id.removePrefix("transfer_to_").toIntOrNull()
                val destinationAccount = accounts.find { it?.id == destinationAccountId }
                val destinationAccountTitle = destinationAccount?.title ?: "Unknown"
                Text(
                    "${transaction.account.title} to $destinationAccountTitle",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            } else {
                // For regular transactions: show category title
                Text(
                    transaction.subcategory.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal, fontSize = 15.sp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState,
    transactionToEdit: Transaction?,
    accounts: List<UiAccount?>,
    categories: List<Category>,
    selectedMonth: MonthKey,
    preselectedCategory: Category? = null,
    onAdd: (Transaction) -> Unit,
    onUpdate: (Transaction) -> Unit,
) {
    val isEditMode = transactionToEdit != null

    var amount by remember { mutableStateOf(transactionToEdit?.amount?.formatToPlainString()) }
    var newAccountValue by remember { mutableStateOf("") }

    val defaultAccount = accounts.filterNotNull().toAccounts().find { it.title.contains("Primary") }
    var selectedAccount by remember { mutableStateOf(transactionToEdit?.account ?: defaultAccount) }
    
    // For edit mode transfers, extract destination account from category ID
    val initialDestinationAccount = if (isEditMode && transactionToEdit?.subcategory?.type == CategoryType.TRANSFER) {
        val destinationAccountId = transactionToEdit.subcategory.id.removePrefix("transfer_to_").toIntOrNull()
        destinationAccountId?.let { id ->
            accounts.filterNotNull().toAccounts().find { it.id == id }
        }
    } else null
    
    var destinationAccount by remember { mutableStateOf(initialDestinationAccount) }
    
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
    
    // Auto-select default categories based on transaction type
    val getDefaultCategoryForType: (CategoryType) -> Category? = { type ->
        when (type) {
            CategoryType.EXPENSE -> categories.find { it.title.contains("Groceries") }
            CategoryType.INCOME -> categories.find { it.id == "salary" }
            CategoryType.TRANSFER -> categories.find { it.id == "internal_transfer" }
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
    val selectedSubCategory: Category? = if (transactionToEdit?.subcategory?.isSubCategory() == true) transactionToEdit.subcategory else null
    
    var currentSelectedCategory by remember { mutableStateOf(selectedCategory) }
    var currentSelectedSubCategory by remember { mutableStateOf(selectedSubCategory) }
    var showCategorySheet by remember { mutableStateOf(false) }


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


    MooneyBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp).fillMaxSize()) {
            Text(
                text = if (isEditMode) "Edit This Transaction" else "Add New Transaction",
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Triple switch for transaction type
            TransactionTypeSwitch(
                selectedType = selectedTransactionType,
                onTypeSelected = { type ->
                    selectedTransactionType = type
                    // Auto-select appropriate category when type changes
                    currentSelectedCategory = getDefaultCategoryForType(type)
                    currentSelectedSubCategory = null
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            // Detect if reconciliation category is selected
            val isReconciliation = currentSelectedSubCategory?.id?.contains("reconciliation") == true ||
                                   currentSelectedCategory?.id?.contains("reconciliation") == true
            
            if (isReconciliation && selectedAccount != null) {
                // For reconciliation: show new account value field instead of amount
                MooneyTextField(
                    value = newAccountValue,
                    onValueChange = { newAccountValue = it },
                    label = "New Account Value",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
                
                // Show current account value and calculated difference
                val currentValue = selectedAccount!!.amount
                val newValue = newAccountValue.replace(",", "").toDoubleOrNull() ?: currentValue
                val difference = newValue - currentValue
                
                if (difference != 0.0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Current value: ${currentValue.formatWithCommas()} ${selectedAccount!!.currency.symbol}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Difference: ${if (difference > 0) "+" else ""}${difference.formatWithCommas()} ${selectedAccount!!.currency.symbol}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (difference > 0) MaterialTheme.appColors.incomeColor else MaterialTheme.appColors.expenseColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Normal amount field for regular transactions
                MooneyTextField(
                    value = amount ?: "",
                    onValueChange = { amount = it },
                    label = "Amount",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
            }

            Spacer(Modifier.height(12.dp))

            // For transfers, show both source and destination accounts
            if (selectedTransactionType == CategoryType.TRANSFER) {
                Text(
                    text = "From Account",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                AccountField(selectedAccount, accounts.filterNotNull(), { selectedAccount = it })

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "To Account",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                AccountField(
                    destinationAccount, 
                    accounts.filterNotNull().filter { 
                        accounts.filterNotNull().toAccounts().find { acc -> acc.id == it.id }?.id != selectedAccount?.id 
                    },
                    { destinationAccount = it }
                )
            } else {
                // For expense/income, show single account
                AccountField(selectedAccount, accounts.filterNotNull(), { selectedAccount = it })
            }
            
            // Category display - only show for expense/income
            if (selectedTransactionType != CategoryType.TRANSFER) {
                // For expense/income, allow category selection
                val categoryText = when {
                    currentSelectedSubCategory != null -> "${currentSelectedSubCategory!!.resolveEmoji()} ${currentSelectedSubCategory!!.title}"
                    currentSelectedCategory != null -> "${currentSelectedCategory!!.emoji ?: ""} ${currentSelectedCategory!!.title}"
                    else -> when (selectedTransactionType) {
                        CategoryType.EXPENSE -> "🛒 Groceries & Household"
                        CategoryType.INCOME -> "💸 Salary"
                        else -> "Select Category"
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

            // Date selector with navigation arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous day button
                IconButton(
                    onClick = { 
                        selectedDate = selectedDate.minus(1, DateTimeUnit.DAY)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous day"
                    )
                }
                
                // Date button (expanded to fill remaining space)
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
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next day"
                    )
                }
            }



            Spacer(Modifier.height(16.dp))

            MooneyButton(
                text = if (transactionToEdit != null) "Update Transaction" else "Add Transaction",
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.PRIMARY,
                onClick = {
                    // Detect if this is a reconciliation transaction
                    val isReconciliation = currentSelectedSubCategory?.id?.contains("reconciliation") == true ||
                                           currentSelectedCategory?.id?.contains("reconciliation") == true
                    
                    val parsedAmount: Double?
                    val finalAmount: Double?
                    
                    if (isReconciliation && selectedAccount != null) {
                        // For reconciliation: use the difference between old and new account values
                        val currentValue = selectedAccount!!.amount
                        val newValue = newAccountValue.replace(",", "").toDoubleOrNull()
                        parsedAmount = newValue
                        finalAmount = if (newValue != null) kotlin.math.abs(newValue - currentValue) else null
                    } else {
                        // For regular transactions: use the amount field
                        parsedAmount = amount?.toDoubleOrNull()
                        finalAmount = parsedAmount
                    }
                    
                    val finalCategory = if (selectedTransactionType == CategoryType.TRANSFER) {
                        categories.find { it.id == "internal_transfer" }
                    } else {
                        currentSelectedCategory ?: getDefaultCategoryForType(selectedTransactionType)
                    }
                    
                    // Validation logic
                    val isValid = if (selectedTransactionType == CategoryType.TRANSFER) {
                        parsedAmount != null && selectedAccount != null && destinationAccount != null && 
                        finalCategory != null && selectedAccount!!.id != destinationAccount!!.id
                    } else if (isReconciliation) {
                        // For reconciliation: validate new account value is provided and different from current
                        parsedAmount != null && selectedAccount != null && finalCategory != null &&
                        finalAmount != null && finalAmount > 0.01 // Ensure meaningful difference
                    } else {
                        // For regular transactions
                        parsedAmount != null && selectedAccount != null && finalCategory != null
                    }
                    
                    if (isValid) {
                        val transaction = Transaction(
                            id = transactionToEdit?.id ?: 0,
                            amount = finalAmount!!,
                            account = if (isReconciliation) {
                                // For reconciliation: update account to new value
                                selectedAccount!!.copy(amount = parsedAmount!!)
                            } else {
                                selectedAccount!!
                            },
                            subcategory = if (selectedTransactionType == CategoryType.TRANSFER) {
                                // Create a dynamic category that stores the destination account ID
                                finalCategory!!.copy(
                                    id = "transfer_to_${destinationAccount!!.id}",
                                    title = "Transfer to ${destinationAccount!!.title}"
                                )
                            } else {
                                currentSelectedSubCategory ?: finalCategory!!
                            },
                            date = selectedDate
                        )
                        
                        if (isEditMode) {
                            onUpdate(transaction)
                        } else {
                            onAdd(transaction)
                        }
                    }
                }
            )
        }
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
            onCategorySelected = { category, subCategory ->
                currentSelectedCategory = category
                currentSelectedSubCategory = subCategory
                showCategorySheet = false
            }
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
        Column(Modifier.padding(20.dp).fillMaxSize()) {
            Text(
                text = "Select Date",
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
    var expanded by remember { mutableStateOf(false) }
    val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val years = (2000..currentDate.year).toList().reversed()

    Box {
        MooneyButton(
            text = "Year: $selectedYear",
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.SECONDARY
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            years.forEach { year ->
                DropdownMenuItem(
                    text = { Text(year.toString()) },
                    onClick = {
                        onYearSelected(year)
                        expanded = false
                    }
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
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val monthAbbreviations = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    val listState = rememberLazyListState()
    
    // Auto-scroll to center the selected month
    LaunchedEffect(selectedMonth) {
        val targetIndex = selectedMonth - 1
        // Center the selected month by scrolling to position it in the middle
        val offset = -500 // Adjust to center the item (card width + spacing)
        listState.animateScrollToItem(
            index = maxOf(0, targetIndex),
            scrollOffset = offset
        )
    }

    Column {
        Text(
            text = "Month:",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Creative horizontal scrollable month selector
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(12) { index ->
                val month = index + 1
                val isSelected = month == selectedMonth
                
                Card(
                    modifier = Modifier
                        .clickable { onMonthSelected(month) }
                        .width(60.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isSelected) 8.dp else 2.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = monthAbbreviations[index],
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = month.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
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
            text = "Day:",
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
                            
                            Card(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { onDaySelected(day) },
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (isSelected || isToday) 4.dp else 1.dp
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
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


@Composable
private fun AccountField(
    initialSelectedAccount: Account?,
    accounts: List<UiAccount>,
    onAccountSelected: (Account) -> Unit
) {
    var optionsExpanded by remember { mutableStateOf(false) }
    MooneyButton(
        text = initialSelectedAccount?.let { "${it.emoji} ${it.title} (${it.amount.formatWithCommas()} ${it.currency.symbol})" }
            ?: "Select Account",
        onClick = { optionsExpanded = true },
        modifier = Modifier.fillMaxWidth(),
        variant = ButtonVariant.SECONDARY
    )

    DropdownMenu(
        expanded = optionsExpanded,
        onDismissRequest = { optionsExpanded = false }
    ) {
        accounts.toAccounts().forEach { account ->
            DropdownMenuItem(
                text = { Text("${account.emoji} ${account.title} (${account.amount.formatWithCommas()} ${account.currency.symbol})") },
                onClick = {
                    onAccountSelected(account)
                    optionsExpanded = false
                }
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
    onCategorySelected: (Category, Category?) -> Unit
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
            Text(
                text = "Select Category",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Expense") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Income") }
                )
            }

            Spacer(Modifier.height(16.dp))

            val filteredCategories by remember {
                derivedStateOf {
                    val categoryType = if (selectedTabIndex == 0) CategoryType.EXPENSE else CategoryType.INCOME
                    categories.filter { 
                        it.isGeneralCategory() && it.type == categoryType 
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredCategories) { category ->
                    CategoryCard(
                        category = category,
                        isSelected = category.id == initialSelectedCategory?.id,
                        onClick = {
                            val hasSubCategories = categories.any { 
                                it.isSubCategory() && it.parent?.id == category.id 
                            }
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

    if (showSubCategorySheet && selectedParentCategory != null) {
        SubCategorySelectionBottomSheet(
            onDismiss = { showSubCategorySheet = false },
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { true }
            ),
            parentCategory = selectedParentCategory!!,
            categories = categories,
            initialSelectedSubCategory = initialSelectedSubCategory,
            onSubCategorySelected = { subCategory ->
                onCategorySelected(selectedParentCategory!!, subCategory)
                showSubCategorySheet = false
            },
            onParentSelected = {
                onCategorySelected(selectedParentCategory!!, null)
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
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = category.emoji ?: "📋",
                fontSize = 32.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = category.title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
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
                text = "Select ${parentCategory.title}",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Parent category option
            SubCategoryItem(
                title = "${parentCategory.emoji} ${parentCategory.title}",
                subtitle = "General",
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

            LazyColumn {
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
            .clickable { onClick() }
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
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
            Icon(
                Icons.Default.Add, // You might want to use a checkmark icon
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
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
    
    LaunchedEffect(Unit) {
        try {
            val categories = getMostUsedCategoriesUseCase(8)
            frequentCategories = if (categories.isEmpty()) {
                // Fallback to default categories if no usage data exists
                com.andriybobchuk.mooney.mooney.data.CategoryDataSource.categories
                    .filter { !it.isSubCategory() }
                    .take(8)
            } else {
                categories
            }
        } catch (e: Exception) {
            // Fallback in case of error
            frequentCategories = com.andriybobchuk.mooney.mooney.data.CategoryDataSource.categories
                .filter { !it.isSubCategory() }
                .take(8)
        }
    }
    
    if (frequentCategories.isNotEmpty()) {
        Column {
            Text(
                text = "Frequently Used",
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
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    
    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(200.dp)
        ) { page ->
            when (page) {
                0 -> SpendingCalendarView(
                    selectedMonth = selectedMonth,
                    dailyTotals = dailyTotals,
                    modifier = Modifier.fillMaxSize()
                )
                1 -> SpendingLineChart(
                    selectedMonth = selectedMonth,
                    dailyTotals = dailyTotals,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        //Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(2) { index ->
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
    
    val viewModel: TransactionViewModel = koinViewModel()
    val previousMonthTotals = remember { mutableStateOf(emptyMap<Int, Double>()) }

    LaunchedEffect(selectedMonth) {
        try {
            val avgTotals = mutableMapOf<Int, Double>()
            val monthsToAverage = 6
            var currentMonth = selectedMonth

            // Collect daily totals for past 6 months
            val allMonthTotals = mutableListOf<Map<Int, Double>>()
            repeat(monthsToAverage) {
                currentMonth = currentMonth.previousMonth()
                val totals = mutableMapOf<Int, Double>()
                val prevDays = getDaysInMonth(currentMonth.year, currentMonth.month)
                for (day in 1..prevDays) {
                    val date = LocalDate(currentMonth.year, currentMonth.month, day)
                    totals[day] = viewModel.getDailyTotalForMonth(date)
                }
                allMonthTotals.add(totals)
            }

            // Average across months for each day
            for (day in 1..daysInMonth) {
                val values = allMonthTotals.mapNotNull { it[day] }.filter { it > 0 }
                avgTotals[day] = if (values.isNotEmpty()) values.average() else 0.0
            }

            previousMonthTotals.value = avgTotals
        } catch (e: Exception) {
            previousMonthTotals.value = emptyMap()
        }
    }
    
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Spending Comparison",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
                    text = "Current",
                    style = MaterialTheme.typography.bodySmall
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
                    text = "6mo avg",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        
       // Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            val currentTotal = dailyTotals.values.sum()
            val previousTotal = previousMonthTotals.value.values.sum()
            
            if (currentTotal == 0.0 && previousTotal == 0.0) {
                Text(
                    text = "No spending data available",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxTotal = maxOf(currentTotal, previousTotal)
                    
                    if (maxTotal <= 0) return@Canvas
                    
                    val width = size.width
                    val height = size.height
                    val dayWidth = width / daysInMonth
                    
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

@Composable
fun SpendingCalendarView(
    selectedMonth: MonthKey,
    dailyTotals: Map<Int, Double>,
    modifier: Modifier = Modifier
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
                text = "Week",
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
                            
                            Card(
                                modifier = Modifier
                                    .aspectRatio(1.5f)
                                    .fillMaxSize(),
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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.appColors.cardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Expense option
            TransactionTypeOption(
                type = CategoryType.EXPENSE,
                label = "Expense",
                emoji = "💸",
                isSelected = selectedType == CategoryType.EXPENSE,
                onClick = { onTypeSelected(CategoryType.EXPENSE) },
                modifier = Modifier.weight(1f)
            )
            
            // Income option
            TransactionTypeOption(
                type = CategoryType.INCOME,
                label = "Income",
                emoji = "💰",
                isSelected = selectedType == CategoryType.INCOME,
                onClick = { onTypeSelected(CategoryType.INCOME) },
                modifier = Modifier.weight(1f)
            )
            
            // Transfer option
            TransactionTypeOption(
                type = CategoryType.TRANSFER,
                label = "Transfer",
                emoji = "↔️",
                isSelected = selectedType == CategoryType.TRANSFER,
                onClick = { onTypeSelected(CategoryType.TRANSFER) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TransactionTypeOption(
    type: CategoryType,
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        when (type) {
            CategoryType.EXPENSE -> MaterialTheme.appColors.expenseColor.copy(alpha = 0.2f)
            CategoryType.INCOME -> MaterialTheme.appColors.incomeColor.copy(alpha = 0.2f)
            CategoryType.TRANSFER -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        }
    } else {
        Color.Transparent
    }
    
    val textColor = if (isSelected) {
        when (type) {
            CategoryType.EXPENSE -> MaterialTheme.appColors.expenseColor
            CategoryType.INCOME -> MaterialTheme.appColors.incomeColor
            CategoryType.TRANSFER -> MaterialTheme.colorScheme.primary
        }
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

