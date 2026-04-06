package com.andriybobchuk.mooney.mooney.presentation.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import org.jetbrains.compose.resources.stringResource
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetCategoriesScreen(
    viewModel: AssetCategoriesViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showAddSheet by remember { mutableStateOf(false) }
    var addIsLiability by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var deleteName by remember { mutableStateOf("") }
    var renamingId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    val assetCats = remember(state.assetCategories) { state.assetCategories.filter { !it.isLiability } }
    val liabilityCats = remember(state.assetCategories) { state.assetCategories.filter { it.isLiability } }

    // Delete confirmation
    deleteId?.let { id ->
        MooneyBottomSheet(onDismissRequest = { deleteId = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.delete),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Delete \"$deleteName\"? Accounts using this category will keep their data but appear under \"Other\".",
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
                            viewModel.onAction(AssetCategoriesAction.Delete(id))
                            deleteId = null
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

    // Add category sheet
    if (showAddSheet) {
        MooneyBottomSheet(onDismissRequest = { showAddSheet = false }) {
            var title by remember { mutableStateOf("") }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    if (addIsLiability) "Add Liability Category" else "Add Asset Category",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 30) title = it },
                    label = { Text(stringResource(Res.string.category_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.onAction(AssetCategoriesAction.Add(title.trim(), addIsLiability))
                            showAddSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotBlank()
                ) {
                    Text(stringResource(Res.string.add))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            Toolbars.Primary(
                title = "Asset Categories",
                showBackButton = true,
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ASSETS section header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("\uD83D\uDCB0", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Assets",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            addIsLiability = false
                            showAddSheet = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "+ Add",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Asset items
            items(assetCats.size) { index ->
                val category = assetCats[index]
                CategoryItemRow(
                    title = category.title,
                    isRenaming = renamingId == category.id,
                    renameText = renameText,
                    onRenameTextChange = { renameText = it },
                    onStartRename = {
                        renamingId = category.id
                        renameText = category.title
                    },
                    onSaveRename = {
                        if (renameText.isNotBlank()) {
                            viewModel.onAction(AssetCategoriesAction.Rename(category.id, renameText.trim()))
                        }
                        renamingId = null
                    },
                    onDelete = {
                        deleteId = category.id
                        deleteName = category.title
                    }
                )
            }

            // Spacer between sections
            item { Spacer(Modifier.height(16.dp)) }

            // LIABILITIES section header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("\uD83D\uDCC9", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Liabilities",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            addIsLiability = true
                            showAddSheet = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "+ Add",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Liability items
            items(liabilityCats.size) { index ->
                val category = liabilityCats[index]
                CategoryItemRow(
                    title = category.title,
                    isRenaming = renamingId == category.id,
                    renameText = renameText,
                    onRenameTextChange = { renameText = it },
                    onStartRename = {
                        renamingId = category.id
                        renameText = category.title
                    },
                    onSaveRename = {
                        if (renameText.isNotBlank()) {
                            viewModel.onAction(AssetCategoriesAction.Rename(category.id, renameText.trim()))
                        }
                        renamingId = null
                    },
                    onDelete = {
                        deleteId = category.id
                        deleteName = category.title
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryItemRow(
    title: String,
    isRenaming: Boolean,
    renameText: String,
    onRenameTextChange: (String) -> Unit,
    onStartRename: () -> Unit,
    onSaveRename: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)
            .then(if (!isRenaming) Modifier.clickable { onStartRename() } else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRenaming) {
            OutlinedTextField(
                value = renameText,
                onValueChange = { if (it.length <= 30) onRenameTextChange(it) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onSaveRename) {
                Text("Save")
            }
        } else {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Text(
                    "\u00D7",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
