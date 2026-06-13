package com.andriybobchuk.mooney.core.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.HistoricalRate
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import kotlinx.datetime.*
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.not_enough_data
import mooney.composeapp.generated.resources.rate_above_avg
import mooney.composeapp.generated.resources.rate_below_avg
import mooney.composeapp.generated.resources.rate_high
import mooney.composeapp.generated.resources.rate_low
import org.jetbrains.compose.resources.stringResource

enum class ChartTimeRange(val months: Int, val label: String) {
    ONE_MONTH(1, "1M"),
    THREE_MONTHS(3, "3M"),
    SIX_MONTHS(6, "6M")
}

@Composable
fun CurrencyRateChart(
    chartCurrency: Currency,
    baseCurrency: Currency,
    currentRate: Double?,
    historicalRates: List<HistoricalRate>,
    percentile: Int? = null,
    assetAmount: Double? = null,
    initialTimeRange: ChartTimeRange = ChartTimeRange.SIX_MONTHS,
    modifier: Modifier = Modifier
) {
    var timeRange by remember { mutableStateOf(initialTimeRange) }
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val cutoffDate = today.minus(timeRange.months, DateTimeUnit.MONTH)
    val chartRates = historicalRates.filter { it.date >= cutoffDate }

    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${chartCurrency.name}/${baseCurrency.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (currentRate != null) {
                    Text(
                        text = "1 ${chartCurrency.symbol} = ${currentRate.formatWithCommas()} ${baseCurrency.symbol}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ChartTimeRange.entries.forEach { range ->
                    val isSelected = range == timeRange
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable { timeRange = range }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = range.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (chartRates.size < 2) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(Res.string.not_enough_data), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            return
        }

        val minRate = chartRates.minOf { it.rate }
        val maxRate = chartRates.maxOf { it.rate }
        val avgRate = chartRates.map { it.rate }.average()
        val range = maxRate - minRate
        val padding = if (range == 0.0) maxRate * 0.02 else range * 0.15
        val yMin = minRate - padding
        val yMax = maxRate + padding
        val yRange = yMax - yMin

        val maWindow = 20
        val movingAvg = chartRates.mapIndexed { i, _ ->
            val start = (i - maWindow + 1).coerceAtLeast(0)
            chartRates.subList(start, i + 1).map { it.rate }.average()
        }

        fun formatY(v: Double): String {
            val rounded = kotlin.math.round(v * 1000) / 1000
            return rounded.toString()
        }

        val gridLines = 6
        val gridValues = (0..gridLines).map { i -> yMin + (yRange * i / gridLines) }

        val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        val lineColor = MaterialTheme.colorScheme.primary
        val currentLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        val highLineColor = Color(0xFF16A34A).copy(alpha = 0.4f)
        val lowLineColor = Color(0xFFDC2626).copy(alpha = 0.4f)
        val avgLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        val maColor = Color(0xFFFF9800).copy(alpha = 0.5f)

        // Chart
        Row(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            Column(
                modifier = Modifier.width(44.dp).fillMaxHeight().padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                gridValues.reversed().forEach { value ->
                    Text(formatY(value), style = MaterialTheme.typography.labelSmall, color = labelColor, fontSize = 8.sp, maxLines = 1)
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    gridValues.forEach { value ->
                        val y = h - ((value - yMin) / yRange * h).toFloat()
                        drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(w, y), 1f)
                    }

                    val highY = h - ((maxRate - yMin) / yRange * h).toFloat()
                    drawLine(highLineColor, androidx.compose.ui.geometry.Offset(0f, highY), androidx.compose.ui.geometry.Offset(w, highY), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))

                    val lowY = h - ((minRate - yMin) / yRange * h).toFloat()
                    drawLine(lowLineColor, androidx.compose.ui.geometry.Offset(0f, lowY), androidx.compose.ui.geometry.Offset(w, lowY), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))

                    val avgY = h - ((avgRate - yMin) / yRange * h).toFloat()
                    drawLine(avgLineColor, androidx.compose.ui.geometry.Offset(0f, avgY), androidx.compose.ui.geometry.Offset(w, avgY), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))

                    if (currentRate != null) {
                        val currentY = h - ((currentRate - yMin) / yRange * h).toFloat()
                        drawLine(currentLineColor, androidx.compose.ui.geometry.Offset(0f, currentY), androidx.compose.ui.geometry.Offset(w, currentY), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                    }

                    if (movingAvg.size >= 2) {
                        val maPath = Path()
                        movingAvg.forEachIndexed { i, ma ->
                            val x = (i.toFloat() / (movingAvg.size - 1)) * w
                            val y = h - ((ma - yMin) / yRange * h).toFloat()
                            if (i == 0) maPath.moveTo(x, y) else maPath.lineTo(x, y)
                        }
                        drawPath(maPath, maColor, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
                    }

                    val path = Path()
                    chartRates.forEachIndexed { i, hr ->
                        val x = (i.toFloat() / (chartRates.size - 1)) * w
                        val y = h - ((hr.rate - yMin) / yRange * h).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // X-axis dates
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labelCount = if (timeRange == ChartTimeRange.ONE_MONTH) 5 else 7
            val step = (chartRates.size - 1).coerceAtLeast(1).toFloat() / (labelCount - 1).coerceAtLeast(1)
            for (i in 0 until labelCount) {
                val idx = (i * step).toInt().coerceAtMost(chartRates.size - 1)
                val date = chartRates[idx].date
                Text("${date.dayOfMonth} ${shortMonth(date.monthNumber)}", style = MaterialTheme.typography.labelSmall, color = labelColor, fontSize = 8.sp)
            }
        }

        Spacer(Modifier.height(10.dp))

        // MA20 legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(14.dp, 2.dp)) {
                drawLine(maColor, androidx.compose.ui.geometry.Offset(0f, center.y), androidx.compose.ui.geometry.Offset(size.width, center.y), 2f)
            }
            Spacer(Modifier.width(4.dp))
            Text("20-day moving average", style = MaterialTheme.typography.labelSmall, color = maColor, fontSize = 9.sp)
        }

        Spacer(Modifier.height(8.dp))

        // Stats cards — scrollable row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard("Low", formatY(minRate), lowLineColor, true, assetAmount, minRate, currentRate, baseCurrency)
            StatCard("Avg", formatY(avgRate), avgLineColor, true, assetAmount, avgRate, currentRate, baseCurrency)
            StatCard("High", formatY(maxRate), highLineColor, true, assetAmount, maxRate, currentRate, baseCurrency)
            if (currentRate != null) {
                StatCard("Now", formatY(currentRate), currentLineColor, true, assetAmount, currentRate, null, baseCurrency)
            }
        }

        // Percentile row
        if (percentile != null) {
            Spacer(Modifier.height(12.dp))
            PercentileRow(percentile = percentile)
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    rateValue: String,
    color: Color,
    dashed: Boolean,
    assetAmount: Double?,
    rate: Double,
    currentRate: Double?,
    baseCurrency: Currency
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Legend line + label
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Canvas(modifier = Modifier.size(12.dp, 2.dp)) {
                    drawLine(color, androidx.compose.ui.geometry.Offset(0f, center.y), androidx.compose.ui.geometry.Offset(size.width, center.y), 2f, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4f, 3f)) else null)
                }
                Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(6.dp))

            // Rate
            Text(
                text = rateValue,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            // Converted amount
            if (assetAmount != null) {
                Spacer(Modifier.height(2.dp))
                val converted = assetAmount * rate
                Text(
                    text = "${converted.formatWithCommas()} ${baseCurrency.symbol}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Diff from current
                if (currentRate != null && currentRate != rate) {
                    val currentConverted = assetAmount * currentRate
                    val diff = converted - currentConverted
                    val sign = if (diff >= 0) "+" else ""
                    val diffColor = if (diff >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                    Text(
                        text = "$sign${diff.formatWithCommas()} ${baseCurrency.symbol}",
                        style = MaterialTheme.typography.labelMedium,
                        color = diffColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun PercentileRow(percentile: Int) {
    val barColor = when {
        percentile >= 67 -> Color(0xFF16A34A)
        percentile <= 33 -> Color(0xFFDC2626)
        else -> Color(0xFFFF9800)
    }
    val label = when {
        percentile >= 75 -> stringResource(Res.string.rate_high)
        percentile >= 50 -> stringResource(Res.string.rate_above_avg)
        percentile >= 25 -> stringResource(Res.string.rate_below_avg)
        else -> stringResource(Res.string.rate_low)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(barColor.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bar
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentile / 100f)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "${percentile}th percentile",
            style = MaterialTheme.typography.labelMedium,
            color = barColor,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
fun PercentileBar(percentile: Int) {
    val barColor = when {
        percentile >= 67 -> Color(0xFF16A34A)
        percentile <= 33 -> Color(0xFFDC2626)
        else -> Color(0xFFFF9800)
    }
    Column(horizontalAlignment = Alignment.End) {
        Box(modifier = Modifier.width(60.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(percentile / 100f).clip(RoundedCornerShape(2.dp)).background(barColor))
        }
    }
}

@Composable
fun ChartLegendItem(label: String, color: Color, dashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(16.dp, 2.dp)) {
            drawLine(color, androidx.compose.ui.geometry.Offset(0f, center.y), androidx.compose.ui.geometry.Offset(size.width, center.y), 2f, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4f, 3f)) else null)
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 9.sp)
    }
}

@Composable
fun StatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        Text(value, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

fun shortMonth(month: Int): String = when (month) {
    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
    7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
    else -> ""
}
