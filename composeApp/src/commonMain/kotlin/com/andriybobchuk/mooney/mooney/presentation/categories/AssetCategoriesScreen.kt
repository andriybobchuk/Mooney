package com.andriybobchuk.mooney.mooney.presentation.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andriybobchuk.mooney.core.data.database.AssetCategoryEntity
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import com.andriybobchuk.mooney.mooney.data.localizedAssetCategoryTitle
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.add_asset_category
import mooney.composeapp.generated.resources.add_liability_category
import mooney.composeapp.generated.resources.asset_categories
import mooney.composeapp.generated.resources.assets_section
import mooney.composeapp.generated.resources.cancel
import mooney.composeapp.generated.resources.category_name
import mooney.composeapp.generated.resources.delete
import mooney.composeapp.generated.resources.delete_asset_category_msg
import mooney.composeapp.generated.resources.liabilities_section
import mooney.composeapp.generated.resources.save
import org.jetbrains.compose.resources.stringResource

/** Matches the look of [TransactionCategoriesScreen]: pill toggle on top, list
 *  rows mid, big black CTA pill at the bottom. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetCategoriesScreen(
    viewModel: AssetCategoriesViewModel,
    onBackClick: () -> Unit,
    embedded: Boolean = false
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showAssets by remember { mutableStateOf(true) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<AssetCategoryEntity?>(null) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var deleteName by remember { mutableStateOf("") }

    val assetCats by remember(state.assetCategories) {
        derivedStateOf { state.assetCategories.filter { !it.isLiability } }
    }
    val liabilityCats by remember(state.assetCategories) {
        derivedStateOf { state.assetCategories.filter { it.isLiability } }
    }
    val visibleCats = if (showAssets) assetCats else liabilityCats

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
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = stringResource(Res.string.delete_asset_category_msg, deleteName),
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

    editingCategory?.let { cat ->
        val localizedName = localizedAssetCategoryTitle(cat)
        AssetCategoryEditSheet(
            category = cat,
            onDismiss = { editingCategory = null },
            onRename = { newName ->
                viewModel.onAction(AssetCategoriesAction.Rename(cat.id, newName))
                editingCategory = null
            },
            onDelete = {
                deleteId = cat.id
                deleteName = localizedName
                editingCategory = null
            }
        )
    }

    if (showAddSheet) {
        AddAssetCategoryNameSheet(
            isLiability = !showAssets,
            onDismiss = { showAddSheet = false },
            onAdd = { name ->
                viewModel.onAction(AssetCategoriesAction.Add(name, !showAssets))
                showAddSheet = false
            }
        )
    }

    val body: @Composable (PaddingValues) -> Unit = body@{ paddingValues ->
        if (state.isInitialLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
            return@body
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TypePill(
                    label = stringResource(Res.string.assets_section),
                    selected = showAssets,
                    onClick = { showAssets = true }
                )
                TypePill(
                    label = stringResource(Res.string.liabilities_section),
                    selected = !showAssets,
                    onClick = { showAssets = false }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${visibleCats.size} categories",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(visibleCats, key = { it.id }) { category ->
                    AssetCategoryListRow(
                        emoji = category.emoji.ifBlank { if (showAssets) "💰" else "📉" },
                        title = localizedAssetCategoryTitle(category),
                        onClick = { editingCategory = category }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.onBackground)
                    .clickable { showAddSheet = true }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (showAssets) {
                        "+ ${stringResource(Res.string.add_asset_category)}"
                    } else {
                        "+ ${stringResource(Res.string.add_liability_category)}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.background
                )
            }
        }
    }

    if (embedded) {
        body(PaddingValues(0.dp))
    } else {
        Scaffold(
            topBar = {
                Toolbars.Primary(
                    title = stringResource(Res.string.asset_categories),
                    showBackButton = true,
                    onBackClick = onBackClick,
                    scrollBehavior = scrollBehavior
                )
            }
        ) { paddingValues -> body(paddingValues) }
    }
}

@Composable
private fun TypePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.background
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun AssetCategoryListRow(
    emoji: String,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = com.andriybobchuk.mooney.core.presentation.Icons.ChevronRightIcon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAssetCategoryNameSheet(
    isLiability: Boolean,
    onDismiss: () -> Unit,
    onAdd: (name: String) -> Unit
) {
    MooneyBottomSheet(onDismissRequest = onDismiss) {
        var name by remember { mutableStateOf("") }
        val canSave = name.isNotBlank()

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            EditSheetTopBar(
                title = if (isLiability) {
                    stringResource(Res.string.add_liability_category)
                } else {
                    stringResource(Res.string.add_asset_category)
                },
                saveLabel = stringResource(Res.string.save),
                saveEnabled = canSave,
                onCancel = onDismiss,
                onSave = { if (canSave) onAdd(name.trim()) }
            )
            Spacer(Modifier.height(20.dp))
            CategoryHero(emoji = if (isLiability) "📉" else "💰")
            Spacer(Modifier.height(8.dp))
            Text(
                text = name.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))

            FormSectionLabel("NAME")
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 30) name = it },
                placeholder = { Text(stringResource(Res.string.category_name)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetCategoryEditSheet(
    category: AssetCategoryEntity,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    MooneyBottomSheet(onDismissRequest = onDismiss) {
        val localizedTitle = localizedAssetCategoryTitle(category)
        var name by remember(category.id) { mutableStateOf(localizedTitle) }
        val emoji = category.emoji.ifBlank { if (category.isLiability) "📉" else "💰" }

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            EditSheetTopBar(
                title = "Edit category",
                saveLabel = stringResource(Res.string.save),
                saveEnabled = name.isNotBlank() && name.trim() != localizedTitle && name.trim() != category.title,
                onCancel = onDismiss,
                onSave = { onRename(name.trim()) }
            )
            Spacer(Modifier.height(20.dp))
            CategoryHero(emoji = emoji)
            Spacer(Modifier.height(8.dp))
            Text(
                text = name.ifBlank { localizedTitle },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))

            FormSectionLabel("NAME")
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 30) name = it },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                    .clickable { onDelete() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🗑  ${stringResource(Res.string.delete)} category",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun EditSheetTopBar(
    title: String,
    saveLabel: String,
    saveEnabled: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.cancel),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clickable { onCancel() }
                .padding(end = 12.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = saveLabel,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (saveEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            },
            modifier = Modifier
                .clickable(enabled = saveEnabled) { onSave() }
                .padding(start = 12.dp)
        )
    }
}

@Composable
private fun CategoryHero(emoji: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 40.sp)
        }
    }
}

@Composable
private fun FormSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
