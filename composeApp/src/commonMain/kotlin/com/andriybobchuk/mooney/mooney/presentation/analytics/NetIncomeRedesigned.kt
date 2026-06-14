package com.andriybobchuk.mooney.mooney.presentation.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.app.appColors
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.MonthlyMetricSnapshot
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.actual_label
import mooney.composeapp.generated.resources.est_income_next_12
import mooney.composeapp.generated.resources.est_savings_next_12
import mooney.composeapp.generated.resources.forecast_disclaimer
import mooney.composeapp.generated.resources.if_you_keep_pace
import mooney.composeapp.generated.resources.last_12_months
import mooney.composeapp.generated.resources.last_6_months_label
import mooney.composeapp.generated.resources.months_forward
import mooney.composeapp.generated.resources.net_worth_trajectory
import mooney.composeapp.generated.resources.nw_in_12_mo
import mooney.composeapp.generated.resources.nw_in_6_mo
import mooney.composeapp.generated.resources.of_every_dollar_earned
import mooney.composeapp.generated.resources.projected_label
import mooney.composeapp.generated.resources.saved_dot_pct
import mooney.composeapp.generated.resources.spent_dot_pct
import mooney.composeapp.generated.resources.today_marker
import org.jetbrains.compose.resources.stringResource

/**
 * Redesigned Net Income screen — designed to answer "am I saving enough and
 * where am I headed" in one glance:
 *
 *  - Period toggle (12mo / 6mo) re-bases every metric on the screen.
 *  - Saved hero with savings rate and per-month average.
 *  - Net worth trajectory: actual line up to today + dashed projection 12mo
 *    forward derived from the period's average savings.
 *  - "Of every dollar earned" progress bar for the savings rate gut-check.
 *  - "If you keep this pace" forecast cards.
 *  - Forecast disclaimer.
 *
 * Everything is computed live from [historicalData]; no DB access here.
 */
