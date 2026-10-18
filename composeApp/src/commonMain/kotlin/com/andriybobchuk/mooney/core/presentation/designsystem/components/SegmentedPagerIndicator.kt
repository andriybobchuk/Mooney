package com.andriybobchuk.mooney.core.presentation.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * The segmented progress bar used across onboarding + review screens. One
 * horizontal "pill" per page; filled proportionally based on current page +
 * swipe offset so the fill animates smoothly as the user drags.
 *
 * Pulled out of OnboardingScreen so the Review screen can reuse the exact
 * same visual language — the user asked that "the pager should be similar
 * to the design we have on onboarding screen, use the same components for
 * unification".
 */
@Composable
fun SegmentedPagerIndicator(
    currentStep: Int,
    totalSteps: Int,
    pagerOffset: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(totalSteps) { index ->
            val fillFraction = when {
                index < currentStep -> 1f
                index == currentStep -> (1f + pagerOffset).coerceIn(0f, 1f)
                index == currentStep + 1 && pagerOffset > 0f -> pagerOffset.coerceIn(0f, 1f)
                else -> 0f
            }
            val animatedFill by animateFloatAsState(
                targetValue = fillFraction,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "pagerSegmentFill"
            )
            val activeColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
            val inactiveColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = inactiveColor)
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = activeColor,
                        size = size.copy(width = size.width * animatedFill)
                    )
                }
            }
        }
    }
}
