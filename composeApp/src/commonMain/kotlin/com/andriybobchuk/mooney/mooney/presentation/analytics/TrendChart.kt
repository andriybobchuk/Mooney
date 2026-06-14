package com.andriybobchuk.mooney.mooney.presentation.analytics

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.MonthlyMetricSnapshot
import kotlin.math.max
import kotlin.math.min
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.period_1y
import mooney.composeapp.generated.resources.period_6mo
import mooney.composeapp.generated.resources.period_lifetime
import mooney.composeapp.generated.resources.trend_not_enough_data
import org.jetbrains.compose.resources.stringResource

enum class TimePeriod(val displayName: String, val months: Int) {
    SIX_MONTHS("6mo", 6),
    ONE_YEAR("1y", 12),
    LIFETIME("Lifetime", Int.MAX_VALUE)
}

@Composable
fun TrendChart(
    historicalData: List<MonthlyMetricSnapshot>,
    selectedMonth: MonthKey,
    modifier: Modifier = Modifier,
    selectedPeriod: TimePeriod = TimePeriod.SIX_MONTHS,
    onPeriodSelected: (TimePeriod) -> Unit = {},
    lifetimeData: List<MonthlyMetricSnapshot> = emptyList(),
    isLifetimeLoading: Boolean = false
) {

    // Filter data based on selected period.
    // Lifetime: use the lazily-loaded wider dataset, drop empty leading months.
    // Otherwise: last N months from the standard window.
    val filteredData = if (selectedPeriod == TimePeriod.LIFETIME) {
        val firstNonEmpty = lifetimeData.indexOfFirst {
            it.transactionCount > 0 || it.revenue != 0.0 || it.operatingCosts != 0.0
        }
        if (firstNonEmpty == -1) emptyList() else lifetimeData.drop(firstNonEmpty)
    } else {
        historicalData.takeLast(selectedPeriod.months)
    }

    val showLifetimeShimmer = selectedPeriod == TimePeriod.LIFETIME && isLifetimeLoading

    if (filteredData.isEmpty() && !showLifetimeShimmer) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            // No bottom month-label row anymore (month picking lives in the
            // shared MonthSelector above the chart) so the container is a
            // touch shorter than before.
            .height(220.dp)
            .padding(16.dp)
    ) {
        // Time Period Selector - Minimalistic centered design
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    RoundedCornerShape(20.dp)
                )
                .padding(2.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TimePeriod.entries.forEach { period ->
                val isSelected = period == selectedPeriod
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onPeriodSelected(period) }
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                            RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = localizedPeriod(period),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 11.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
        
        // Chart Canvas (or shimmer while Lifetime is loading, or "not enough data" message).
        // Without a minimum of 2 months that actually have transactions, the trend lines
        // get drawn through y=0 across the entire chart — which crosses the spike from
        // the single populated month and produces a confusing diamond/X shape.
        val monthsWithData = filteredData.count { it.transactionCount > 0 }
        val notEnoughData = monthsWithData < 2 && !showLifetimeShimmer

        if (showLifetimeShimmer) {
            LifetimeShimmerChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp)
            )
        } else if (notEnoughData) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.trend_not_enough_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        } else {
            // Resolve theme-aware colors here so the DrawScope helpers below
            // don't need MaterialTheme access. Zero line was hardcoded to
            // semi-transparent black, which is invisible in dark mode.
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            val zeroLineColor = MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (isDark) 0.75f else 0.55f
            )
            val markerColor = MaterialTheme.colorScheme.primary
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp)
            ) {
                drawTrendChart(
                    data = filteredData,
                    width = size.width,
                    height = size.height,
                    zeroLineColor = zeroLineColor,
                    selectedMonth = selectedMonth,
                    markerColor = markerColor
                )
            }
        }
    }
}

@Composable
private fun LifetimeShimmerChart(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "lifetimeShimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lifetimeShimmerAlpha"
    )
    val barColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f * alpha * 2f)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // 16 placeholder bars approximating a multi-year chart
        val heights = listOf(40, 70, 55, 85, 65, 110, 95, 75, 130, 100, 60, 90, 120, 80, 50, 105)
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(h.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}

