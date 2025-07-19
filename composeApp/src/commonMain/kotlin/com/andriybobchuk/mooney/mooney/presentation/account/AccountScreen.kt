package com.andriybobchuk.mooney.mooney.presentation.account

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.presentation.formatWithCommas
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel = koinViewModel(),
    bottomNavbar: @Composable () -> Unit,
) {
    // General
    val state by viewModel.state.collectAsState()
    val accounts = state.accounts
    val totalNetWorth = state.totalNetWorth

    // Sheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<UiAccount?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.background(Color(0xFF3E4DBA)),
        topBar = {
            Toolbars.Primary(
                title = "Accounts",
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            bottomNavbar()
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSheet = true },
                content = {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                },
                contentColor = Color.White,
                containerColor = Color(0xFF3E4DBA)
            )
        },
        content = { paddingValues ->
            AccountScreenContent(
                Modifier.padding(paddingValues),
                accounts,
                totalNetWorth,
                state.totalNetWorthCurrency,
                { viewModel.onNetWorthLabelClick() },
                {
                    editingAccount = it
                    showSheet = true
                },
                { viewModel.deleteAccount(it.id) }
            )

            if (showSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showSheet = false
                        editingAccount = null
                    },
                    sheetState = sheetState,
                    containerColor = Color(0xFFF8F9FF)
                ) {
                    AccountSheet(
                        editingAccount = editingAccount,
                        onAdd = { title, emoji, amount, currency ->
                            viewModel.upsertAccount(editingAccount?.id ?: 0, title, emoji, amount, currency)
                            scope.launch { sheetState.hide(); showSheet = false }
                        }
                    )
                }
            }
        }
    )
}


@Composable
private fun AccountScreenContent(
    modifier: Modifier,
    accounts: List<UiAccount?>,
    totalNetWorth: Double,
    totalNetWorthCurrency: Currency,
    onTotalNetWorthClick: () -> Unit,
    onEdit: (UiAccount) -> Unit,
    onDelete: (UiAccount) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF3E4DBA))
    ) {
        Header(totalNetWorth, totalNetWorthCurrency, onTotalNetWorthClick)
        AccountsColumn(
            accounts = accounts,
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

@Composable
private fun Header(totalNetWorth: Double, totalNetWorthCurrency: Currency, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("${totalNetWorth.formatWithCommas()}")
                }
                withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                    append(" ${totalNetWorthCurrency.symbol}")
                }
            },
            fontSize = 28.sp,
            color = Color.White
        )
        Text(
            text = "Total net worth",
            fontSize = 18.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun AccountsColumn(
    accounts: List<UiAccount?>,
    onEdit: (UiAccount) -> Unit,
    onDelete: (UiAccount) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
            .background(Color.White),
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            items(accounts) { account ->
                account?.let {
                    AccountCard(
                        account = account,
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountCard(
    account: UiAccount,
    onEdit: (UiAccount) -> Unit,
    onDelete: (UiAccount) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { onEdit(account) },
                onLongClick = { expanded = true }
            ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFFF8F9FF))
                .padding(18.dp)
        ) {
            Column {
                Row {
                    Text(
                        text = account.emoji,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("${account.baseCurrencyAmount.formatWithCommas()} ")
                            }
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                                append("${GlobalConfig.baseCurrency.symbol}")
                            }
                        },
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = account.title,
                    fontSize = 16.sp,
                    color = Color.Gray
                )

            }
            Spacer(Modifier.weight(1f))
            if (account.originalCurrency != GlobalConfig.baseCurrency) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontSize = 20.sp)) {
                            append("${account.originalAmount.formatWithCommas()} ${account.originalCurrency.symbol}")
                        }
                        append("\n")
                        withStyle(style = SpanStyle(fontSize = 16.sp, color = Color.Gray)) {
                            append("*${account.exchangeRate?.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}")
                        }
                    },
                    color = Color.Gray
                )
            }
        }
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("✏\uFE0F Edit") },
            onClick = {
                expanded = false
                onEdit(account)
            }
        )
        DropdownMenuItem(
            text = { Text("\uD83D\uDDD1\uFE0F Delete") },
            onClick = {
                expanded = false
                onDelete(account)
            }
        )
    }
}

@Composable
private fun AccountSheet(
    editingAccount: UiAccount? = null,
    onAdd: (String, String, Double, Currency) -> Unit
) {
    var title by remember { mutableStateOf(editingAccount?.title ?: "") }
    var emoji by remember { mutableStateOf(editingAccount?.emoji ?: "💰") }
    var amount by remember { mutableStateOf(editingAccount?.originalAmount?.formatWithCommas() ?: "") }
    var selectedCurrency by remember { mutableStateOf(editingAccount?.originalCurrency ?: GlobalConfig.baseCurrency) }

    val currencies = Currency.entries.toList()

    Column(modifier = Modifier.padding(20.dp)) {
        Text(if (editingAccount != null) "Edit This Account" else "Add New Account", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = emoji,
            onValueChange = {
                if (it.length <= 2) emoji = it
            },
            label = { Text("Emoji") },
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = title,
            onValueChange = {
                if (it.length <= 16) title = it
            },
            label = { Text("Title") },
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        // Currency dropdown
        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { expanded = true }) {
                Text("Currency: ${selectedCurrency.name}")
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                currencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency.name) },
                        onClick = {
                            selectedCurrency = currency
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                onAdd(title, emoji, amt, selectedCurrency)
            }
        ) {
            Text(if (editingAccount != null) "Update Account" else "Create Account")
        }
    }
}



