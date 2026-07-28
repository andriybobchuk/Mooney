package com.andriybobchuk.mooney.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.andriybobchuk.mooney.core.widgets.WidgetBudgetLine
import com.andriybobchuk.mooney.core.widgets.WidgetDataSnapshot

/**
 * Budget-progress widget — the "am I about to blow it?" glance.
 *
 * Sizes:
 *  - medium (4×2): top-1 budget line with a horizontal bar.
 *  - large  (4×4): top-3 budget lines, each with its own bar.
 *
 * Tap: opens the Analytics tab (Expenses breakdown).
 *
 * Empty state (no budgets set): a friendly nudge to "Set a budget" that opens
 * the Analytics → Expenses → Set-limit sheet. Widget-driven feature discovery.
 */
class BudgetProgressWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(WidgetSizes.Medium, WidgetSizes.Large)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val snapshot = readSnapshot()
            val size = LocalSize.current
            val maxLines = if (size.height >= WidgetSizes.Large.height) 3 else 1
            BudgetContent(snapshot, maxLines)
        }
    }
}

@Composable
private fun BudgetContent(snapshot: WidgetDataSnapshot, maxLines: Int) {
    val ctx = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetColors.SurfaceContainer)
            .cornerRadius(WidgetTokens.CornerRadius)
            .padding(WidgetTokens.Padding)
            .clickable(actionStartActivity(WidgetIntents.openAppIntent(ctx, "budget", WidgetIntents.ROUTE_ANALYTICS)))
    ) {
        Text(
            text = "BUDGET · THIS MONTH",
            style = TextStyle(
                color = ColorProvider(WidgetColors.OnSurfaceMuted),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(6.dp))
        if (snapshot.budgetLines.isEmpty()) {
            EmptyBudgetState()
            return@Column
        }
        snapshot.budgetLines.take(maxLines).forEachIndexed { index, line ->
            if (index > 0) Spacer(GlanceModifier.height(8.dp))
            BudgetLineRow(line = line, symbol = snapshot.baseCurrencySymbol)
        }
    }
}

@Composable
private fun BudgetLineRow(line: WidgetBudgetLine, symbol: String) {
    val progressClamped = line.progress.coerceIn(0f, 1.5f)
    val fillFraction = progressClamped.coerceAtMost(1f)
    val barColor: Color = when {
        line.isOverBudget -> WidgetColors.Expense
        line.progress >= 0.80f -> WidgetColors.Warning
        else -> WidgetColors.Primary
    }
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = line.emoji.takeIf { it.isNotEmpty() } ?: "🎯",
                style = TextStyle(fontSize = 13.sp)
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = line.title,
                style = TextStyle(
                    color = ColorProvider(WidgetColors.OnSurface),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = "${formatMoney(line.spentBase, symbol)} / ${formatMoney(line.limitBase, symbol)}",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.OnSurfaceMuted),
                    fontSize = 11.sp
                )
            )
        }
        Spacer(GlanceModifier.height(3.dp))
        // Fake progress bar — Glance doesn't ship a LinearProgressIndicator so
        // we compose two Boxes horizontally with weight = fill vs remainder.
        Row(modifier = GlanceModifier.fillMaxWidth().height(6.dp)) {
            if (fillFraction > 0f) {
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(6.dp)
                        .background(barColor)
                        .cornerRadius(3.dp)
                ) { }
                if (fillFraction < 1f) {
                    Box(
                        modifier = GlanceModifier
                            // fillMaxWidth would blow the row; use inverse weight.
                            .width((250.dp * (1f - fillFraction)))
                            .height(6.dp)
                            .background(WidgetColors.OnSurfaceMuted.copy(alpha = 0.25f))
                            .cornerRadius(3.dp)
                    ) { }
                }
            } else {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(WidgetColors.OnSurfaceMuted.copy(alpha = 0.25f))
                        .cornerRadius(3.dp)
                ) { }
            }
        }
    }
}

@Composable
private fun EmptyBudgetState() {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            text = "No budgets set yet",
            style = TextStyle(
                color = ColorProvider(WidgetColors.OnSurface),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = "Tap to set a category limit — Mooley will keep an eye on it for you.",
            style = TextStyle(
                color = ColorProvider(WidgetColors.OnSurfaceMuted),
                fontSize = 11.sp
            )
        )
    }
}