private fun DrawScope.drawTrendChart(
    data: List<MonthlyMetricSnapshot>,
    width: Float,
    height: Float,
    zeroLineColor: Color,
    selectedMonth: MonthKey,
    markerColor: Color
) {
    if (data.size < 2) return

    val padding = 40f
    val chartWidth = width - (padding * 2)
    val chartHeight = height - (padding * 2)

    // Calculate data ranges
    val allValues = data.flatMap { listOf(it.revenue, it.taxes, it.operatingCosts, it.netIncome) }
    val maxValue = allValues.maxOrNull() ?: 0.0
    val minValue = min(0.0, allValues.minOrNull() ?: 0.0)
    val valueRange = maxValue - minValue

    if (valueRange == 0.0) return

    // Colors for each metric
    val colors = listOf(
        Color(0xFF4CAF50), // Revenue - Green
        Color(0xFFFF9800), // Taxes - Orange
        Color(0xFFF44336), // Operating Costs - Red
        Color(0xFF2196F3)  // Net Income - Blue
    )

    // Draw grid lines
    drawGridLines(padding, chartWidth, chartHeight, maxValue, minValue, zeroLineColor)

    // Vertical accent line marking the currently-selected month. Drawn before
    // the trend lines so it sits behind the data points / paths and reads as
    // a quiet guide rather than competing with the metrics.
    val selectedIndex = data.indexOfFirst { it.month == selectedMonth }
    if (selectedIndex >= 0) {
        val x = padding + (chartWidth * selectedIndex / (data.size - 1))
        drawLine(
            color = markerColor.copy(alpha = 0.55f),
            start = Offset(x, padding),
            end = Offset(x, padding + chartHeight),
            strokeWidth = 1.5.dp.toPx()
        )
    }

    // Draw trend lines for each metric
    drawMetricLine(data, { it.revenue }, colors[0], padding, chartWidth, chartHeight, maxValue, minValue)
    drawMetricLine(data, { it.taxes }, colors[1], padding, chartWidth, chartHeight, maxValue, minValue)
    drawMetricLine(data, { it.operatingCosts }, colors[2], padding, chartWidth, chartHeight, maxValue, minValue)
    drawMetricLine(data, { it.netIncome }, colors[3], padding, chartWidth, chartHeight, maxValue, minValue)
}

private fun DrawScope.drawGridLines(
    padding: Float,
    chartWidth: Float,
    chartHeight: Float,
    maxValue: Double,
    minValue: Double,
    zeroLineColor: Color
) {
    val gridColor = Color.Gray.copy(alpha = 0.2f)

    // Horizontal grid lines
    for (i in 0..4) {
        val y = padding + (chartHeight * i / 4)
        drawLine(
            color = gridColor,
            start = Offset(padding, y),
            end = Offset(padding + chartWidth, y),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Vertical grid lines
    for (i in 0..5) {
        val x = padding + (chartWidth * i / 5)
        drawLine(
            color = gridColor,
            start = Offset(x, padding),
            end = Offset(x, padding + chartHeight),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Bold zero line — theme-aware color so it stays visible against both
    // the light surface and the dark surface. Hardcoded black was invisible
    // in dark mode against the dark background.
    if (minValue <= 0.0 && maxValue >= 0.0) {
        val zeroY = padding + chartHeight - (chartHeight * ((0.0 - minValue) / (maxValue - minValue))).toFloat()
        drawLine(
            color = zeroLineColor,
            start = Offset(padding, zeroY),
            end = Offset(padding + chartWidth, zeroY),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

private fun DrawScope.drawMetricLine(
    data: List<MonthlyMetricSnapshot>,
    valueExtractor: (MonthlyMetricSnapshot) -> Double,
    color: Color,
    padding: Float,
    chartWidth: Float,
    chartHeight: Float,
    maxValue: Double,
    minValue: Double
) {
    val path = Path()
    val points = mutableListOf<Offset>()
    // Break the line over months with no transactions — otherwise sparse data
    // (e.g., one April expense and zeros around it) draws a zigzag through the
    // y=0 midpoint that looks like a fake diamond.
    var pendingMove = true

    data.forEachIndexed { index, snapshot ->
        if (snapshot.transactionCount == 0) {
            pendingMove = true
            return@forEachIndexed
        }
        val value = valueExtractor(snapshot)
        val x = padding + (chartWidth * index / (data.size - 1))
        val y = padding + chartHeight - (chartHeight * ((value - minValue) / (maxValue - minValue))).toFloat()

        points.add(Offset(x, y))

        if (pendingMove) {
            path.moveTo(x, y)
            pendingMove = false
        } else {
            path.lineTo(x, y)
        }
    }
    
    // Draw the line
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 3.dp.toPx())
    )
    
    // Draw data points
    points.forEach { point ->
        drawCircle(
            color = color,
            radius = 4.dp.toPx(),
            center = point
        )
    }
}

@Composable
private fun localizedPeriod(period: TimePeriod): String = when (period) {
    TimePeriod.SIX_MONTHS -> stringResource(Res.string.period_6mo)
    TimePeriod.ONE_YEAR -> stringResource(Res.string.period_1y)
    TimePeriod.LIFETIME -> stringResource(Res.string.period_lifetime)
}

