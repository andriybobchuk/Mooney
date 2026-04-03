package com.andriybobchuk.mooney.mooney.presentation.exchange

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import kotlinx.datetime.*
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.alpha

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeScreen(
    viewModel: ExchangeViewModel = koinViewModel(),
    bottomNavbar: @Composable () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    
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
                            text = "Exchange Rates",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        )
                        Text(
                            text = "Base: ${state.displayBaseCurrency.symbol} ${state.displayBaseCurrency.name}",
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
        
        // Historical Chart
        item {
            HistoricalExchangeRateChart(
                state = state,
                onTimeRangeToggle = { onAction(ExchangeAction.ToggleTimeRange) },
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )
        }
        
        // Currency Rate Cards
        items(
            Currency.entries.filter { it != state.displayBaseCurrency }
        ) { currency ->
            CurrencyRateCard(
                currency = currency,
                rate = state.currentRates[currency] ?: 0.0,
                baseCurrency = state.displayBaseCurrency,
                isSelected = currency == state.selectedCurrency,
                color = getCurrencyColor(currency),
                onClick = { 
                    onAction(ExchangeAction.SelectCurrency(
                        if (currency == state.selectedCurrency) null else currency
                    ))
                }
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.appColors.cardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historical Rates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Time range selector
                Card(
                    modifier = Modifier.clickable { onTimeRangeToggle() },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = state.timeRange.label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                if (state.historicalRates.isEmpty()) {
                    Text(
                        text = "Loading historical data...",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                } else {
                    ExchangeRateLineChart(
                        historicalRates = state.historicalRates,
                        selectedCurrency = state.selectedCurrency,
                        timeRange = state.timeRange,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Month labels
            MonthLabelsRow(timeRange = state.timeRange)
        }
    }
}

@Composable
private fun ExchangeRateLineChart(
    historicalRates: Map<Currency, List<HistoricalRate>>,
    selectedCurrency: Currency?,
    timeRange: TimeRange,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Filter data based on time range
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val cutoffDate = today.minus(timeRange.months, DateTimeUnit.MONTH)
        
        historicalRates.forEach { (currency, rates) ->
            val isVisible = selectedCurrency == null || selectedCurrency == currency
            if (!isVisible) return@forEach
            
            val filteredRates = rates.filter { it.date >= cutoffDate }
            if (filteredRates.isEmpty()) return@forEach
            
            val minRate = filteredRates.minOf { it.rate }
            val maxRate = filteredRates.maxOf { it.rate }
            val rateRange = maxRate - minRate
            
            if (rateRange == 0.0) return@forEach
            
            val path = Path()
            var started = false
            
            filteredRates.forEachIndexed { index, rate ->
                val x = (index.toFloat() / (filteredRates.size - 1).coerceAtLeast(1)) * width
                val y = height - ((rate.rate - minRate) / rateRange * height * 0.9f).toFloat()
                
                if (!started) {
                    path.moveTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y)
                }
            }
            
            drawPath(
                path = path,
                color = getCurrencyColor(currency),
                style = Stroke(
                    width = if (currency == selectedCurrency) 3.dp.toPx() else 2.dp.toPx(),
                    cap = StrokeCap.Round
                ),
                alpha = if (selectedCurrency == null || selectedCurrency == currency) 1f else 0.3f
            )
        }
    }
}

@Composable
private fun CurrencyRateCard(
    currency: Currency,
    rate: Double,
    baseCurrency: Currency,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Currency color indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
            
            Spacer(Modifier.width(12.dp))
            
            // Currency info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currency.symbol,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = currency.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = "1 ${currency.symbol} = ${rate.formatWithCommas()} ${baseCurrency.symbol}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Rate value
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = rate.formatWithCommas(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = baseCurrency.symbol,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MonthLabelsRow(timeRange: TimeRange) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val monthLabels = generateMonthLabels(today, timeRange.months)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        monthLabels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

private fun generateMonthLabels(currentDate: LocalDate, monthsBack: Int): List<String> {
    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    
    val labels = mutableListOf<String>()
    val startDate = currentDate.minus(monthsBack, DateTimeUnit.MONTH)
    
    // Generate month labels from start date to current date
    for (i in 0..monthsBack) {
        val date = startDate.plus(i, DateTimeUnit.MONTH)
        val monthLabel = monthNames[date.monthNumber - 1]
        labels.add(monthLabel)
    }
    
    return labels
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