@Composable
fun NetIncomeRedesigned(
    historicalData: List<MonthlyMetricSnapshot>,
    currentNetWorth: Double,
    modifier: Modifier = Modifier
) {
    var months by rememberSaveable { mutableStateOf(12) }
    val window = remember(historicalData, months) {
        historicalData.takeLast(months)
    }
    val savings = window.sumOf { it.netIncome }
    val gross = window.sumOf { it.revenue }
    val expenses = window.sumOf { it.operatingCosts + it.taxes }
    val savedPct = if (gross > 0) (savings / gross) * 100 else 0.0
    val avgMonthlySavings = if (window.isNotEmpty()) savings / window.size else 0.0
    val nextYearIncome = if (window.isNotEmpty()) (gross / window.size) * 12 else 0.0
    val nextYearSavings = avgMonthlySavings * 12
    val sixMonthNW = currentNetWorth + avgMonthlySavings * 6
    val twelveMonthNW = currentNetWorth + avgMonthlySavings * 12

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        PeriodToggle(selected = months, onSelect = { months = it })
        Spacer(modifier = Modifier.height(16.dp))
        SavedHero(savings = savings, savedPct = savedPct, avgMonthly = avgMonthlySavings, months = months)
        Spacer(modifier = Modifier.height(16.dp))
        TrajectoryCard(
            window = window,
            currentNetWorth = currentNetWorth,
            avgMonthlySavings = avgMonthlySavings
        )
        Spacer(modifier = Modifier.height(16.dp))
        EveryDollarCard(gross = gross, savings = savings, expenses = expenses, savedPct = savedPct)
        Spacer(modifier = Modifier.height(16.dp))
        PaceHeader()
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ForecastCard(modifier = Modifier.weight(1f), label = stringResource(Res.string.est_income_next_12), value = nextYearIncome)
            ForecastCard(
                modifier = Modifier.weight(1f),
                label = stringResource(Res.string.est_savings_next_12),
                value = nextYearSavings,
                colorByValue = true,
                showSign = true
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ForecastCard(modifier = Modifier.weight(1f), label = stringResource(Res.string.nw_in_6_mo), value = sixMonthNW, accent = MaterialTheme.colorScheme.primary)
            ForecastCard(modifier = Modifier.weight(1f), label = stringResource(Res.string.nw_in_12_mo), value = twelveMonthNW, accent = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(Res.string.forecast_disclaimer, months, "${avgMonthlySavings.toInt()} ${GlobalConfig.baseCurrency.symbol}"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PeriodToggle(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            12 to stringResource(Res.string.last_12_months),
            6 to stringResource(Res.string.last_6_months_label)
        ).forEach { (value, label) ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SavedHero(savings: Double, savedPct: Double, avgMonthly: Double, months: Int) {
    val isPositive = savings >= 0
    val accent = if (isPositive) MaterialTheme.appColors.incomeColor else MaterialTheme.appColors.expenseColor
    com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyCard(
        modifier = Modifier.fillMaxWidth(),
        variant = com.andriybobchuk.mooney.core.presentation.designsystem.components.CardVariant.FILLED
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "SAVED · LAST $months MONTHS",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                val prefix = if (isPositive) "+" else ""
                Text(
                    text = "$prefix${savings.formatWithCommas()}",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                    color = accent
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = GlobalConfig.baseCurrency.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(accent.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    val pct = (savedPct * 10).toInt() / 10.0
                    Text(
                        text = "↑ $pct % of gross income",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = accent
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "≈ ${avgMonthly.toInt()} ${GlobalConfig.baseCurrency.symbol}/mo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrajectoryCard(
    window: List<MonthlyMetricSnapshot>,
    currentNetWorth: Double,
    avgMonthlySavings: Double
) {
    com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyCard(
        modifier = Modifier.fillMaxWidth(),
        variant = com.andriybobchuk.mooney.core.presentation.designsystem.components.CardVariant.FILLED
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.net_worth_trajectory),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val twelveMonthProjection = currentNetWorth + avgMonthlySavings * 12
            Spacer(modifier = Modifier.height(2.dp))
            Row {
                Text(
                    text = "${currentNetWorth.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol} today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "→",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "${twelveMonthProjection.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol} in 12 mo",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            TrajectoryChart(
                window = window,
                currentNetWorth = currentNetWorth,
                avgMonthlySavings = avgMonthlySavings
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = window.firstOrNull()?.month?.toShortDisplayString() ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(Res.string.today_marker),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(Res.string.months_forward, 12),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp, 3.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary))
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(Res.string.actual_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.size(16.dp))
                Box(modifier = Modifier.size(12.dp, 3.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)))
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(Res.string.projected_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TrajectoryChart(
    window: List<MonthlyMetricSnapshot>,
    currentNetWorth: Double,
    avgMonthlySavings: Double
) {
    if (window.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp))
        return
    }
    // Build series: walk backwards from current net worth, subtracting each
    // month's net income to reconstruct historical balance. Then add 12 future
    // months projected at avgMonthlySavings.
    val sortedAsc = window.sortedBy { it.month.year * 100 + it.month.month }
    val past = mutableListOf<Double>()
    var running = currentNetWorth
    for (snap in sortedAsc.reversed()) {
        running -= snap.netIncome
        past += running
    }
    past.reverse()
    past += currentNetWorth
    val future = (1..12).map { currentNetWorth + avgMonthlySavings * it }
    val all = past + future
    val accent = MaterialTheme.colorScheme.primary
    val isDark = isSystemInDarkTheme()
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.15f else 0.10f)
    val todayLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val padding = 16f
        val w = size.width - padding * 2
        val h = size.height - padding * 2
        val maxV = all.max()
        val minV = all.min()
        val range = (maxV - minV).takeIf { it > 0 } ?: 1.0
        for (i in 0..3) {
            val y = padding + h * i / 3
            drawLine(gridColor, Offset(padding, y), Offset(padding + w, y), 1.dp.toPx())
        }
        fun pointAt(i: Int): Offset {
            val x = padding + w * i / (all.size - 1)
            val y = padding + h - (h * ((all[i] - minV) / range)).toFloat()
            return Offset(x, y)
        }
        val historyCount = past.size
        // Vertical today line.
        val todayPoint = pointAt(historyCount - 1)
        drawLine(
            color = todayLineColor,
            start = Offset(todayPoint.x, padding),
            end = Offset(todayPoint.x, padding + h),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )
        // Actual area shading under the line.
        val fillPath = Path().apply {
            moveTo(padding, padding + h)
            for (i in 0 until historyCount) {
                val p = pointAt(i)
                lineTo(p.x, p.y)
            }
            lineTo(pointAt(historyCount - 1).x, padding + h)
            close()
        }
        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(accent.copy(alpha = 0.20f), accent.copy(alpha = 0.0f))
            )
        )
        // Actual line.
        val actualPath = Path()
        for (i in 0 until historyCount) {
            val p = pointAt(i)
            if (i == 0) actualPath.moveTo(p.x, p.y) else actualPath.lineTo(p.x, p.y)
        }
        drawPath(actualPath, accent, style = Stroke(width = 3.dp.toPx()))
        // Today circle.
        drawCircle(accent, radius = 5.dp.toPx(), center = todayPoint)
        // Projection dashed.
        val projPath = Path()
        projPath.moveTo(todayPoint.x, todayPoint.y)
        for (i in historyCount until all.size) {
            val p = pointAt(i)
            projPath.lineTo(p.x, p.y)
        }
        drawPath(
            projPath,
            accent.copy(alpha = 0.55f),
            style = Stroke(width = 2.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        )
        // 12mo label pill.
        val endPoint = pointAt(all.size - 1)
        val labelText = "${all.last().formatToShortStringSafe()} ${GlobalConfig.baseCurrency.symbol}"
        // We render the label outside Canvas — Canvas drawText is fiddly. Skip for now.
        drawCircle(accent.copy(alpha = 0.6f), radius = 4.dp.toPx(), center = endPoint)
    }
}

private fun Double.formatToShortStringSafe(): String {
    val absVal = kotlin.math.abs(this)
    return when {
        absVal >= 1_000_000 -> "${(this / 1_000_000.0 * 10).toInt() / 10.0}M"
        absVal >= 1_000 -> "${(this / 1_000.0 * 10).toInt() / 10.0}k"
        else -> this.toInt().toString()
    }
}

@Composable
private fun EveryDollarCard(gross: Double, savings: Double, expenses: Double, savedPct: Double) {
    com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyCard(
        modifier = Modifier.fillMaxWidth(),
        variant = com.andriybobchuk.mooney.core.presentation.designsystem.components.CardVariant.FILLED
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(Res.string.of_every_dollar_earned),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${gross.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol} gross",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            val pct = if (gross > 0) (savings / gross).coerceIn(0.0, 1.0) else 0.0
            val income = MaterialTheme.appColors.incomeColor
            val expense = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(expense)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pct.toFloat())
                        .height(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(income)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                LegendCol(
                    modifier = Modifier.weight(1f),
                    dotColor = income,
                    label = stringResource(Res.string.saved_dot_pct, "${(savedPct * 10).toInt() / 10.0}"),
                    value = savings
                )
                LegendCol(
                    modifier = Modifier.weight(1f),
                    dotColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    label = stringResource(Res.string.spent_dot_pct, "${((1 - (savings / gross.coerceAtLeast(1.0))) * 100).toInt()}"),
                    value = expenses
                )
            }
        }
    }
}

@Composable
private fun LegendCol(modifier: Modifier, dotColor: Color, label: String, value: Double) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${value.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PaceHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(14.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(Res.string.if_you_keep_pace),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ForecastCard(
    modifier: Modifier,
    label: String,
    value: Double,
    colorByValue: Boolean = false,
    showSign: Boolean = false,
    accent: Color? = null
) {
    val color = when {
        accent != null -> accent
        colorByValue -> if (value >= 0) MaterialTheme.appColors.incomeColor else MaterialTheme.appColors.expenseColor
        else -> MaterialTheme.colorScheme.onSurface
    }
    val prefix = if (showSign && value >= 0) "+" else ""
    com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyCard(
        modifier = modifier,
        variant = com.andriybobchuk.mooney.core.presentation.designsystem.components.CardVariant.FILLED
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$prefix${value.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
