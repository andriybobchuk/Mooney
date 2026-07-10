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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andriybobchuk.mooney.core.premium.PaywallSheet
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import com.andriybobchuk.mooney.mooney.data.localizedCategoryTitle
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.add_category
import mooney.composeapp.generated.resources.add_subcategory
import mooney.composeapp.generated.resources.cancel
import mooney.composeapp.generated.resources.categories_label
import mooney.composeapp.generated.resources.category_name
import mooney.composeapp.generated.resources.delete
import mooney.composeapp.generated.resources.delete_tx_category_msg
import mooney.composeapp.generated.resources.edit_category
import mooney.composeapp.generated.resources.expense
import mooney.composeapp.generated.resources.income
import mooney.composeapp.generated.resources.new_expense_category
import mooney.composeapp.generated.resources.new_income_category
import mooney.composeapp.generated.resources.save
import mooney.composeapp.generated.resources.subcategories_label
import mooney.composeapp.generated.resources.transaction_categories
import org.jetbrains.compose.resources.stringResource

private const val DEFAULT_NEW_EMOJI = "🏷️"

private val EMOJI_PALETTE = listOf(
    "🛒", "🍽️", "☕", "🍺", "🏠", "💡", "🚗", "🚲",
    "⛽", "🚕", "🎧", "📱", "☁️", "💻", "❤️", "💪",
    "💊", "🏥", "✨", "👕", "👟", "🎓", "📚", "🎮",
    "🎬", "🎁", "🐶", "🐾", "🛡️", "🏦", "💼", "💵"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionCategoriesScreen(
    viewModel: TransactionCategoriesViewModel,
    onBackClick: () -> Unit,
    embedded: Boolean = false
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var selectedType by remember { mutableStateOf(CategoryType.EXPENSE) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var deleteName by remember { mutableStateOf("") }

    val generalCategories by remember(state.allCategories, selectedType) {
        derivedStateOf {
            state.allCategories.filter { it.isGeneralCategory() && it.type == selectedType }
        }
    }

    val subcategoryCount by remember(state.allCategories, selectedType) {
        derivedStateOf {
            state.allCategories.count { it.isSubCategory() && it.type == selectedType }
        }
    }

    if (state.showPaywall) {
        PaywallSheet(
            isLoading = state.isPurchasing,
            errorMessage = state.purchaseError,
            trigger = com.andriybobchuk.mooney.core.premium.PaywallTrigger.CATEGORY_LIMIT,
            onDismiss = { viewModel.onAction(TransactionCategoriesAction.DismissPaywall) },
            onSubscribe = { viewModel.onAction(TransactionCategoriesAction.Subscribe) },
            onRestore = { viewModel.onAction(TransactionCategoriesAction.RestorePurchases) }
        )
    }

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
                    text = stringResource(Res.string.delete_tx_category_msg, deleteName),
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
                            viewModel.onAction(TransactionCategoriesAction.DeleteCategory(id))
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
        val localizedName = localizedCategoryTitle(cat)
        CategoryEditSheet(
            category = cat,
            subcategories = state.allCategories.filter { it.parent?.id == cat.id },
            onDismiss = { editingCategory = null },
            onSaveName = { /* future: rename support — VM lacks it today */ },
            onAddSubcategory = { subTitle ->
                viewModel.onAction(
                    TransactionCategoriesAction.AddCategory(
                        title = subTitle,
                        type = cat.type.name,
                        emoji = null,
                        parentId = cat.id
                    )
                )
            },
            onDeleteSubcategory = { subId ->
                viewModel.onAction(TransactionCategoriesAction.DeleteCategory(subId))
            },
            onDeleteCategory = {
                deleteId = cat.id
                deleteName = localizedName
                editingCategory = null
            }
        )
    }

    if (showAddSheet) {
        AddCategoryNameSheet(
            type = selectedType,
            onDismiss = { showAddSheet = false },
            onAdd = { name, emoji ->
                viewModel.onAction(
                    TransactionCategoriesAction.AddCategory(
                        title = name,
                        type = selectedType.name,
                        emoji = emoji.ifBlank { null },
                        parentId = null
                    )
                )
                showAddSheet = false
            }
        )
    }

    val body: @Composable (PaddingValues) -> Unit = { paddingValues ->
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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Expense / Income type toggle. Bigger contrast than the
                // outer Transactions/Accounts segmented tabs, since this is
                // the *active* filter, not a screen pivot.
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        CategoryType.EXPENSE to stringResource(Res.string.expense),
                        CategoryType.INCOME to stringResource(Res.string.income)
                    ).forEach { (type, label) ->
                        TypePill(
                            label = label,
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                    }
                }

                // Count line — mirrors the screenshot ("9 categories · 21
                // subcategories"). The Reorder affordance was here previously
                // as a disabled label; removed because it read to users as a
                // broken button. Bring it back as a real drag-mode toggle
                // when drag-drop ships for this screen.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${generalCategories.size} " +
                            stringResource(Res.string.categories_label) + " · " +
                            "$subcategoryCount " +
                            stringResource(Res.string.subcategories_label),
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
                    items(generalCategories, key = { it.id }) { category ->
                        val subCount = state.allCategories.count { it.parent?.id == category.id }
                        CategoryListRow(
                            emoji = category.resolveEmoji().ifBlank { DEFAULT_NEW_EMOJI },
                            title = localizedCategoryTitle(category),
                            subcategoryCount = subCount,
                            onClick = { editingCategory = category }
                        )
                    }
                }

                // "New category" CTA — full-width black pill at the bottom,
                // matching the design language Accounts uses for "New
                // account type" and Transactions uses for "Add transaction".
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
                        text = "+ ${stringResource(Res.string.add_category)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.background
                    )
                }
            }
        }
    }

    if (embedded) {
        body(PaddingValues(0.dp))
    } else {
        Scaffold(
            topBar = {
                Toolbars.Primary(
                    title = stringResource(Res.string.transaction_categories),
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
private fun CategoryListRow(
    emoji: String,
    title: String,
    subcategoryCount: Int,
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
        if (subcategoryCount > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = subcategoryCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.width(8.dp))
        }
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
internal fun AddCategoryNameSheet(
    type: CategoryType,
    onDismiss: () -> Unit,
    onAdd: (name: String, emoji: String) -> Unit
) {
    MooneyBottomSheet(onDismissRequest = onDismiss) {
        var name by remember { mutableStateOf("") }
        var emoji by remember { mutableStateOf(DEFAULT_NEW_EMOJI) }
        var showIconPicker by remember { mutableStateOf(false) }
        val canSave = name.isNotBlank()

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            EditSheetTopBar(
                title = stringResource(
                    if (type == CategoryType.EXPENSE) Res.string.new_expense_category
                    else Res.string.new_income_category
                ),
                saveLabel = stringResource(Res.string.save),
                saveEnabled = canSave,
                onCancel = onDismiss,
                onSave = { if (canSave) onAdd(name.trim(), emoji) }
            )
            Spacer(Modifier.height(20.dp))
            CategoryHero(emoji = emoji)
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
                onValueChange = { name = it },
                placeholder = { Text(stringResource(Res.string.category_name)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            IconPickerToggle(
                emoji = emoji,
                expanded = showIconPicker,
                onToggle = { showIconPicker = !showIconPicker }
            )

            if (showIconPicker) {
                Spacer(Modifier.height(12.dp))
                EmojiGrid(selected = emoji, onSelect = { emoji = it })
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditSheet(
    category: Category,
    subcategories: List<Category>,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
    onAddSubcategory: (String) -> Unit,
    onDeleteSubcategory: (String) -> Unit,
    onDeleteCategory: () -> Unit
) {
    MooneyBottomSheet(onDismissRequest = onDismiss) {
        val localizedTitle = localizedCategoryTitle(category)
        var name by remember(category.id) { mutableStateOf(localizedTitle) }
        var newSubName by remember { mutableStateOf("") }
        var showIconPicker by remember { mutableStateOf(false) }
        val emoji = category.resolveEmoji().ifBlank { DEFAULT_NEW_EMOJI }

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            EditSheetTopBar(
                title = stringResource(Res.string.edit_category),
                saveLabel = stringResource(Res.string.save),
                saveEnabled = name.isNotBlank() && name.trim() != localizedTitle && name.trim() != category.title,
                onCancel = onDismiss,
                onSave = {
                    onSaveName(name.trim())
                    onDismiss()
                }
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
                onValueChange = { name = it },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            IconPickerToggle(
                emoji = emoji,
                expanded = showIconPicker,
                onToggle = { showIconPicker = !showIconPicker }
            )

            // Hint: icon edits aren't persisted yet — VM upsert path keeps the
            // original. Visual picker stays so users can preview the swap;
            // wire to a real rename/icon-update when the VM supports it.
            if (showIconPicker) {
                Spacer(Modifier.height(12.dp))
                EmojiGrid(selected = emoji, onSelect = { /* preview-only */ })
            }

            Spacer(Modifier.height(20.dp))
            FormSectionLabel("SUBCATEGORIES · OPTIONAL")

            subcategories.forEach { sub ->
                SubcategoryChip(
                    title = localizedCategoryTitle(sub),
                    onDelete = { onDeleteSubcategory(sub.id) }
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newSubName,
                    onValueChange = { newSubName = it },
                    placeholder = { Text(stringResource(Res.string.add_subcategory)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    onClick = {
                        if (newSubName.isNotBlank()) {
                            onAddSubcategory(newSubName.trim())
                            newSubName = ""
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                    .clickable { onDeleteCategory() }
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
        modifier = Modifier
            .fillMaxWidth(),
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

@Composable
private fun IconPickerToggle(
    emoji: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (expanded) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Icon",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(emoji, fontSize = 20.sp)
        }
    }
}

@Composable
private fun EmojiGrid(selected: String, onSelect: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .padding(12.dp)
                .heightIn(max = 220.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            gridItems(EMOJI_PALETTE) { e ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (e == selected) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable { onSelect(e) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(e, fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun SubcategoryChip(title: String, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "✕",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clickable { onDelete() }
                .padding(start = 8.dp)
        )
    }
}
