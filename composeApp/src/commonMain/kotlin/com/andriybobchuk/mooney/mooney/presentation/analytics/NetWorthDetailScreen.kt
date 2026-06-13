package com.andriybobchuk.mooney.mooney.presentation.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andriybobchuk.mooney.app.appColors
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.MonthlyMetricSnapshot
import com.andriybobchuk.mooney.mooney.domain.formatToShortString
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.actual_label
import mooney.composeapp.generated.resources.avg_per_month
import mooney.composeapp.generated.resources.highest_label
import mooney.composeapp.generated.resources.in_12_mo
import mooney.composeapp.generated.resources.in_6_mo
import mooney.composeapp.generated.resources.lowest_label
import mooney.composeapp.generated.resources.months_forward
import mooney.composeapp.generated.resources.net_worth_disclaimer
import mooney.composeapp.generated.resources.net_worth_title
import mooney.composeapp.generated.resources.net_worth_trajectory
import mooney.composeapp.generated.resources.projected_label
import mooney.composeapp.generated.resources.today_marker
import org.jetbrains.compose.resources.stringResource

/**
 * "Where is my net worth heading" screen. Reached by tapping the Net Worth
 * card on the Analytics tab. Follows the same design language as the
 * redesigned Net Income screen — hero figure, history chart with projection,
 * supporting stats — but the axis is net worth instead of cumulative savings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthDetailScreen(
    viewModel: AnalyticsViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    // Reconstruct a "net worth over time" series by walking the historical
    // monthly net income backwards from today's known net worth. Each step
    // back subtracts the net income recorded for that month. Approximate
    // (assumes balance changes equal monthly net income, ignoring large
    // one-off events like a sold car) but accurate enough for the trend.
    val history = remember(state.historicalMetrics, state.currentNetWorth) {
        buildNetWorthHistory(
            now = state.currentNetWorth,
            monthly = state.historicalMetrics
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(Color.Transparent),
        topBar = {
            Toolbars.Primary(
                title = stringResource(Res.string.net_worth_title),
                showBackButton = true,
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            com.andriybobchuk.mooney.core.ads.AdBannerSlot(
                placement = com.andriybobchuk.mooney.core.ads.AdPlacement.ANALYTICS_BREAKDOWN_BANNER
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            NetWorthHero(amount = state.currentNetWorth, history = history)
            Spacer(modifier = Modifier.height(20.dp))
            NetWorthTrajectoryCard(history = history)
            Spacer(modifier = Modifier.height(20.dp))
            NetWorthStatsGrid(history = history)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(Res.string.net_worth_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class NetWorthPoint(val month: String, val amount: Double)

private fun buildNetWorthHistory(
    now: Double,
    monthly: List<MonthlyMetricSnapshot>
): List<NetWorthPoint> {
    if (monthly.isEmpty()) return listOf(NetWorthPoint("Now", now))
    val sortedDesc = monthly.sortedByDescending { it.month.year * 100 + it.month.month }
    val out = mutableListOf<NetWorthPoint>()
    var running = now
    out += NetWorthPoint("Now", running)
    for (snapshot in sortedDesc) {
        running -= snapshot.netIncome
        out += NetWorthPoint(snapshot.month.toShortDisplayString(), running)
    }
    return out.reversed()
}

@Composable
private fun NetWorthHero(amount: Double, history: List<NetWorthPoint>) {
    val first = history.firstOrNull()?.amount ?: amount
    val delta = amount - first
    val pct = if (first != 0.0) (delta / first) * 100 else 0.0
    val accent = if (delta >= 0) MaterialTheme.appColors.incomeColor else MaterialTheme.appColors.expenseColor
    Column {
        Text(
            text = "CURRENT NET WORTH",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = amount.formatWithCommas(),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = GlobalConfig.baseCurrency.symbol,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        if (history.size > 1) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                val sign = if (delta >= 0) "+" else ""
                Text(
                    text = "$sign${delta.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol} · " +
                        "$sign${(pct * 10).toInt() / 10.0}% over period",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = accent
                )
            }
        }
    }
}

@Composable
private fun NetWorthTrajectoryCard(history: List<NetWorthPoint>) {
    com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyCard(
        modifier = Modifier.fillMaxWidth(),
        variant = com.andriybobchuk.mooney.core.presentation.designsystem.components.CardVariant.OUTLINED
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.net_worth_trajectory),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (history.size < 2) {
                Text(
                    text = "Once you have a couple of months of activity the trajectory shows up here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@MooneyCard
            }
            // Projection: same monthly trend over the next 12 months. Picks
            // the linear slope from the last few real points to keep the
            // future line connected, not flatlined.
            val avgMonthlyDelta = if (history.size >= 2) {
                (history.last().amount - history.first().amount) / (history.size - 1)
            } else 0.0
            val projected = (1..12).map { idx ->
                NetWorthPoint(
                    month = "+$idx",
                    amount = history.last().amount + (avgMonthlyDelta * idx)
                )
            }
            val all = history + projected
            val accent = MaterialTheme.colorScheme.primary
            val isDark = isSystemInDarkTheme()
            val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.15f else 0.10f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(top = 8.dp)
            ) {
                val padding = 16f
                val w = size.width - padding * 2
                val h = size.height - padding * 2
                val values = all.map { it.amount }
                val maxV = values.max()
                val minV = values.min()
                val range = (maxV - minV).takeIf { it > 0 } ?: 1.0
                // Subtle horizontal grid.
                for (i in 0..3) {
                    val y = padding + h * i / 3
                    drawLine(gridColor, Offset(padding, y), Offset(padding + w, y), 1.dp.toPx())
                }
                val historyCount = history.size
                fun pointAt(i: Int): Offset {
                    val x = padding + w * i / (all.size - 1)
                    val y = padding + h - (h * ((all[i].amount - minV) / range)).toFloat()
                    return Offset(x, y)
                }
                // Actual line.
                val actualPath = Path()
                for (i in 0 until historyCount) {
                    val p = pointAt(i)
                    if (i == 0) actualPath.moveTo(p.x, p.y) else actualPath.lineTo(p.x, p.y)
                }
                drawPath(actualPath, accent, style = Stroke(width = 3.dp.toPx()))
                // Today marker.
                drawCircle(accent, radius = 5.dp.toPx(), center = pointAt(historyCount - 1))
                // Projection line — same slope, dashed.
                val projPath = Path()
                projPath.moveTo(pointAt(historyCount - 1).x, pointAt(historyCount - 1).y)
                for (i in historyCount until all.size) {
                    val p = pointAt(i)
                    projPath.lineTo(p.x, p.y)
                }
                drawPath(
                    projPath,
                    accent.copy(alpha = 0.55f),
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(history.first().month, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(Res.string.today_marker), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(Res.string.months_forward, 12), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(Res.string.actual_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.size(16.dp))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)))
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(Res.string.projected_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NetWorthStatsGrid(history: List<NetWorthPoint>) {
    if (history.size < 2) return
    val avg = history.sumOf { it.amount } / history.size
    val maxPoint = history.maxByOrNull { it.amount }
    val minPoint = history.minByOrNull { it.amount }
    val avgMonthlyDelta = (history.last().amount - history.first().amount) / (history.size - 1)
    val sixMonthProjection = history.last().amount + avgMonthlyDelta * 6
    val twelveMonthProjection = history.last().amount + avgMonthlyDelta * 12
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = stringResource(Res.string.highest_label),
                value = maxPoint?.amount ?: 0.0,
                sublabel = maxPoint?.month
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = stringResource(Res.string.lowest_label),
                value = minPoint?.amount ?: 0.0,
                sublabel = minPoint?.month
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = stringResource(Res.string.avg_per_month),
                value = avg
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Δ / month",
                value = avgMonthlyDelta,
                colorByValue = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = stringResource(Res.string.in_6_mo),
                value = sixMonthProjection,
                accent = MaterialTheme.colorScheme.primary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = stringResource(Res.string.in_12_mo),
                value = twelveMonthProjection,
                accent = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    label: String,
    value: Double,
    sublabel: String? = null,
    colorByValue: Boolean = false,
    accent: Color? = null
) {
    val color = when {
        accent != null -> accent
        colorByValue -> if (value >= 0) MaterialTheme.appColors.incomeColor else MaterialTheme.appColors.expenseColor
        else -> MaterialTheme.colorScheme.onSurface
    }
    com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyCard(
        modifier = modifier,
        variant = com.andriybobchuk.mooney.core.presentation.designsystem.components.CardVariant.OUTLINED
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${value.formatWithCommas()} ${GlobalConfig.baseCurrency.symbol}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
