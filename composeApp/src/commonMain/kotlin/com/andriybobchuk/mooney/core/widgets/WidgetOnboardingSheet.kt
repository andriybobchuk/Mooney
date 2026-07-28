package com.andriybobchuk.mooney.core.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker

/**
 * Post-activation bottom sheet that teaches the user how to add Mooney to
 * their home screen. Shown at most once per install after the user hits our
 * "activated" threshold (≥3 transactions across ≥2 distinct days).
 *
 * The sheet is intentionally short + friendly. A Mooley illustration + one
 * headline + one paragraph of platform-specific instructions + two buttons.
 * We measured this pattern (Duolingo, Notion, Yazio) — the sheets that
 * convert best are the ones with a single obvious CTA and a warm mascot,
 * NOT a laundry list of every widget variant.
 *
 * ## Analytics
 *
 * - Fires `widget_onboarding_shown(platform)` on first render.
 * - Fires `widget_onboarding_action(action)` on each button:
 *     - "add_now"    — user tapped "Show me how"
 *     - "learn_more" — user tapped "See all widgets"
 *     - "later"      — user swiped to dismiss / tapped "Later"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetOnboardingSheet(
    platform: WidgetOnboardingPlatform,
    analyticsTracker: AnalyticsTracker,
    onSeeAllWidgets: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        analyticsTracker.trackEvent(
            AnalyticsEvent.WidgetOnboardingShown(platform = platform.name.lowercase())
        )
    }

    val wrapDismiss: (String) -> Unit = { action ->
        analyticsTracker.trackEvent(AnalyticsEvent.WidgetOnboardingAction(action))
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = { wrapDismiss("later") },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Mooley — happy mood since this triggers on activation.
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Mooley(
                    mood = WidgetMooleyMood.HAPPY,
                    modifier = Modifier.size(96.dp)
                )
            }
            Spacer(Modifier.height(20.dp))

            // TODO(i18n): route through strings.xml when the widget copy is
            // translated across all 8 locales. English-only for the MVP so
            // we can ship widgets tonight without blocking on translations.
            Text(
                text = "Meet Mooley on your home screen", // allow-hardcoded (widgets MVP)
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your net worth, today's spending, budgets, streaks — all one glance away. Log a spend in a single tap.", // allow-hardcoded (widgets MVP)
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            InstructionCard(platform)
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    analyticsTracker.trackEvent(AnalyticsEvent.WidgetOnboardingAction("learn_more"))
                    onSeeAllWidgets()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("See all widgets", fontWeight = FontWeight.SemiBold) // allow-hardcoded (widgets MVP)
            }
            TextButton(onClick = { wrapDismiss("later") }) {
                Text("Later") // allow-hardcoded (widgets MVP)
            }
        }
    }
}

@Composable
private fun InstructionCard(platform: WidgetOnboardingPlatform) {
    val lines = when (platform) {
        WidgetOnboardingPlatform.ANDROID -> listOf(
            "Long-press an empty spot on your home screen",
            "Tap Widgets → Mooney",
            "Drag Mooley wherever feels right"
        )
        WidgetOnboardingPlatform.IOS -> listOf(
            "Long-press an empty spot on your home screen",
            "Tap the + in the top corner",
            "Search Mooney → pick a size → Add Widget"
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        lines.forEachIndexed { index, line ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (index < lines.lastIndex) Spacer(Modifier.height(10.dp))
        }
    }
}

enum class WidgetOnboardingPlatform {
    ANDROID,
    IOS
}
