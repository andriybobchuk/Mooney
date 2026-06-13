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
import mooney.composeapp.generated.resources.months_n_below_caption
import org.jetbrains.compose.resources.stringResource

/**
 * Reusable month selector — chevron-left, pill, chevron-right. Tapping the pill
 * opens a bottom sheet with a year nav + 3×4 month grid. Optionally each grid
 * cell shows a count under the label (transaction count for that month).
 *
 * Used by both the Transactions and Analytics screens so the affordance is
 * identical wherever the user changes month. Forward stepping past the current
 * month is disabled (Mooney has no meaningful future-month data).
 *
 * @param monthlyCounts optional per-month value rendered as a small caption
 *   under each month cell (typically the transaction count for that month).
 *   Missing or zero values render no caption.
 */
@Composable
fun MonthSelector(
    selectedMonth: MonthKey,
    onMonthSelected: (MonthKey) -> Unit,
    modifier: Modifier = Modifier,
    monthlyCounts: Map<MonthKey, Int> = emptyMap()
) {
    val currentMonth = remember { MonthKey.current() }
    val canGoForward = remember(selectedMonth, currentMonth) {
        selectedMonth.year < currentMonth.year ||
            (selectedMonth.year == currentMonth.year && selectedMonth.month < currentMonth.month)
    }
    var showSheet by remember { mutableStateOf(false) }
    val monthName = remember(selectedMonth) { selectedMonth.toShortDisplayString() }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MonthStepperButton(
            onClick = { onMonthSelected(selectedMonth.previousMonth()) },
            painter = MooneyIcons.ChevronLeftIcon(),
            contentDescription = "Previous month",
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
            contentDescription = "Next month",
            enabled = canGoForward
        )
    }

    if (showSheet) {
        YearMonthPickerSheet(
            initialMonth = selectedMonth,
            currentMonth = currentMonth,
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
                        contentDescription = "Previous year",
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
                        contentDescription = "Next year",
                        enabled = year < currentMonth.year
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val monthLabels = listOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
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

                            MonthGridCell(
                                label = monthLabels[monthIndex],
                                count = count.takeIf { it > 0 },
                                isSelected = isSelected,
                                isDisabled = isFuture,
                                onClick = {
                                    if (!isFuture) onPick(cellMonth)
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
