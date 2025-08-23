package com.andriybobchuk.mooney.mooney.presentation.transaction

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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.core.presentation.theme.appColors
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.presentation.account.UiAccount
import com.andriybobchuk.mooney.mooney.presentation.account.toAccounts
import com.andriybobchuk.mooney.mooney.presentation.analytics.MonthPicker
import com.andriybobchuk.mooney.mooney.presentation.formatWithCommas
import com.andriybobchuk.mooney.mooney.presentation.formatToPlainString
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionViewModel = koinViewModel(),
    bottomNavbar: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val transactions = state.transactions
    val total = state.total
    val totalCurrency = state.totalCurrency

    // Sheet
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isBottomSheetOpen by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.primary),
        topBar = {
            Toolbars.Primary(
                title = "Transactions",
                scrollBehavior = scrollBehavior,
                customContent = {
                    MonthPicker(
                        selectedMonth = state.selectedMonth,
                        onMonthSelected = viewModel::onMonthSelected,
                    )
                }
            )
        },
        bottomBar = { bottomNavbar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isBottomSheetOpen = true },
                content = { Icon(Icons.Default.Add, contentDescription = "Add") },
                contentColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.primary
            )
        },
        content = { paddingValues ->
            TransactionsScreenContent(
                modifier = Modifier.padding(paddingValues),
                transactions = transactions,
                total = total,
                currency = totalCurrency,
                onCurrencyClick = viewModel::onTotalCurrencyClick,
                onEdit = {
                    transactionToEdit = it
                    isBottomSheetOpen = true
                },
                onDelete = viewModel::deleteTransaction,
                onDailyTotal = viewModel::getDailyTotal
            )

            if (isBottomSheetOpen) {
                TransactionBottomSheet(
                    onDismiss = {
                        isBottomSheetOpen = false
                        transactionToEdit = null
                    },
                    sheetState = bottomSheetState,
                    transactionToEdit = transactionToEdit,
                    accounts = state.accounts,
                    categories = state.categories,
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
        }
    )
}

