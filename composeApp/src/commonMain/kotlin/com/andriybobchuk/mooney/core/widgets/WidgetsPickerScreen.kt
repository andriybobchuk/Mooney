package com.andriybobchuk.mooney.core.widgets

import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andriybobchuk.mooney.core.presentation.Toolbars

/**
 * Manual widget browser. Reachable from Settings → Widgets, and from the
 * "See all widgets" CTA on the post-activation onboarding sheet.
 *
 * Purpose: after the one-time onboarding sheet is dismissed, this is the
 * only surface where users can still discover every widget variant + the
 * exact platform instructions to add them. Also serves as a marketing
 * showcase — screenshots of this screen work as an App Store / Play Store
 * feature-graphic anchor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsPickerScreen(
    platform: WidgetOnboardingPlatform,
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = {
            // TODO(i18n): translate widget copy across all 8 locales. English-only
            // for the widgets MVP so we can ship tonight without blocking on translations.
            Toolbars.Primary(
                title = "Widgets", // allow-hardcoded (widgets MVP)
                showBackButton = true,
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            HeroCard()
            Spacer(Modifier.height(16.dp))
            HowToCard(platform)
            Spacer(Modifier.height(16.dp))
            Text(
                "The lineup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            widgetCatalog().forEach { entry ->
                WidgetCatalogCard(entry)
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Mooley(mood = WidgetMooleyMood.HAPPY, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Meet Mooley",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Your money mascot lives on your home screen. Different moods for different budgets.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun HowToCard(platform: WidgetOnboardingPlatform) {
    val lines = when (platform) {
        WidgetOnboardingPlatform.ANDROID -> listOf(
            "Long-press an empty spot on your home screen",
            "Tap Widgets → find Mooney",
            "Drag your favourite one to the perfect spot"
        )
        WidgetOnboardingPlatform.IOS -> listOf(
            "Long-press an empty spot on your home screen",
            "Tap the + in the top corner",
            "Search Mooney, pick a size, tap Add Widget"
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(
            "How to add",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(10.dp))
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
            if (index < lines.lastIndex) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WidgetCatalogCard(entry: WidgetCatalogEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(entry.accent.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(entry.emoji, style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                entry.sizes,
                style = MaterialTheme.typography.labelSmall,
                color = entry.accent,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                entry.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

private data class WidgetCatalogEntry(
    val title: String,
    val emoji: String,
    val sizes: String,
    val description: String,
    val accent: Color
)

// TODO(i18n): translate widget catalog copy across all 8 locales.
// Kept as inline strings for the widgets MVP so we can ship tonight.
private fun widgetCatalog(): List<WidgetCatalogEntry> = listOf(
    WidgetCatalogEntry(
        title = "Net Worth", // allow-hardcoded (widgets MVP)
        emoji = "💰",
        sizes = "SMALL · MEDIUM", // allow-hardcoded (widgets MVP)
        description = "Your bottom line at a glance. Updates the moment you log a transaction.", // allow-hardcoded (widgets MVP)
        accent = Color(0xFF3562F6)
    ),
    WidgetCatalogEntry(
        title = "Today's Spend", // allow-hardcoded (widgets MVP)
        emoji = "☕",
        sizes = "MEDIUM", // allow-hardcoded (widgets MVP)
        description = "How much you've spent today plus the top category. Perfect after coffee.", // allow-hardcoded (widgets MVP)
        accent = Color(0xFFDC2626)
    ),
    WidgetCatalogEntry(
        title = "Budget Progress", // allow-hardcoded (widgets MVP)
        emoji = "🎯",
        sizes = "MEDIUM · LARGE", // allow-hardcoded (widgets MVP)
        description = "Your top budgets and how close you are to hitting them. Bar turns red if you overspend.", // allow-hardcoded (widgets MVP)
        accent = Color(0xFFE0A80B)
    ),
    WidgetCatalogEntry(
        title = "Streak · Mooley", // allow-hardcoded (widgets MVP)
        emoji = "🔥",
        sizes = "SMALL · MEDIUM", // allow-hardcoded (widgets MVP)
        description = "Mooley's mood mirrors your budget. Log every day to keep the fire alive.", // allow-hardcoded (widgets MVP)
        accent = Color(0xFF16A34A)
    ),
    WidgetCatalogEntry(
        title = "Quick Add", // allow-hardcoded (widgets MVP)
        emoji = "＋",
        sizes = "SMALL", // allow-hardcoded (widgets MVP)
        description = "One tap to open the Add Transaction sheet, straight from home.", // allow-hardcoded (widgets MVP)
        accent = Color(0xFF60A5FA)
    )
)
