package com.andriybobchuk.mooney.mooney.presentation.analytics

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
import kotlin.math.max
import kotlin.math.min

enum class TimePeriod(val displayName: String, val months: Int) {
    SIX_MONTHS("6mo", 6),
    ONE_YEAR("1y", 12)
}

@Composable
fun TrendChart(
    historicalData: List<MonthlyMetricSnapshot>,
    selectedMonth: MonthKey,
    onMonthSelected: (MonthKey) -> Unit,
    modifier: Modifier = Modifier,
    selectedPeriod: TimePeriod = TimePeriod.SIX_MONTHS,
    onPeriodSelected: (TimePeriod) -> Unit = {}
) {
    
    // Filter data based on selected period
    val filteredData = historicalData.takeLast(selectedPeriod.months)
    
    if (filteredData.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp) // Increased height for selector
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
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
                        .clickable { onPeriodSelected(period) }
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                            RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 11.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
        
        // Chart Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
        ) {
            drawTrendChart(filteredData, size.width, size.height)
        }
        
        // Month Labels (Clickable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            filteredData.forEach { snapshot ->
                val isSelected = snapshot.month == selectedMonth
                Box(
                    modifier = Modifier
                        .clickable { onMonthSelected(snapshot.month) }
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = snapshot.month.toShortDisplayString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Text(
                            text = "${snapshot.transactionCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}

private fun DrawScope.drawTrendChart(
    data: List<MonthlyMetricSnapshot>,
    width: Float,
    height: Float
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
    drawGridLines(padding, chartWidth, chartHeight, maxValue, minValue)
    
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
    minValue: Double
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
    
    // Draw bold zero line if zero is within the value range
    if (minValue <= 0.0 && maxValue >= 0.0) {
        val zeroY = padding + chartHeight - (chartHeight * ((0.0 - minValue) / (maxValue - minValue))).toFloat()
        drawLine(
            color = Color.Black.copy(alpha = 0.5f),
            start = Offset(padding, zeroY),
            end = Offset(padding + chartWidth, zeroY),
            strokeWidth = 1.dp.toPx()
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
    
    data.forEachIndexed { index, snapshot ->
        val value = valueExtractor(snapshot)
        val x = padding + (chartWidth * index / (data.size - 1))
        val y = padding + chartHeight - (chartHeight * ((value - minValue) / (maxValue - minValue))).toFloat()
        
        points.add(Offset(x, y))
        
        if (index == 0) {
            path.moveTo(x, y)
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