fun LocalDate.formatForDisplay(): String {
    val month = this.month.name.lowercase().replaceFirstChar { it.uppercase() }
    val day = this.dayOfMonth
    return "$month $day" // e.g., "May 4"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreenContent(
    modifier: Modifier,
    transactions: List<Transaction?>,
    total: Double,
    currency: Currency,
    onCurrencyClick: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Int) -> Unit,
    onDailyTotal: (LocalDate) -> Double = { 0.0 }
) {
    // Group and sort transactions by date (descending)
    val grouped = transactions.filterNotNull().groupBy { it.date }
    val sortedGroups = grouped.entries.sortedByDescending { it.key }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${total.formatWithCommas()} ${currency.symbol}",
                modifier = Modifier.clickable { onCurrencyClick() },
                style = MaterialTheme.typography.headlineLarge.copy(
                   // color = MaterialTheme.colorScheme.onPrimary,
                    color  = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            )
            Text(
                "Spent this month",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                  //  color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    color = Color.White
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.background),
        ) {
            LazyColumn {
                sortedGroups.forEach { (date, txList) ->
                    stickyHeader {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Day pill on the left
                            Text(
                                text = date.formatForDisplay(),
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.appColors.pillBackground,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(vertical = 4.dp, horizontal = 12.dp),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                //color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start
                            )
                            
                            // Daily total pill on the right
                            val dailyTotal = onDailyTotal(date)
                            if (dailyTotal > 0) {
                                Text(
                                    text = "${dailyTotal.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}",
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.appColors.pillBackgroundSecondary.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(vertical = 4.dp, horizontal = 12.dp),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.End
                                )
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
                            TransactionItem(tx)
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
    }
}


@Composable
fun TransactionItem(transaction: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.appColors.transactionIcon),
            contentAlignment = Alignment.Center
        ) {
            Text(transaction.subcategory.resolveEmoji(), fontSize = 25.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                transaction.subcategory.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            if (transaction.subcategory.isSubCategory()) {
                Text(
                    transaction.subcategory.parent?.title ?: "???",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${transaction.amount.formatWithCommas()} ${transaction.account.currency.symbol}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (transaction.subcategory.type == CategoryType.INCOME) MaterialTheme.appColors.incomeColor else MaterialTheme.appColors.expenseColor
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
    onAdd: (Transaction) -> Unit,
    onUpdate: (Transaction) -> Unit,
) {
    val isEditMode = transactionToEdit != null

    var amount by remember { mutableStateOf(transactionToEdit?.amount?.formatToPlainString()) }

    val defaultAccount = accounts.filterNotNull().toAccounts().find { it.title.contains("Primary") }
    var selectedAccount by remember { mutableStateOf(transactionToEdit?.account ?: defaultAccount) }

    val defaultCategoryType: Category? = categories.find { it.isTypeCategory() && it.type == CategoryType.EXPENSE }
    val categoryType: Category? = if (isEditMode) {
        transactionToEdit?.subcategory?.getRoot()
    } else {
        defaultCategoryType
    }
    var selectedCategoryType by remember { mutableStateOf<Category?>(categoryType) }


    val defaultCategory = categories.find { it.title.contains("Groceries") }
    val category: Category? = if (isEditMode) {
        when {
            transactionToEdit?.subcategory?.isGeneralCategory() == true -> transactionToEdit.subcategory
            transactionToEdit?.subcategory?.isSubCategory() == true -> transactionToEdit.subcategory.parent
            else -> defaultCategory
        }
    } else {
        defaultCategory
    }


    var selectedCategory by remember { mutableStateOf(category) }
    var selectedSubCategory by remember { mutableStateOf(if (transactionToEdit?.subcategory?.isSubCategory() == true) transactionToEdit.subcategory else null) }
    var subCategoryFieldEnabled by remember { 
        mutableStateOf(
            if (isEditMode) {
                categories.filter { it.isSubCategory() && it.parent == category }.isNotEmpty()
            } else {
                false
            }
        ) 
    }


    var selectedDate by remember {
        mutableStateOf(transactionToEdit?.date ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
    }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
        }

        Column(Modifier.padding(20.dp).fillMaxSize()) {
            Text(
                text = if (isEditMode) "Edit This Transaction" else "Add New Transaction",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = amount ?: "",
                onValueChange = { amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )

            Spacer(Modifier.height(8.dp))

            AccountField(selectedAccount, accounts.filterNotNull(), { selectedAccount = it })
            CategoryTypeField(selectedCategoryType, categories.filter { it.isTypeCategory() }, {
                selectedCategoryType = it
                selectedCategory = null
            })
            CategoryField(
                selectedCategory,
                categories.filter { it.isGeneralCategory() && it.getRoot() == selectedCategoryType?.getRoot() },
                {
                    selectedCategory = it
                    selectedSubCategory = null
                    subCategoryFieldEnabled = categories.filter { it.isSubCategory() && it.parent == selectedCategory }.isNotEmpty()
                })
            SubCategoryField(
                selectedSubCategory,
                categories.filter { it.isSubCategory() && it.parent == selectedCategory },
                subCategoryFieldEnabled,
                { selectedSubCategory = it })


            Spacer(Modifier.height(8.dp))

            DateSelector(
                selectedDate = selectedDate,
                onDateChange = { selectedDate = it }
            )



            Spacer(Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val parsedAmount = amount?.toDoubleOrNull()
                    if (parsedAmount != null && selectedAccount != null && selectedCategory != null) {
                        val transaction = Transaction(
                            id = transactionToEdit?.id ?: 0,
                            amount = parsedAmount,
                            account = selectedAccount!!,
                            subcategory = selectedSubCategory ?: selectedCategory!!,
                            date = selectedDate
                        )
                        
                        if (isEditMode) {
                            onUpdate(transaction)
                        } else {
                            onAdd(transaction)
                        }
                    }
                }
            ) {
                Text(if (isEditMode) "Update" else "Add")
            }
        }
    }
}

@Composable
fun DateSelector(
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    var showYearMenu by remember { mutableStateOf(false) }
    var showMonthMenu by remember { mutableStateOf(false) }
    var showDayMenu by remember { mutableStateOf(false) }

    val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val years = (2000..currentDate.year).toList().reversed()
    val months = (1..12).toList()

    // Compute valid day count for the selected year/month
    val daysInMonth = getDaysInMonth(selectedDate.year, selectedDate.monthNumber)
    val days = (1..daysInMonth).toList()

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Year dropdown
        Box {
            OutlinedButton(onClick = { showYearMenu = true }) {
                Text("${selectedDate.year}")
            }
            DropdownMenu(expanded = showYearMenu, onDismissRequest = { showYearMenu = false }) {
                years.forEach { year ->
                    DropdownMenuItem(
                        text = { Text(year.toString()) },
                        onClick = {
                            val newDay = selectedDate.dayOfMonth.coerceAtMost(getDaysInMonth(year, selectedDate.monthNumber))
                            onDateChange(LocalDate(year, selectedDate.monthNumber, newDay))
                            showYearMenu = false
                        }
                    )
                }
            }
        }

        // Month dropdown
        Box {
            OutlinedButton(onClick = { showMonthMenu = true }) {
                Text(selectedDate.monthNumber.padZero())
            }
            DropdownMenu(expanded = showMonthMenu, onDismissRequest = { showMonthMenu = false }) {
                months.forEach { month ->
                    DropdownMenuItem(
                        text = { Text(month.padZero()) },
                        onClick = {
                            val newDay = selectedDate.dayOfMonth.coerceAtMost(getDaysInMonth(selectedDate.year, month))
                            onDateChange(LocalDate(selectedDate.year, month, newDay))
                            showMonthMenu = false
                        }
                    )
                }
            }
        }

        // Day dropdown
        Box {
            OutlinedButton(onClick = { showDayMenu = true }) {
                Text(formatDayWithWeekday(selectedDate.dayOfMonth, selectedDate.year, selectedDate.monthNumber))
            }
            DropdownMenu(expanded = showDayMenu, onDismissRequest = { showDayMenu = false }) {
                days.forEach { day ->
                    DropdownMenuItem(
                        text = { Text(formatDayWithWeekday(day, selectedDate.year, selectedDate.monthNumber)) },
                        onClick = {
                            onDateChange(LocalDate(selectedDate.year, selectedDate.monthNumber, day))
                            showDayMenu = false
                        }
                    )
                }
            }
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


@Composable
private fun AccountField(
    initialSelectedAccount: Account?,
    accounts: List<UiAccount>,
    onAccountSelected: (Account) -> Unit
) {
    var optionsExpanded by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { optionsExpanded = true },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(initialSelectedAccount?.let { "${it.emoji} ${it.title} (${it.amount.formatWithCommas()} ${it.currency.symbol})" }
            ?: "Select Account")
    }

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

@Composable
private fun CategoryTypeField(
    initialSelectedCategoryType: Category?,
    types: List<Category>,
    onTypeSelected: (Category) -> Unit
) {
    var optionsExpanded by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { optionsExpanded = true },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(initialSelectedCategoryType?.title ?: "")
    }

    DropdownMenu(
        expanded = optionsExpanded,
        onDismissRequest = { optionsExpanded = false }
    ) {
        types.forEach { it ->
            DropdownMenuItem(
                text = { Text(it.title) },
                onClick = {
                    onTypeSelected(it)
                    optionsExpanded = false
                }
            )
        }
    }
}

@Composable
private fun CategoryField(
    initialSelectedCategory: Category?,
    types: List<Category>,
    onSelected: (Category) -> Unit
) {
    var optionsExpanded by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { optionsExpanded = true },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("${initialSelectedCategory?.emoji} ${initialSelectedCategory?.title}")
    }

    DropdownMenu(
        expanded = optionsExpanded,
        onDismissRequest = { optionsExpanded = false }
    ) {
        types.forEach { it ->
            DropdownMenuItem(
                text = {
                    if (it.emoji != null && it.title != null) {
                        Text("${it.emoji} ${it.title}")
                    } else {
                        Text("Category (*)")
                    }
                },
                onClick = {
                    onSelected(it)
                    optionsExpanded = false
                }
            )
        }
    }
}

@Composable
private fun SubCategoryField(
    initialSelectedSubCategory: Category?,
    types: List<Category>,
    enabled: Boolean = true,
    onSelected: (Category) -> Unit
) {
    var optionsExpanded by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { optionsExpanded = true },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled
    ) {
        Text("${initialSelectedSubCategory?.resolveEmoji() ?: ""} ${initialSelectedSubCategory?.title ?: "Subcategory (Optional)"}")
    }

    DropdownMenu(
        expanded = optionsExpanded,
        onDismissRequest = { optionsExpanded = false }
    ) {
        types.forEach {
            DropdownMenuItem(
                text = { Text("${it.resolveEmoji()} ${it.title}") },
                onClick = {
                    onSelected(it)
                    optionsExpanded = false
                }
            )
        }
    }
}

