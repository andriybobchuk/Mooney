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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MeshGradientBackground
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.andriybobchuk.mooney.app.appColors
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
        // Solid theme background — the mesh gradient only shows in the
        // middle content area (see Box below). Otherwise the mesh bled into
        // the status bar + home-indicator strip, producing a weird gradient
        // under the chrome that didn't match either light or dark theme.
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
                .navigationBarsPadding()
        ) {
            // `prominent = true` — user asked for a stronger mesh background
            // on Review for better contrast between the card and the backdrop.
            MeshGradientBackground(prominent = true)
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
                state.queue.isNotEmpty() -> {
                    PagerContent(
                        state = state,
                        onPickCategory = { category ->
                            viewModel.onAction(UpkeepReviewAction.PickCategory(category))
                        },
                        onMoreClicked = {
                            viewModel.onAction(UpkeepReviewAction.OpenCategoryPicker)
                        },
                        onSwipedToPage = { index ->
                            viewModel.onAction(UpkeepReviewAction.SwipedToPage(index))
                        },
                        onShiftDate = { deltaDays ->
                            viewModel.onAction(UpkeepReviewAction.ShiftCurrentCardDate(deltaDays))
                        }
                    )
                }
            }
        }

        // Reuse the SAME multilevel picker the Add Transaction sheet uses —
        // category tab (expense/income), general-category list with drill-down
        // into subcategories, inline "add category", etc. We get all of that
        // + the proven design/UX in exchange for a few lines of adapter code.
        val card = state.currentCard
        if (state.isCategoryPickerOpen && card != null) {
            val currentGeneral = card.subcategory.parent?.getRoot() ?: card.subcategory
            com.andriybobchuk.mooney.mooney.presentation.transaction.CategorySelectionBottomSheet(
                onDismiss = {
                    viewModel.onAction(UpkeepReviewAction.DismissCategoryPicker)
                },
                sheetState = androidx.compose.material3.rememberModalBottomSheetState(
                    skipPartiallyExpanded = true,
                    confirmValueChange = { true }
                ),
                categories = state.allCategories,
                initialSelectedCategory = currentGeneral,
                initialSelectedSubCategory = card.subcategory.takeIf { it.isSubCategory() },
                onCategorySelected = { general, sub ->
                    // Prefer the drilled-down subcategory. If the user picked a
                    // top-level general with no children, use that id instead.
                    val chosen = sub ?: general
                    viewModel.onAction(UpkeepReviewAction.PickCategory(chosen))
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
            // Scaffold with a custom topBar doesn't auto-apply status bar
            // insets — do it explicitly so the Close button and progress
            // text clear the Dynamic Island / notch.
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Matches the Mooney 44dp round nav button used on every other top
        // bar (see Toolbars.kt back-button pattern) so the chrome feels
        // consistent instead of a plain Material IconButton.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
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

/**
 * Pager-wrapped card content. User can swipe freely between pending auto-
 * added transactions; tapping a chip commits the current page's card AND
 * advances to the next page. The swipe-driven animation gives a strong
 * "this is a different transaction" cue the previous single-card flow
 * lacked.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PagerContent(
    state: UpkeepReviewState,
    onPickCategory: (Category) -> Unit,
    onMoreClicked: () -> Unit,
    onSwipedToPage: (Int) -> Unit,
    onShiftDate: (Int) -> Unit
) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = state.currentIndex.coerceAtMost((state.queue.size - 1).coerceAtLeast(0))
    ) { state.queue.size }

    // Keep VM in sync with the pager — fires once per settled page.
    androidx.compose.runtime.LaunchedEffect(pagerState) {
        androidx.compose.runtime.snapshotFlow { pagerState.settledPage }.collect { page ->
            onSwipedToPage(page)
        }
    }
    // Keep the pager in sync with VM-driven index changes (chip tap advances).
    androidx.compose.runtime.LaunchedEffect(state.currentIndex) {
        if (state.currentIndex < state.queue.size &&
            pagerState.currentPage != state.currentIndex) {
            pagerState.animateScrollToPage(state.currentIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        com.andriybobchuk.mooney.core.presentation.designsystem.components.SegmentedPagerIndicator(
            currentStep = pagerState.currentPage,
            totalSteps = state.queue.size,
            pagerOffset = pagerState.currentPageOffsetFraction,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val transaction = state.queue.getOrNull(page) ?: return@HorizontalPager
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TransactionCard(
                    transaction = transaction,
                    onShiftDate = onShiftDate
                )
                Spacer(modifier = Modifier.height(24.dp))
                CategoryChipGrid(
                    categories = state.frequentCategories,
                    currentCategoryId = transaction.subcategory.id,
                    onPick = onPickCategory
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.TextButton(
                    onClick = onMoreClicked
                ) {
                    Text(
                        text = stringResource(Res.string.upkeep_more_categories),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: Transaction,
    onShiftDate: (Int) -> Unit
) {
    val isExpense = transaction.subcategory.type ==
        com.andriybobchuk.mooney.mooney.domain.CategoryType.EXPENSE
    // Theme-aware red/green from the shared tokens — same ones the main
    // transactions list uses, so colors stay consistent across the app
    // and respect dark/light mode.
    val amountColor = if (isExpense) {
        MaterialTheme.appColors.expenseColor
    } else {
        MaterialTheme.appColors.incomeColor
    }
    val sign = if (isExpense) "-" else "+"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            // Opaque surface sits on top of the Mesh background so the card
            // reads as a distinct container. `surface` (not surfaceVariant)
            // gives higher contrast against the mesh.
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
            maxLines = 2,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "· ${transaction.account.title}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        DateStepperRow(
            date = transaction.date,
            onShiftDate = onShiftDate
        )
    }
}

/**
 * Prev / next day arrow buttons flanking the current card's date. Same
 * 36dp round-button visual language as [MonthStepperButton] in the top-
 * bar MonthSelector so the user recognises the control. Tapping either
 * arrow fires [ShiftCurrentCardDate]; the VM persists + the queue updates
 * in place so the card re-renders with the new date.
 */
@Composable
private fun DateStepperRow(
    date: LocalDate,
    onShiftDate: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DateStepperButton(
            onClick = { onShiftDate(-1) },
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date.toString(),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        DateStepperButton(
            onClick = { onShiftDate(1) },
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight
        )
    }
}

@Composable
private fun DateStepperButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CategoryChipGrid(
    categories: List<Category>,
    currentCategoryId: String,
    onPick: (Category) -> Unit
) {
    // 2-column grid rendered as a Column of Rows — no need to pull in
    // LazyVerticalGrid for 8 items that always fit above the fold. Chip
    // count bumped from 6 to 8 (4 rows) so a wider spread of recent
    // categories is one tap away before the user has to drill into
    // "More categories…".
    val rows = categories.chunked(2)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
    // Opaque surface so chips read cleanly against the mesh background.
    // Selected chip filled with primaryContainer; its on-color is theme-
    // tuned for both light and dark modes.
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
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
            color = textColor,
            maxLines = 2
        )
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
