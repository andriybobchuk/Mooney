package com.andriybobchuk.mooney.mooney.presentation.goals.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Goal
import com.andriybobchuk.mooney.mooney.domain.usecase.GoalCompletionEstimate
import com.andriybobchuk.mooney.mooney.domain.usecase.GoalProgressResult
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import kotlinx.datetime.*

data class SimpleTimelinePoint(
    val monthLabel: String,
    val amount: Double,
    val isCurrentMonth: Boolean,
    val isTargetMonth: Boolean
)

@Composable
fun SimpleGamefiedTimeline(
    goal: Goal,
    currentProgress: GoalProgressResult?,
    completionEstimate: GoalCompletionEstimate,
    modifier: Modifier = Modifier
) {
    if (completionEstimate !is GoalCompletionEstimate.EstimatedCompletion) {
        return
    }
    
    val timelineData = remember(goal, currentProgress, completionEstimate) {
        generateSimpleTimelineData(goal, currentProgress, completionEstimate)
    }
    
    if (timelineData.isEmpty()) return

    SimpleTimelineView(
        timelineData = timelineData,
        targetAmount = goal.targetAmount,
        currency = goal.currency,
        modifier = modifier.fillMaxWidth()
    )
}

// Helper function to format numbers compactly
private fun formatCompactNumber(value: Double): String {
    val intValue = value.toInt()
    return when {
        intValue >= 1_000_000 -> "${intValue / 1_000_000}M"
        intValue >= 1_000 -> "${intValue / 1_000}k"
        else -> intValue.toString()
    }
}


@Composable
private fun SimpleTimelineView(
    timelineData: List<SimpleTimelinePoint>,
    targetAmount: Double,
    currency: Currency,
    modifier: Modifier = Modifier
) {
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 2500),
        label = "timeline_animation"
    )

    // Group timeline data into rows of 6 months each
    val chunkedData = timelineData.chunked(6)
    
    // Add scrollable behavior when timeline has many rows
    val scrollState = rememberScrollState()
    val maxHeight = if (chunkedData.size > 3) 300.dp else Dp.Unspecified
    
    Column(
        modifier = modifier
            .let { if (maxHeight != Dp.Unspecified) it.heightIn(max = maxHeight).verticalScroll(scrollState) else it }
    ) {
        chunkedData.forEachIndexed { rowIndex, rowData ->
            TimelineRow(
                data = rowData,
                animationProgress = animationProgress,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (rowIndex < chunkedData.lastIndex) {
                // Wrap indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "⋯",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TimelineRow(
    data: List<SimpleTimelinePoint>,
    animationProgress: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Amount labels above circles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { point ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatCompactNumber(point.amount),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            point.isTargetMonth -> Color(0xFF10B981)
                            point.isCurrentMonth -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        },
                        fontSize = 11.sp
                    )
                }
            }
            
            // Fill empty spaces for incomplete rows
            repeat(6 - data.size) {
                Box(modifier = Modifier.weight(1f))
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Timeline with connecting line and circles
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            // Background line (only span the actual data)
            Box(
                modifier = Modifier
                    .fillMaxWidth(data.size / 6f)
                    .height(4.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        RoundedCornerShape(2.dp)
                    )
            )
            
            // Animated progress line
            val progressFraction = (animationProgress * data.size / 6f).coerceAtMost(data.size / 6f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressFraction)
                    .height(4.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                Color(0xFF10B981)
                            )
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )
            
            // Milestone circles
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                data.forEachIndexed { index, point ->
                    val scale = 1f // Always full scale for consistency
                    
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(
                                    when {
                                        point.isTargetMonth -> 24.dp * scale
                                        point.isCurrentMonth -> 22.dp * scale  
                                        else -> 18.dp * scale
                                    }
                                )
                                .clip(CircleShape)
                                .background(
                                    when {
                                        point.isTargetMonth -> Color(0xFF10B981)
                                        point.isCurrentMonth -> MaterialTheme.colorScheme.primary
                                        else -> Color.White
                                    }
                                )
                                .border(
                                    width = 3.dp,
                                    color = when {
                                        point.isTargetMonth -> Color(0xFF10B981)
                                        point.isCurrentMonth -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                point.isTargetMonth -> {
                                    Text(
                                        text = "🎯",
                                        fontSize = 10.sp
                                    )
                                }
                                point.isCurrentMonth -> {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Fill empty spaces for incomplete rows
                repeat(6 - data.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Month labels below circles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            data.forEach { point ->
                Text(
                    text = point.monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        point.isTargetMonth -> Color(0xFF10B981)
                        point.isCurrentMonth -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
                    fontWeight = if (point.isTargetMonth || point.isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    lineHeight = 11.sp
                )
            }
            
            // Fill empty spaces for incomplete rows
            repeat(6 - data.size) {
                Box(modifier = Modifier.weight(1f))
            }
        }
    }
}


private fun generateSimpleTimelineData(
    goal: Goal,
    currentProgress: GoalProgressResult?,
    completionEstimate: GoalCompletionEstimate.EstimatedCompletion
): List<SimpleTimelinePoint> {
    val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val monthlySavingsRate = completionEstimate.monthlySavingsRate
    val currentSavings = currentProgress?.savedAmount ?: 0.0
    
    val timelinePoints = mutableListOf<SimpleTimelinePoint>()
    
    // Calculate how many months we need to reach the goal
    val targetAmountInBaseCurrency = currentProgress?.targetAmount ?: goal.targetAmount
    val remainingAmount = targetAmountInBaseCurrency - currentSavings
    val monthsToComplete = if (remainingAmount <= 0) {
        0
    } else {
        kotlin.math.ceil(remainingAmount / monthlySavingsRate).toInt()
    }
    
    // Start from current month (but don't duplicate it)
    var iterDate = LocalDate(currentDate.year, currentDate.month, 1)
    var cumulativeSavings = currentSavings
    
    // Add current month
    timelinePoints.add(
        SimpleTimelinePoint(
            monthLabel = "${iterDate.month.name.take(3)}\n'${iterDate.year.toString().takeLast(2)}",
            amount = cumulativeSavings,
            isCurrentMonth = true,
            isTargetMonth = monthsToComplete == 0
        )
    )
    
    // Add each subsequent month until we reach the goal
    for (monthOffset in 1..monthsToComplete) {
        iterDate = iterDate.plus(1, DateTimeUnit.MONTH)
        cumulativeSavings += monthlySavingsRate
        
        val isTargetMonth = cumulativeSavings >= targetAmountInBaseCurrency
        val finalAmount = cumulativeSavings // Show real amount, don't cap to goal
        
        timelinePoints.add(
            SimpleTimelinePoint(
                monthLabel = if (isTargetMonth) {
                    "${iterDate.month.name.take(3)}\n'${iterDate.year.toString().takeLast(2)}"
                } else {
                    iterDate.month.name.take(3)
                },
                amount = finalAmount,
                isCurrentMonth = false,
                isTargetMonth = isTargetMonth
            )
        )
        
        // Stop when we reach the target
        if (isTargetMonth) break
    }
    
    return timelinePoints
}