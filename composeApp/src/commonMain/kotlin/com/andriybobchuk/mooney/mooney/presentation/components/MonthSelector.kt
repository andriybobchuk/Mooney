package com.andriybobchuk.mooney.mooney.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andriybobchuk.mooney.core.presentation.Icons as MooneyIcons
import com.andriybobchuk.mooney.mooney.domain.MonthKey
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.cd_next_month
import mooney.composeapp.generated.resources.cd_next_year
import mooney.composeapp.generated.resources.cd_previous_month
import mooney.composeapp.generated.resources.cd_previous_year
import mooney.composeapp.generated.resources.months_n_below_caption
import org.jetbrains.compose.resources.stringResource

/**
 * Reusable month selector — chevron-left, pill, chevron-right. Tapping the pill
 * opens a bottom sheet with a year nav + 3×4 month grid. Optionally each grid
 * cell shows a count under the label (transaction count for that month).
 *
 * Used by both the Transactions and Analytics screens so the affordance is
 * identical wherever the user changes month.
 *
 * ## Future-month gating
 *
 * The selector normally caps at the current month, but any FUTURE month that
 * has at least one transaction (i.e. `monthlyCounts[m] > 0` for `m > now`) is
 * also reachable — this lets users plan forward by dating a transaction in a
 * future month and then reviewing it there. Cells for future months with zero
 * transactions stay disabled so the grid doesn't invite empty navigation.
 *
 * @param monthlyCounts per-month transaction count. Used BOTH to render the
 *   small caption under each month cell AND to compute which future months
 *   are selectable. Past-month counts are cosmetic; future-month counts are
 *   load-bearing.
 */
@Composable
fun MonthSelector(
    selectedMonth: MonthKey,
    onMonthSelected: (MonthKey) -> Unit,
    modifier: Modifier = Modifier,
    monthlyCounts: Map<MonthKey, Int> = emptyMap()
) {
    val currentMonth = remember { MonthKey.current() }
    // The effective upper bound: today OR the furthest future month with data
    // (whichever is greater). This is what canGoForward + the picker's year
    // chevron key off of.
    val upperBound = remember(currentMonth, monthlyCounts) {
        monthlyCounts
            .filter { (m, count) ->
                count > 0 && (m.year > currentMonth.year ||
                    (m.year == currentMonth.year && m.month > currentMonth.month))
            }
            .keys
            .maxByOrNull { it.year * 12 + it.month }
            ?: currentMonth
    }
    val canGoForward = remember(selectedMonth, upperBound) {
        selectedMonth.year < upperBound.year ||
            (selectedMonth.year == upperBound.year && selectedMonth.month < upperBound.month)
    }
    var showSheet by remember { mutableStateOf(false) }
    // Build the chip label from localized month name + year so it follows
    // the device language instead of always being English ("Jun 2026").
    val localizedMonth = com.andriybobchuk.mooney.core.presentation.i18n.localizedMonthName(
        selectedMonth.month, short = true
    )
    val monthName = "$localizedMonth ${selectedMonth.year}"

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MonthStepperButton(
            onClick = { onMonthSelected(selectedMonth.previousMonth()) },
            painter = MooneyIcons.ChevronLeftIcon(),
            contentDescription = stringResource(Res.string.cd_previous_month),
            enabled = true
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .clickable { showSheet = true }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        MonthStepperButton(
            onClick = { if (canGoForward) onMonthSelected(selectedMonth.nextMonth()) },
            painter = MooneyIcons.ChevronRightIcon(),
            contentDescription = stringResource(Res.string.cd_next_month),
            enabled = canGoForward
        )
    }

    if (showSheet) {
        YearMonthPickerSheet(
            initialMonth = selectedMonth,
            currentMonth = currentMonth,
            upperBound = upperBound,
            monthlyCounts = monthlyCounts,
            onPick = { picked ->
                onMonthSelected(picked)
                showSheet = false
            },
            onDismiss = { showSheet = false }
        )
    }
}

@Composable
private fun MonthStepperButton(
    onClick: () -> Unit,
    painter: Painter,
    contentDescription: String,
    enabled: Boolean
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.35f else 0.15f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.35f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearMonthPickerSheet(
    initialMonth: MonthKey,
    currentMonth: MonthKey,
    upperBound: MonthKey,
    monthlyCounts: Map<MonthKey, Int>,
    onPick: (MonthKey) -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember { mutableStateOf(initialMonth.year) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MonthStepperButton(
                        onClick = { year-- },
                        painter = MooneyIcons.ChevronLeftIcon(),
                        contentDescription = stringResource(Res.string.cd_previous_year),
                        enabled = true
                    )
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    MonthStepperButton(
                        onClick = { year++ },
                        painter = MooneyIcons.ChevronRightIcon(),
                        contentDescription = stringResource(Res.string.cd_next_year),
                        // Allow the year chevron all the way to the year of
                        // the furthest future-with-data month — otherwise a
                        // user with, say, a Jan-2027 planned tx couldn't
                        // navigate off 2026 to reach it.
                        enabled = year < upperBound.year
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val monthLabels = (1..12).map {
                    com.andriybobchuk.mooney.core.presentation.i18n.localizedMonthName(it, short = true)
                }
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0 until 3) {
                            val monthIndex = row * 3 + col
                            val monthNumber = monthIndex + 1
                            val cellMonth = MonthKey(year, monthNumber)
                            val isSelected = cellMonth == initialMonth
                            val isFuture = year > currentMonth.year ||
                                (year == currentMonth.year && monthNumber > currentMonth.month)
                            val count = monthlyCounts[cellMonth] ?: 0
                            // Future months are reachable only when they hold
                            // real user data (planned txs). Empty future cells
                            // stay disabled so the picker doesn't invite
                            // navigation to blank state.
                            val isDisabled = isFuture && count == 0

                            MonthGridCell(
                                label = monthLabels[monthIndex],
                                count = count.takeIf { it > 0 },
                                isSelected = isSelected,
                                isDisabled = isDisabled,
                                onClick = {
                                    if (!isDisabled) onPick(cellMonth)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Hint so the small number under each month label doesn't read
                // as a mystery. Only shown when we're actually rendering counts.
                if (monthlyCounts.values.any { it > 0 }) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.months_n_below_caption),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthGridCell(
    label: String,
    count: Int?,
    isSelected: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.inverseSurface
        else -> Color.Transparent
    }
    val fg = when {
        isSelected -> MaterialTheme.colorScheme.inverseOnSurface
        isDisabled -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.onBackground
    }
    // When we have a count, the cell needs more vertical room so the caption
    // doesn't crowd the label. Stays at the same 40dp when no counts are passed.
    val cellHeight = if (count != null) 52.dp else 40.dp
    Box(
        modifier = modifier
            .height(cellHeight)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(enabled = !isDisabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = fg
            )
            if (count != null) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = fg.copy(alpha = 0.6f)
                )
            }
        }
    }
}
