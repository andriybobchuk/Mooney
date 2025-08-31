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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.andriybobchuk.mooney.mooney.presentation.analytics.MonthKey
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
    onSettingsClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val transactions = state.transactions
    val total = state.total
    val totalCurrency = state.totalCurrency
    val frequentCategories by viewModel.frequentCategories.collectAsState()

    // Sheet
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isBottomSheetOpen by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var preselectedCategory by remember { mutableStateOf<Category?>(null) }

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
                },
                actions = listOf(
                    Toolbars.ToolBarAction(
                        icon = Icons.Default.Settings,
                        contentDescription = "Settings",
                        onClick = onSettingsClick
                    ),
                    Toolbars.ToolBarAction(
                        icon = Icons.Default.Add,
                        contentDescription = "Add Transaction",
                        onClick = {
                            preselectedCategory = null
                            isBottomSheetOpen = true
                        }
                    )
                )
            )
        },
        bottomBar = { bottomNavbar() },
        floatingActionButton = {
            FrequentCategoryFABs(
                categories = frequentCategories.take(8),
                onCategorySelected = { category ->
                    preselectedCategory = category
                    isBottomSheetOpen = true
                }
            )
        },
        content = { paddingValues ->
            TransactionsScreenContent(
                modifier = Modifier
                    .padding(paddingValues)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                transactions = transactions,
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
                onDailyTotal = viewModel::getDailyTotal
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
        }
    )
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
    total: Double,
    currency: Currency,
    selectedMonth: MonthKey,
    dailyTotals: Map<Int, Double> = emptyMap(),
    onCurrencyClick: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Int) -> Unit,
    onDailyTotal: (LocalDate) -> Double = { 0.0 }
) {
    // Group and sort transactions by date (descending), then by ID (most recent first)
    val grouped = transactions.filterNotNull().groupBy { it.date }
    val sortedGroups = grouped.entries
        .sortedByDescending { it.key }
        .map { (date, transactions) ->
            date to transactions.sortedByDescending { it.id }
        }

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
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            )
            Text(
                "Spent in ${selectedMonth.toDisplayString()}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
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
                item {
                    TransactionPagerView(
                        selectedMonth = selectedMonth,
                        dailyTotals = dailyTotals,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                
                sortedGroups.forEach { (date, txList) ->
                    stickyHeader {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 14.dp),
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
                                    .padding(vertical = 3.dp, horizontal = 10.dp),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
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
                                        .padding(vertical = 3.dp, horizontal = 10.dp),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
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
            Text(
                transaction.subcategory.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
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
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
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
    selectedMonth: MonthKey,
    preselectedCategory: Category? = null,
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
        preselectedCategory ?: defaultCategoryType
    }
    // New category selection state
    val defaultCategory = categories.find { it.title.contains("Groceries") }
    val selectedCategory: Category? = if (isEditMode) {
        when {
            transactionToEdit?.subcategory?.isGeneralCategory() == true -> transactionToEdit.subcategory
            transactionToEdit?.subcategory?.isSubCategory() == true -> transactionToEdit.subcategory.parent
            else -> defaultCategory
        }
    } else {
        preselectedCategory ?: defaultCategory
    }
    val selectedSubCategory: Category? = if (transactionToEdit?.subcategory?.isSubCategory() == true) transactionToEdit.subcategory else null
    
    var currentSelectedCategory by remember { mutableStateOf(selectedCategory) }
    var currentSelectedSubCategory by remember { mutableStateOf(selectedSubCategory) }
    var showCategorySheet by remember { mutableStateOf(false) }


    var selectedDate by remember {
        mutableStateOf(
            transactionToEdit?.date ?: LocalDate(selectedMonth.year, selectedMonth.month, 1)
        )
    }
    var showDateSheet by remember { mutableStateOf(false) }


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
            
            // New single category button
            OutlinedButton(
                onClick = { showCategorySheet = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                val displayText = when {
                    currentSelectedSubCategory != null -> "${currentSelectedSubCategory!!.resolveEmoji()} ${currentSelectedSubCategory!!.title}"
                    currentSelectedCategory != null -> "${currentSelectedCategory!!.emoji ?: ""} ${currentSelectedCategory!!.title}"
                    else -> "🛒 Groceries & Household" // Default
                }
                Text(displayText)
            }

            Spacer(Modifier.height(8.dp))

            // New single date button
            OutlinedButton(
                onClick = { showDateSheet = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                val dayName = selectedDate.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                val dayNumber = selectedDate.dayOfMonth
                Text("$dayName $dayNumber")
            }



            Spacer(Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val parsedAmount = amount?.toDoubleOrNull()
                    val finalCategory = currentSelectedCategory ?: defaultCategory
                    if (parsedAmount != null && selectedAccount != null && finalCategory != null) {
                        val transaction = Transaction(
                            id = transactionToEdit?.id ?: 0,
                            amount = parsedAmount,
                            account = selectedAccount!!,
                            subcategory = currentSelectedSubCategory ?: finalCategory,
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(Modifier.padding(20.dp).fillMaxSize()) {
            Text(
                text = "Select Date",
                fontWeight = FontWeight.Bold,
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
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Year: $selectedYear")
        }
        
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
    
    val previousMonth = remember(selectedMonth) {
        if (month == 1) {
            MonthKey(year - 1, 12)
        } else {
            MonthKey(year, month - 1)
        }
    }
    
    val viewModel: TransactionViewModel = koinViewModel()
    val previousMonthTotals = remember { mutableStateOf(emptyMap<Int, Double>()) }
    
    LaunchedEffect(previousMonth) {
        try {
            val totals = mutableMapOf<Int, Double>()
            val prevDaysInMonth = getDaysInMonth(previousMonth.year, previousMonth.month)
            for (day in 1..prevDaysInMonth) {
                val date = LocalDate(previousMonth.year, previousMonth.month, day)
                val dayTotal = viewModel.getDailyTotalForMonth(date)
                totals[day] = dayTotal
            }
            previousMonthTotals.value = totals
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
                    text = "Previous",
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
                    val dayWidth = width / maxOf(daysInMonth, getDaysInMonth(previousMonth.year, previousMonth.month))
                    
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
                    
                    val prevDaysInMonth = getDaysInMonth(previousMonth.year, previousMonth.month)
                    for (day in 1..minOf(daysInMonth, prevDaysInMonth)) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
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
        }
        
        Spacer(Modifier.height(4.dp))
        
        val weeks = (calendarData.daysInMonth + calendarData.startOffset + 6) / 7
        repeat(weeks) { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
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
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (intensity > 0.5) 
                                            MaterialTheme.colorScheme.onPrimary 
                                        else 
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                    if (amount > 0) {
                                        Text(
                                            text = "${amount.toInt()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 9.sp,
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

