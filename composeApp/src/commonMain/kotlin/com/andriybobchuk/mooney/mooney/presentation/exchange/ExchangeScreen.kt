package com.andriybobchuk.mooney.mooney.presentation.exchange

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import com.andriybobchuk.mooney.app.appColors
import com.andriybobchuk.mooney.core.presentation.Toolbars
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import androidx.compose.ui.graphics.PathEffect
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.HistoricalRate
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import kotlinx.datetime.*
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.alpha
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.exchange_rates
import mooney.composeapp.generated.resources.historical_rates
import mooney.composeapp.generated.resources.rate_above_avg_short
import mooney.composeapp.generated.resources.rate_below_avg_short
import mooney.composeapp.generated.resources.rate_high_short
import mooney.composeapp.generated.resources.rate_low_short
import mooney.composeapp.generated.resources.base_currency_prefix
import mooney.composeapp.generated.resources.current_rate_prefix
import mooney.composeapp.generated.resources.tap_currency_for_chart
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeScreen(
    viewModel: ExchangeViewModel = koinViewModel(),
    bottomNavbar: @Composable () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        topBar = {
            Toolbars.Primary(
                titleContent = {
                    Column(
                        modifier = Modifier.clickable { 
                            viewModel.onAction(ExchangeAction.CycleBaseCurrency) 
                        }
                    ) {
                        Text(
                            text = stringResource(Res.string.exchange_rates),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        )
                        Text(
                            text = stringResource(Res.string.base_currency_prefix, "${state.displayBaseCurrency.symbol} ${state.displayBaseCurrency.name}"),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                actions = listOf(
                    Toolbars.ToolBarAction(
                        painter = com.andriybobchuk.mooney.core.presentation.Icons.RefreshIcon(),
                        contentDescription = "Refresh Rates",
                        onClick = { viewModel.onAction(ExchangeAction.RefreshRates) }
                    ),
                    Toolbars.ToolBarAction(
                        painter = com.andriybobchuk.mooney.core.presentation.Icons.SettingsIcon(),
                        contentDescription = "Settings",
                        onClick = onSettingsClick
                    )
                )
            )
        },
        bottomBar = bottomNavbar
    ) { paddingValues ->
        ExchangeScreenContent(
            modifier = Modifier.padding(paddingValues),
            state = state,
            onAction = viewModel::onAction
        )
    }

    state.alertSheetCurrency?.let { currency ->
        MooneyBottomSheet(
            onDismissRequest = { viewModel.onAction(ExchangeAction.CloseAlertSheet) }
        ) {
            RateWatchAlertSheet(
                baseCurrency = state.displayBaseCurrency,
                targetCurrency = currency,
                currentRate = state.currentRates[currency] ?: 0.0,
                existingAlerts = state.activeAlerts,
                onSave = { targetRate, direction ->
                    viewModel.onAction(ExchangeAction.SaveAlert(state.displayBaseCurrency, currency, targetRate, direction))
                },
                onDelete = { viewModel.onAction(ExchangeAction.DeleteAlert(it)) }
            )
        }
    }
}

@Composable
private fun ExchangeScreenContent(
    modifier: Modifier = Modifier,
    state: ExchangeState,
    onAction: (ExchangeAction) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
        }

        if (state.error != null) {
            item {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        // Historical Chart
        item {
            HistoricalExchangeRateChart(
                state = state,
                onTimeRangeToggle = { onAction(ExchangeAction.ToggleTimeRange) },
                onTimeRangeSelected = { onAction(ExchangeAction.SelectTimeRange(it)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Triggered alert banners
        if (state.triggeredAlerts.isNotEmpty()) {
            items(state.triggeredAlerts) { triggered ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${triggered.alert.fromCurrency.name}→${triggered.alert.toCurrency.name} crossed ${triggered.alert.targetRate}!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(Res.string.current_rate_prefix, "${triggered.currentRate.formatWithCommas()} ${triggered.alert.toCurrency.symbol}"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = "✕",
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAction(ExchangeAction.DismissTriggeredAlert(triggered.alert.id)) }
                                .padding(8.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Currency Rate Cards — filtered to user's currencies
        val visibleCurrencies = Currency.entries.filter {
            it != state.displayBaseCurrency &&
                state.currentRates.containsKey(it) &&
                (state.userCurrencyCodes.isEmpty() || it.name in state.userCurrencyCodes)
        }
        items(visibleCurrencies) { currency ->
            val historicalRates = state.historicalRates[currency] ?: emptyList()
            CurrencyRateCard(
                currency = currency,
                rate = state.currentRates[currency] ?: return@items,
                baseCurrency = state.displayBaseCurrency,
                isSelected = currency == state.selectedCurrency,
                percentile = state.percentiles[currency],
                historicalRates = historicalRates,
                color = getCurrencyColor(currency),
                onClick = {
                    onAction(ExchangeAction.SelectCurrency(
                        if (currency == state.selectedCurrency) null else currency
                    ))
                },
                onLongClick = { onAction(ExchangeAction.OpenAlertSheet(currency)) }
            )
        }
        
        item { 
            Spacer(Modifier.height(80.dp)) // Space for bottom nav
        }
    }
}

@Composable
private fun HistoricalExchangeRateChart(
    state: ExchangeState,
    onTimeRangeToggle: () -> Unit,
    onTimeRangeSelected: (TimeRange) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val cutoffDate = today.minus(state.timeRange.months, DateTimeUnit.MONTH)
    val chartCurrency = state.selectedCurrency ?: state.historicalRates.keys.firstOrNull()
    val chartRates = chartCurrency?.let {
        state.historicalRates[it]?.filter { r -> r.date >= cutoffDate }
    } ?: emptyList()
    val currentRate = chartCurrency?.let { state.currentRates[it] }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.appColors.cardBackground
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (chartCurrency != null) "${chartCurrency.name}/${state.displayBaseCurrency.name}" else stringResource(Res.string.historical_rates),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (currentRate != null && chartCurrency != null) {
                        Text(
                            text = "1 ${chartCurrency.symbol} = ${currentRate.formatWithCommas()} ${state.displayBaseCurrency.symbol}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Time range buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TimeRange.entries.forEach { range ->
                        val isSelected = range == state.timeRange
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .clickable { onTimeRangeSelected(range) }
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

            if (chartRates.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(Res.string.tap_currency_for_chart), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            } else {
                val minRate = chartRates.minOf { it.rate }
                val maxRate = chartRates.maxOf { it.rate }
                val avgRate = chartRates.map { it.rate }.average()
                val range = maxRate - minRate
                val padding = if (range == 0.0) maxRate * 0.02 else range * 0.15
                val yMin = minRate - padding
                val yMax = maxRate + padding
                val yRange = yMax - yMin

                // 20-day simple moving average
                val maWindow = 20
                val movingAvg = chartRates.mapIndexed { i, _ ->
                    val start = (i - maWindow + 1).coerceAtLeast(0)
                    val window = chartRates.subList(start, i + 1)
                    window.map { it.rate }.average()
                }

                // Format Y values with appropriate precision
                fun formatY(v: Double): String {
                    val rounded = kotlin.math.round(v * 1000) / 1000
                    return rounded.toString()
                }

                val gridLines = 6
                val gridValues = (0..gridLines).map { i -> yMin + (yRange * i / gridLines) }

                val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                val lineColor = chartCurrency?.let { getCurrencyColor(it) } ?: MaterialTheme.colorScheme.primary
                val currentLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                val highLineColor = Color(0xFF16A34A).copy(alpha = 0.4f)
                val lowLineColor = Color(0xFFDC2626).copy(alpha = 0.4f)
                val avgLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                val maColor = Color(0xFFFF9800).copy(alpha = 0.5f)

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ChartLegendItem("Now", currentLineColor, dashed = true)
                    ChartLegendItem("High", highLineColor, dashed = true)
                    ChartLegendItem("Low", lowLineColor, dashed = true)
                    ChartLegendItem("MA20", maColor, dashed = false)
                }

                Row(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                    // Y-axis labels
                    Column(
                        modifier = Modifier
                            .width(44.dp)
                            .fillMaxHeight()
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        gridValues.reversed().forEach { value ->
                            Text(
                                text = formatY(value),
                                style = MaterialTheme.typography.labelSmall,
                                color = labelColor,
                                fontSize = 8.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Chart area
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Horizontal grid lines
                            gridValues.forEach { value ->
                                val y = h - ((value - yMin) / yRange * h).toFloat()
                                drawLine(
                                    color = gridColor,
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(w, y),
                                    strokeWidth = 1f
                                )
                            }

                            // High line
                            val highY = h - ((maxRate - yMin) / yRange * h).toFloat()
                            drawLine(
                                color = highLineColor,
                                start = androidx.compose.ui.geometry.Offset(0f, highY),
                                end = androidx.compose.ui.geometry.Offset(w, highY),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                            )

                            // Low line
                            val lowY = h - ((minRate - yMin) / yRange * h).toFloat()
                            drawLine(
                                color = lowLineColor,
                                start = androidx.compose.ui.geometry.Offset(0f, lowY),
                                end = androidx.compose.ui.geometry.Offset(w, lowY),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                            )

                            // Average dashed line
                            val avgY = h - ((avgRate - yMin) / yRange * h).toFloat()
                            drawLine(
                                color = avgLineColor,
                                start = androidx.compose.ui.geometry.Offset(0f, avgY),
                                end = androidx.compose.ui.geometry.Offset(w, avgY),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                            )

                            // Current rate line
                            if (currentRate != null) {
                                val currentY = h - ((currentRate - yMin) / yRange * h).toFloat()
                                drawLine(
                                    color = currentLineColor,
                                    start = androidx.compose.ui.geometry.Offset(0f, currentY),
                                    end = androidx.compose.ui.geometry.Offset(w, currentY),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                                )
                            }

                            // 20-day moving average line
                            if (movingAvg.size >= 2) {
                                val maPath = Path()
                                movingAvg.forEachIndexed { i, ma ->
                                    val x = (i.toFloat() / (movingAvg.size - 1)) * w
                                    val y = h - ((ma - yMin) / yRange * h).toFloat()
                                    if (i == 0) maPath.moveTo(x, y) else maPath.lineTo(x, y)
                                }
                                drawPath(
                                    path = maPath,
                                    color = maColor,
                                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // Rate line (main)
                            if (chartRates.size >= 2) {
                                val path = Path()
                                chartRates.forEachIndexed { i, hr ->
                                    val x = (i.toFloat() / (chartRates.size - 1)) * w
                                    val y = h - ((hr.rate - yMin) / yRange * h).toFloat()
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(
                                    path = path,
                                    color = lineColor,
                                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // X-axis date labels
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 44.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val labelCount = if (state.timeRange == TimeRange.ONE_MONTH) 5 else 7
                    val step = (chartRates.size - 1).coerceAtLeast(1).toFloat() / (labelCount - 1).coerceAtLeast(1)
                    for (i in 0 until labelCount) {
                        val idx = (i * step).toInt().coerceAtMost(chartRates.size - 1)
                        val date = chartRates[idx].date
                        Text(
                            text = "${date.dayOfMonth} ${shortMonth(date.monthNumber)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor,
                            fontSize = 8.sp
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChip("Low", formatY(minRate), Color(0xFFDC2626))
                    StatChip("Avg", formatY(avgRate), MaterialTheme.colorScheme.onSurfaceVariant)
                    StatChip("High", formatY(maxRate), Color(0xFF16A34A))
                    if (currentRate != null) {
                        StatChip("Now", formatY(currentRate), MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(label: String, color: Color, dashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(16.dp, 2.dp)) {
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(0f, center.y),
                end = androidx.compose.ui.geometry.Offset(size.width, center.y),
                strokeWidth = 2f,
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4f, 3f)) else null
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 9.sp)
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        Text(value, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

private fun shortMonth(month: Int): String = when (month) {
    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
    7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
    else -> ""
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun CurrencyRateCard(
    currency: Currency,
    rate: Double,
    baseCurrency: Currency,
    isSelected: Boolean,
    percentile: Int? = null,
    historicalRates: List<HistoricalRate> = emptyList(),
    color: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val minRate = historicalRates.minOfOrNull { it.rate }
    val maxRate = historicalRates.maxOfOrNull { it.rate }
    val prevRate = historicalRates.getOrNull(historicalRates.size - 2)?.rate

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                color.copy(alpha = 0.1f)
            else
                MaterialTheme.appColors.cardBackground
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, color)
        } else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: currency info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${currency.symbol} ${currency.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "1 ${currency.symbol} = ${rate.formatWithCommas()} ${baseCurrency.symbol}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Right: rate + change
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = rate.formatWithCommas(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (prevRate != null && prevRate != 0.0) {
                        val change = ((rate - prevRate) / prevRate * 100)
                        val changeStr = if (change >= 0) "+%.2f%%".let {
                            val v = kotlin.math.round(change * 100) / 100
                            "+${v}%"
                        } else {
                            val v = kotlin.math.round(change * 100) / 100
                            "${v}%"
                        }
                        val changeColor = if (change >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                        Text(
                            text = changeStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = changeColor
                        )
                    }
                }
            }

            // Sparkline + range + percentile
            if (historicalRates.size > 2 && minRate != null && maxRate != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mini sparkline with month labels
                    Column(modifier = Modifier.weight(1f)) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            val range = maxRate - minRate
                            if (range == 0.0) return@Canvas

                            val path = Path()
                            historicalRates.forEachIndexed { i, hr ->
                                val x = (i.toFloat() / (historicalRates.size - 1).coerceAtLeast(1)) * w
                                val y = h - ((hr.rate - minRate) / range * h * 0.85f).toFloat()
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        // Month labels under sparkline
                        val firstDate = historicalRates.firstOrNull()?.date
                        val lastDate = historicalRates.lastOrNull()?.date
                        if (firstDate != null && lastDate != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${shortMonth(firstDate.monthNumber)} ${firstDate.year.toString().takeLast(2)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 8.sp
                                )
                                Text(
                                    text = "${shortMonth(lastDate.monthNumber)} ${lastDate.year.toString().takeLast(2)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    // Range + percentile
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${minRate.formatWithCommas()} – ${maxRate.formatWithCommas()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        if (percentile != null) {
                            Spacer(Modifier.height(2.dp))
                            PercentileBar(percentile = percentile)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PercentileBar(percentile: Int) {
    val barColor = when {
        percentile >= 67 -> Color(0xFF16A34A)
        percentile <= 33 -> Color(0xFFDC2626)
        else -> Color(0xFFFF9800)
    }
    val label = when {
        percentile >= 75 -> stringResource(Res.string.rate_high_short)
        percentile >= 50 -> stringResource(Res.string.rate_above_avg_short)
        percentile >= 25 -> stringResource(Res.string.rate_below_avg_short)
        else -> stringResource(Res.string.rate_low_short)
    }

    Column(horizontalAlignment = Alignment.End) {
        // Bar
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentile / 100f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = barColor,
            fontSize = 9.sp
        )
    }
}

// Helper function to get consistent colors for each currency
private fun getCurrencyColor(currency: Currency): Color {
    return when (currency) {
        Currency.USD -> Color(0xFF4CAF50)
        Currency.EUR -> Color(0xFF2196F3)
        Currency.UAH -> Color(0xFFFFD700)
        Currency.PLN -> Color(0xFF9C27B0)
        Currency.GBP -> Color(0xFFE91E63)
        Currency.CHF -> Color(0xFFFF5722)
        Currency.CZK -> Color(0xFF795548)
        Currency.SEK -> Color(0xFF00BCD4)
        Currency.NOK -> Color(0xFF607D8B)
        Currency.DKK -> Color(0xFFFF9800)
        Currency.JPY -> Color(0xFFF44336)
        Currency.CAD -> Color(0xFF8BC34A)
        Currency.AUD -> Color(0xFF3F51B5)
        Currency.TRY -> Color(0xFFCDDC39)
        Currency.BRL -> Color(0xFF009688)
        Currency.RUB -> Color(0xFF455A64)
        Currency.AED -> Color(0xFFD4AF37)
    }
}