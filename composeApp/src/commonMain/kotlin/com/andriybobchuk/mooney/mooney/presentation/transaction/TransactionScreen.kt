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
    // New category selection state
    val defaultCategory = categories.find { it.title.contains("Groceries") }
    val selectedCategory: Category? = if (isEditMode) {
        when {
            transactionToEdit?.subcategory?.isGeneralCategory() == true -> transactionToEdit.subcategory
            transactionToEdit?.subcategory?.isSubCategory() == true -> transactionToEdit.subcategory.parent
            else -> defaultCategory
        }
    } else {
        defaultCategory
    }
    val selectedSubCategory: Category? = if (transactionToEdit?.subcategory?.isSubCategory() == true) transactionToEdit.subcategory else null
    
    var currentSelectedCategory by remember { mutableStateOf(selectedCategory) }
    var currentSelectedSubCategory by remember { mutableStateOf(selectedSubCategory) }
    var showCategorySheet by remember { mutableStateOf(false) }


    var selectedDate by remember {
        mutableStateOf(transactionToEdit?.date ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
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

