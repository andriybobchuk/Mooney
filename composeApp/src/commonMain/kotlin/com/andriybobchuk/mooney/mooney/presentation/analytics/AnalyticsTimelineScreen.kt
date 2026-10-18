package com.andriybobchuk.mooney.mooney.presentation.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andriybobchuk.mooney.app.appColors
import com.andriybobchuk.mooney.core.presentation.designsystem.components.EnhancedMeshBackground
import com.andriybobchuk.mooney.core.platform.LockLandscape
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.MonthlyMetricSnapshot
import com.andriybobchuk.mooney.mooney.domain.formatToShortString
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.close
import mooney.composeapp.generated.resources.net_income_title
import mooney.composeapp.generated.resources.operating_costs
import mooney.composeapp.generated.resources.revenue
import mooney.composeapp.generated.resources.taxes
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen zoomable Analytics timeline. The main Analytics screen is a
 * portrait-first "my current month at a glance" view; this surface is the
 * opposite — stretch the entire account history across a scrollable chart
 * so the user can hunt for anomalies / seasonality / trajectory shifts
 * without squinting at the compressed 6-month strip.
 *
 * Design decisions:
 * - **Multi-line chart with the same colors as the main Analytics
 *   TrendChart** (green / orange / red / blue). Users already associate
 *   each color with a metric from the main screen; reusing them here
 *   means zero relearn.
 * - **Pinch-zoom scales the per-month width** (not Compose's Modifier.scale,
 *   which would also balloon the stroke widths and labels). Horizontal
 *   scroll follows the finger naturally once a column is wider than the
 *   viewport.
 * - **Tap anywhere → select that month.** The vertical crosshair follows
 *   and the floating card above the crosshair updates in place —
 *   previously the card was pinned to the top of the screen; moving it
 *   next to the selector means the eye never has to leave the chart.
 * - **Smoothed lines** (catmull-rom cubic beziers) so the trajectory
 *   reads as a continuous story rather than a zigzag of month-to-month
 *   deltas.
 * - **Mesh gradient background** so the surface feels distinct from the
 *   portrait analytics screen — it's a drill-in view, treat it as a hero.
 */
@Composable
fun AnalyticsTimelineScreen(
    viewModel: AnalyticsViewModel,
    onBackClick: () -> Unit
) {
    LockLandscape(enabled = true)

    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadLifetimeData() }

    val lifetime = state.lifetimeMetrics
    val data = remember(lifetime, state.historicalMetrics) {
        val source = if (lifetime.isNotEmpty()) lifetime else state.historicalMetrics
        source.dropWhile {
            it.transactionCount == 0 && it.revenue == 0.0 &&
                it.operatingCosts == 0.0 && it.taxes == 0.0
        }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        EnhancedMeshBackground(modifier = Modifier.fillMaxSize())

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = topInset + 4.dp, end = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(Res.string.close),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        if (data.isEmpty()) {
            Text(
                text = "Loading timeline…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        TimelineChart(
            data = data,
            selectedMonth = state.selectedMonth,
            onMonthTapped = viewModel::onMonthSelected,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset + 56.dp, bottom = bottomInset + 16.dp)
        )
    }
}

/** Metric colors — must match the main Analytics TrendChart so users
 *  carry the color→metric association between screens. */
private val RevenueColor = Color(0xFF4CAF50)
private val TaxesColor = Color(0xFFFF9800)
private val OperatingCostsColor = Color(0xFFF44336)
private val NetIncomeColor = Color(0xFF2196F3)

/**
 * Scrollable + pinch-zoomable multi-line timeline. The chart's own width
 * is `data.size * monthWidth` — a horizontal scroll modifier gives
 * free panning, pinch gestures mutate `monthWidth` to zoom.
 */
