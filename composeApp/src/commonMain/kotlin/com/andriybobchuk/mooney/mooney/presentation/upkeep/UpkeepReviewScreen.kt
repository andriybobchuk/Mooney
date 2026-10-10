package com.andriybobchuk.mooney.mooney.presentation.upkeep

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andriybobchuk.mooney.mooney.data.localizedCategoryTitle
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import kotlinx.datetime.LocalDate
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.upkeep_all_caught_up_body
import mooney.composeapp.generated.resources.upkeep_all_caught_up_title
import mooney.composeapp.generated.resources.upkeep_card_header
import mooney.composeapp.generated.resources.upkeep_done_button
import mooney.composeapp.generated.resources.upkeep_more_categories
import mooney.composeapp.generated.resources.upkeep_title
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen Upkeep review flow.
 *
 * Entered via the "⚡ N auto-added to review" banner on the Transactions
 * tab when there's a non-zero queue of auto-added transactions. Shows one
 * transaction at a time as a card with a chip grid of frequently-used
 * expense categories. Tap a chip → category commits + card advances. No
 * skip button, no delete; the only way out is to pick a category per card
 * or close the screen.
 *
 * See [UpkeepReviewViewModel] for state semantics + telemetry hooks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpkeepReviewScreen(
    viewModel: UpkeepReviewViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            UpkeepTopBar(
                state = state,
                onClose = {
                    viewModel.onAction(UpkeepReviewAction.OnDismiss)
                    onBackClick()
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.isCompleted -> {
                    AllCaughtUpContent(onDone = onBackClick)
                }
                state.currentCard != null -> {
                    CardContent(
                        transaction = state.currentCard!!,
                        frequentCategories = state.frequentCategories,
                        onCategoryPicked = { category ->
                            viewModel.onAction(UpkeepReviewAction.PickCategory(category))
                        },
                        onMoreClicked = {
                            viewModel.onAction(UpkeepReviewAction.OpenCategoryPicker)
                        }
                    )
                }
            }
        }

        if (state.isCategoryPickerOpen) {
            CategoryPickerSheet(
                categories = state.allExpenseCategories,
                currentCategoryId = state.currentCard?.subcategory?.id,
                onPick = { category ->
                    viewModel.onAction(UpkeepReviewAction.PickCategory(category))
                },
                onDismiss = {
                    viewModel.onAction(UpkeepReviewAction.DismissCategoryPicker)
                }
            )
        }
    }
}

@Composable
private fun UpkeepTopBar(
    state: UpkeepReviewState,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.upkeep_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            if (!state.isCompleted && state.totalCount > 0) {
                Text(
                    text = stringResource(Res.string.upkeep_card_header, state.progressLabel),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CardContent(
    transaction: Transaction,
    frequentCategories: List<Category>,
    onCategoryPicked: (Category) -> Unit,
    onMoreClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TransactionCard(transaction = transaction)
        Spacer(modifier = Modifier.height(28.dp))
        CategoryChipGrid(
            categories = frequentCategories,
            currentCategoryId = transaction.subcategory.id,
            onPick = onCategoryPicked
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.upkeep_more_categories),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onMoreClicked() }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun TransactionCard(transaction: Transaction) {
    val isExpense = transaction.subcategory.type ==
        com.andriybobchuk.mooney.mooney.domain.CategoryType.EXPENSE
    val amountColor = if (isExpense) {
        Color(0xFFDC2626) // red for expenses
    } else {
        Color(0xFF16A34A) // green for income
    }
    val sign = if (isExpense) "-" else "+"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(transaction.subcategory.resolveEmoji(), fontSize = 36.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "$sign${transaction.amount.formatWithCommas()} ${transaction.account.currency.symbol}",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = amountColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = transaction.description?.takeIf { it.isNotBlank() }
                ?: localizedCategoryTitle(transaction.subcategory),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = formatCardSubtitle(transaction),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatCardSubtitle(tx: Transaction): String {
    val datePart = tx.date.monthShortLabel() + " " + tx.date.dayOfMonth
    return "$datePart · ${tx.account.title}"
}

private fun LocalDate.monthShortLabel(): String =
    this.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }

@Composable
private fun CategoryChipGrid(
    categories: List<Category>,
    currentCategoryId: String,
    onPick: (Category) -> Unit
) {
    // 2-column grid rendered as a Column of Rows — no need to pull in
    // LazyVerticalGrid for 6 items that always fit above the fold.
    val rows = categories.chunked(2)
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { cat ->
                    CategoryChip(
                        category = cat,
                        isSelected = cat.id == currentCategoryId,
                        modifier = Modifier.weight(1f),
                        onClick = { onPick(cat) }
                    )
                }
                // Fill the trailing empty slot in an odd-count row so the
                // last chip doesn't stretch across the full width.
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(category.resolveEmoji(), fontSize = 20.sp)
        Text(
            text = localizedCategoryTitle(category),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )
        if (isSelected) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    categories: List<Category>,
    currentCategoryId: String?,
    onPick: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories, key = { it.id }) { cat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(cat) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(cat.resolveEmoji(), fontSize = 20.sp)
                    Text(
                        text = localizedCategoryTitle(cat),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (cat.id == currentCategoryId) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllCaughtUpContent(onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "🎉", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(Res.string.upkeep_all_caught_up_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.upkeep_all_caught_up_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = stringResource(Res.string.upkeep_done_button),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable { onDone() }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        )
    }
}
