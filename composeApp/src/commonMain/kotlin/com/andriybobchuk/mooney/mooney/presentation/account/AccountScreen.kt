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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.andriybobchuk.mooney.app.appColors
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
import com.andriybobchuk.mooney.mooney.domain.usecase.ReconciliationDifference
import com.andriybobchuk.mooney.mooney.domain.usecase.CreateReconciliationUseCase
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

data class ReconciliationData(
    val accountId: Int,
    val accountTitle: String,
    val oldAmount: Double,
    val newAmount: Double,
    val difference: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel = koinViewModel(),
    bottomNavbar: @Composable () -> Unit,
    onSettingsClick: () -> Unit = {},
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
    
    // Reconciliation dialog state
    var showReconciliationDialog by remember { mutableStateOf(false) }
    var reconciliationData by remember { mutableStateOf<ReconciliationData?>(null) }
    

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        topBar = {
            Toolbars.Primary(
                titleContent = {
                    Column(
                        modifier = Modifier.clickable { viewModel.onNetWorthLabelClick() }
                    ) {
                        Text(
                            text = "${totalNetWorth.formatWithCommas()} ${state.totalNetWorthCurrency.symbol}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        )
                        Text(
                            text = "Total Net Worth",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                actions = listOf(
                    Toolbars.ToolBarAction(
                        painter = com.andriybobchuk.mooney.core.presentation.Icons.RefreshIcon(),
                        contentDescription = "Refresh Exchange Rates",
                        onClick = { viewModel.refreshExchangeRates() }
                    ),
                    Toolbars.ToolBarAction(
                        painter = com.andriybobchuk.mooney.core.presentation.Icons.SettingsIcon(),
                        contentDescription = "Settings",
                        onClick = onSettingsClick
                    )
                )
            )
        },
        bottomBar = {
            bottomNavbar()
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSheet = true },
                content = {
                    Icon(Icons.Outlined.Add, contentDescription = "Add")
                },
                contentColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.primary
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
                MooneyBottomSheet(
                    onDismissRequest = {
                        showSheet = false
                        editingAccount = null
                    },
                    sheetState = sheetState
                ) {
                    AccountSheet(
                        editingAccount = editingAccount,
                        onAdd = { title, emoji, amount, currency ->
                            // Check if this is an edit and if amount changed
                            editingAccount?.let { account ->
                                val difference = amount - account.originalAmount
                                if (kotlin.math.abs(difference) >= 0.01) {
                                    reconciliationData = ReconciliationData(
                                        accountId = account.id,
                                        accountTitle = title,
                                        oldAmount = account.originalAmount,
                                        newAmount = amount,
                                        difference = difference
                                    )
                                }
                            }

                            viewModel.upsertAccount(editingAccount?.id ?: 0, title, emoji, amount, currency)
                            showSheet = false

                            if (reconciliationData != null) {
                                showReconciliationDialog = true
                            }

                            editingAccount = null
                        }
                    )
                }
            }
            
            // Reconciliation dialog
            if (showReconciliationDialog && reconciliationData != null) {
                ReconciliationDialog(
                    data = reconciliationData!!,
                    onConfirm = {
                        showReconciliationDialog = false
                        reconciliationData = null
                        // TODO: Create reconciliation transaction using CreateReconciliationUseCase
                        // For now, just dismiss the dialog
                    },
                    onDismiss = {
                        showReconciliationDialog = false
                        reconciliationData = null
                    }
                )
            }
        }
    )
}

@Composable
private fun ReconciliationDialog(
    data: ReconciliationData,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Account Balance Changed", fontWeight = FontWeight.Medium)
        },
        text = {
            Column {
                Text("The balance for '${data.accountTitle}' has changed:")
                Spacer(Modifier.height(8.dp))
                Text("Previous: ${data.oldAmount.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}")
                Text("New: ${data.newAmount.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}")
                Text("Difference: ${data.difference.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}")
                Spacer(Modifier.height(12.dp))
                Text(
                    "Would you like to automatically create a reconciliation transaction to explain this change?",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No")
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
    AccountsColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        accounts = accounts,
        onEdit = onEdit,
        onDelete = onDelete
    )
}

@Composable
private fun AccountsColumn(
    modifier: Modifier = Modifier,
    accounts: List<UiAccount?>,
    onEdit: (UiAccount) -> Unit,
    onDelete: (UiAccount) -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 16.dp)
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
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { onEdit(account) },
                onLongClick = { expanded = true }
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.appColors.cardBackground)
                .padding(16.dp)
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
        Text(if (editingAccount != null) "Edit This Account" else "Add New Account", fontWeight = FontWeight.Medium, fontSize = 20.sp)
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
                val amt = amount.replace(",", "").toDoubleOrNull() ?: 0.0
                onAdd(title, emoji, amt, selectedCurrency)
            }
        ) {
            Text(if (editingAccount != null) "Update Account" else "Create Account")
        }
    }
}