@Composable
private fun TimelineChart(
    data: List<MonthlyMetricSnapshot>,
    selectedMonth: MonthKey,
    onMonthTapped: (MonthKey) -> Unit,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    var monthWidthDp by remember { mutableStateOf(56f) }

    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
    val selectorColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)

    BoxWithConstraints(modifier = modifier) {
        val viewportWidth = maxWidth
        val maxWidth = 240f
        val minWidth = 24f

        val chartWidthDp = (monthWidthDp * data.size).dp
        val leftPadDp = (viewportWidth.value / 2 - monthWidthDp / 2).coerceAtLeast(0f).dp

        LaunchedEffect(selectedMonth, data.size, monthWidthDp) {
            val idx = data.indexOfFirst { it.month == selectedMonth }
            if (idx >= 0) {
                val centerPx = with(density) {
                    (leftPadDp.value + monthWidthDp * idx + monthWidthDp / 2 - viewportWidth.value / 2).dp.toPx()
                }
                scrollState.scrollTo(centerPx.toInt().coerceAtLeast(0))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .pointerInput(data.size) {
                    detectTransformGestures { _, _, zoom, _ ->
                        monthWidthDp = (monthWidthDp * zoom).coerceIn(minWidth, maxWidth)
                    }
                }
        ) {
            Spacer(modifier = Modifier.width(leftPadDp))
            // The chart canvas + the floating card overlay for the
            // selected month sit in the same Box so the card can position
            // itself to the crosshair's x — moves with the selection.
            Box(
                modifier = Modifier
                    .width(chartWidthDp)
                    .fillMaxSize()
                    .pointerInput(data.size, monthWidthDp) {
                        detectTapGestures { offset ->
                            val xDp = with(density) { offset.x.toDp().value }
                            val idx = (xDp / monthWidthDp).toInt().coerceIn(0, data.size - 1)
                            onMonthTapped(data[idx].month)
                        }
                    }
            ) {
                TimelineCanvas(
                    data = data,
                    selectedMonth = selectedMonth,
                    monthWidthDp = monthWidthDp,
                    gridColor = gridColor,
                    selectorColor = selectorColor,
                    modifier = Modifier.fillMaxSize()
                )

                // Floating card tied to the selector's x position. Clamped
                // via Modifier.offset so it travels with the user's taps
                // instead of being pinned to a top-left slot. Horizontal
                // offset is `index * monthWidthDp - halfCardWidth`,
                // vertical offset keeps it near the top of the chart.
                val selectedIdx = data.indexOfFirst { it.month == selectedMonth }
                if (selectedIdx >= 0) {
                    val snapshot = data[selectedIdx]
                    val cardWidthDp = 180.dp
                    val cardX = (monthWidthDp * selectedIdx + monthWidthDp / 2).dp - cardWidthDp / 2
                    FloatingMonthCard(
                        snapshot = snapshot,
                        modifier = Modifier
                            .width(cardWidthDp)
                            .offset(x = cardX, y = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(leftPadDp))
        }

        // Legend pinned to the bottom-left of the chart area — stays
        // onscreen regardless of pan position.
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LegendDot(RevenueColor, stringResource(Res.string.revenue))
            LegendDot(TaxesColor, stringResource(Res.string.taxes))
            LegendDot(OperatingCostsColor, stringResource(Res.string.operating_costs))
            LegendDot(NetIncomeColor, stringResource(Res.string.net_income_title))
        }
    }
}

@Composable
private fun FloatingMonthCard(
    snapshot: MonthlyMetricSnapshot,
    modifier: Modifier = Modifier
) {
    val currencySymbol = GlobalConfig.baseCurrency.symbol
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = snapshot.month.toDisplayString(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        FloatingMetricRow(stringResource(Res.string.revenue), snapshot.revenue, RevenueColor, currencySymbol)
        FloatingMetricRow(stringResource(Res.string.taxes), snapshot.taxes, TaxesColor, currencySymbol)
        FloatingMetricRow(stringResource(Res.string.operating_costs), snapshot.operatingCosts, OperatingCostsColor, currencySymbol)
        FloatingMetricRow(
            stringResource(Res.string.net_income_title),
            snapshot.netIncome,
            if (snapshot.netIncome >= 0) MaterialTheme.appColors.incomeColor else MaterialTheme.appColors.expenseColor,
            currencySymbol
        )
    }
}

@Composable
private fun FloatingMetricRow(
    label: String,
    value: Double,
    color: Color,
    currencySymbol: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${value.formatToShortString()} $currencySymbol",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun TimelineCanvas(
    data: List<MonthlyMetricSnapshot>,
    selectedMonth: MonthKey,
    monthWidthDp: Float,
    gridColor: Color,
    selectorColor: Color,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOf {
        maxOf(it.revenue, it.operatingCosts, it.taxes, it.netIncome.coerceAtLeast(0.0))
    }.coerceAtLeast(1.0)
    val minValue = data.minOf { it.netIncome }.coerceAtMost(0.0)
    val valueRange = (maxValue - minValue).coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        val stepX = monthWidthDp.dp.toPx()
        val chartTopPx = 24.dp.toPx()
        val chartBottomPx = size.height - 36.dp.toPx()
        val chartHeightPx = (chartBottomPx - chartTopPx).coerceAtLeast(1f)

        fun yFor(value: Double): Float {
            val norm = ((value - minValue) / valueRange).toFloat().coerceIn(0f, 1f)
            return chartBottomPx - norm * chartHeightPx
        }

        for (q in 0..4) {
            val y = chartTopPx + q * chartHeightPx / 4
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }
        if (minValue < 0.0) {
            val zeroY = yFor(0.0)
            drawLine(
                color = gridColor.copy(alpha = 0.6f),
                start = Offset(0f, zeroY),
                end = Offset(size.width, zeroY),
                strokeWidth = 1.5f
            )
        }

        drawSmoothedMetricLine(data, RevenueColor, stepX, minValue, valueRange, chartBottomPx, chartHeightPx) { it.revenue }
        drawSmoothedMetricLine(data, TaxesColor, stepX, minValue, valueRange, chartBottomPx, chartHeightPx) { it.taxes }
        drawSmoothedMetricLine(data, OperatingCostsColor, stepX, minValue, valueRange, chartBottomPx, chartHeightPx) { it.operatingCosts }
        drawSmoothedMetricLine(data, NetIncomeColor, stepX, minValue, valueRange, chartBottomPx, chartHeightPx) { it.netIncome }

        val selectedIdx = data.indexOfFirst { it.month == selectedMonth }
        if (selectedIdx >= 0) {
            val x = stepX * selectedIdx + stepX / 2
            drawLine(
                color = selectorColor,
                start = Offset(x, chartTopPx),
                end = Offset(x, chartBottomPx),
                strokeWidth = 1.5f
            )
            val snapshot = data[selectedIdx]
            drawCircle(RevenueColor, radius = 4.dp.toPx(), center = Offset(x, yFor(snapshot.revenue)))
            drawCircle(TaxesColor, radius = 4.dp.toPx(), center = Offset(x, yFor(snapshot.taxes)))
            drawCircle(OperatingCostsColor, radius = 4.dp.toPx(), center = Offset(x, yFor(snapshot.operatingCosts)))
            drawCircle(NetIncomeColor, radius = 4.dp.toPx(), center = Offset(x, yFor(snapshot.netIncome)))
        }

        val skip = when {
            monthWidthDp >= 72 -> 1
            monthWidthDp >= 48 -> 2
            monthWidthDp >= 32 -> 3
            else -> 6
        }
        for (i in data.indices step skip) {
            val x = stepX * i + stepX / 2
            drawLine(
                color = gridColor,
                start = Offset(x, chartBottomPx),
                end = Offset(x, chartBottomPx + 4.dp.toPx()),
                strokeWidth = 1f
            )
        }
    }
}

/**
 * Draws a smooth catmull-rom-to-cubic-bezier path through the data for
 * one metric. Replaces straight `lineTo` segments, which read as a
 * zigzag when neighbouring months differ sharply. Catmull-rom with
 * tension 0.5 gives a visually appealing curve that still passes exactly
 * through every data point (so you can read values at known months).
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmoothedMetricLine(
    data: List<MonthlyMetricSnapshot>,
    color: Color,
    stepX: Float,
    minValue: Double,
    valueRange: Double,
    chartBottomPx: Float,
    chartHeightPx: Float,
    valueOf: (MonthlyMetricSnapshot) -> Double
) {
    if (data.size < 2) return
    val points = data.mapIndexed { index, snapshot ->
        val norm = ((valueOf(snapshot) - minValue) / valueRange).toFloat().coerceIn(0f, 1f)
        Offset(
            x = stepX * index + stepX / 2,
            y = chartBottomPx - norm * chartHeightPx
        )
    }
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 0 until points.size - 1) {
            val p0 = points[(i - 1).coerceAtLeast(0)]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[(i + 2).coerceAtMost(points.size - 1)]
            // Catmull-Rom to Bezier conversion with tension 0.5.
            val cp1x = p1.x + (p2.x - p0.x) / 6f
            val cp1y = p1.y + (p2.y - p0.y) / 6f
            val cp2x = p2.x - (p3.x - p1.x) / 6f
            val cp2y = p2.y - (p3.y - p1.y) / 6f
            cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
        }
    }
    drawPath(path = path, color = color, style = Stroke(width = 2.5.dp.toPx()))
}
