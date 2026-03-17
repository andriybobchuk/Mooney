package com.andriybobchuk.mooney.mooney.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.settings.ThemeMode
import com.andriybobchuk.mooney.mooney.presentation.settings.components.*
import com.andriybobchuk.mooney.core.platform.FileHandler
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val fileHandler = koinInject<FileHandler>()
    val coroutineScope = rememberCoroutineScope()
    
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var importJsonData by remember { mutableStateOf<String?>(null) }
    var importPreview by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    
    // Handle events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SettingsEvent.ExportReady -> {
                    coroutineScope.launch {
                        fileHandler.saveTextFile(event.jsonData, "mooney_backup.json")
                            .onSuccess {
                                // Could show success toast
                            }
                            .onFailure { error ->
                                // Could show error
                            }
                    }
                }
                is SettingsEvent.ShowImportConfirmation -> {
                    importPreview = Triple(event.transactions, event.accounts, event.goals)
                    showImportConfirmDialog = true
                }
                is SettingsEvent.ImportSuccess -> {
                    // Could show success message
                    showImportConfirmDialog = false
                    importJsonData = null
                    importPreview = null
                }
                is SettingsEvent.ShowError -> {
                    // Handle error display
                }
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            // Show error - could be replaced with SnackbarHost
        }
    }
    
    // Import confirmation dialog
    if (showImportConfirmDialog && importPreview != null && importJsonData != null) {
        AlertDialog(
            onDismissRequest = { 
                showImportConfirmDialog = false
                importJsonData = null
                importPreview = null
            },
            title = { Text("Confirm Import") },
            text = {
                Column {
                    Text("The following data will be imported:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• ${importPreview!!.first} transactions")
                    Text("• ${importPreview!!.second} accounts")
                    Text("• ${importPreview!!.third} goals")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This will add to your existing data.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        viewModel.confirmImport(importJsonData!!)
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showImportConfirmDialog = false
                        importJsonData = null
                        importPreview = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Toolbars.Primary(
                title = "Settings",
                showBackButton = true,
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                item {
                    PinnedCategoriesSection(
                        state = state,
                        onAction = viewModel::onAction
                    )
                }
                
                item {
                    GeneralSettingsSection(
                        state = state,
                        onAction = viewModel::onAction
                    )
                }
                
                item {
                    DataExportImportSection(
                        isExporting = state.isExporting,
                        isImporting = state.isImporting,
                        onExportClick = {
                            viewModel.onAction(SettingsAction.OnExportData)
                        },
                        onImportClick = {
                            coroutineScope.launch {
                                fileHandler.pickAndReadTextFile()
                                    .onSuccess { content ->
                                        content?.let {
                                            importJsonData = it
                                            viewModel.onAction(SettingsAction.OnImportData(it))
                                        }
                                    }
                                    .onFailure { error ->
                                        // Handle error
                                    }
                            }
                        }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}