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
import com.andriybobchuk.mooney.core.widgets.WidgetDataSnapshot

/**
 * Net-worth widget — the "am I doing OK?" glance.
 *
 * Sizes:
 *  - small  (2×2): title + big number + tiny "Mooney" tag.
 *  - medium (4×2): number + this-month IN vs OUT pills.
 *
 * Tap: opens the Balance tab.
 *
 * ## Why this widget is default #1
 *
 * Every retention deep-dive on Cash App / Robinhood / Revolut identifies a
 * single big-number widget as the biggest driver of daily reopens. The user
 * doesn't need to launch the app to feel good about the number, but the
 * number itself is the emotional hook that keeps Mooney installed instead of
 * uninstalled.
 */
class BalanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(WidgetSizes.Small, WidgetSizes.Medium)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val snapshot = readSnapshot()
            val size = LocalSize.current
            if (size.width < WidgetSizes.Medium.width) BalanceSmall(snapshot)
            else BalanceMedium(snapshot)
        }
    }
}

@Composable
private fun BalanceSmall(snapshot: WidgetDataSnapshot) {
    val ctx = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetColors.SurfaceContainer)
            .cornerRadius(WidgetTokens.CornerRadius)
            .padding(WidgetTokens.Padding)
            .clickable(actionStartActivity(WidgetIntents.openAppIntent(ctx, "balance", WidgetIntents.ROUTE_ASSETS)))
    ) {
        Text(
            text = "NET WORTH",
            style = TextStyle(
                color = ColorProvider(WidgetColors.OnSurfaceMuted),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = formatMoney(snapshot.netWorthBase, snapshot.baseCurrencySymbol),
            style = TextStyle(
                color = ColorProvider(WidgetColors.OnSurface),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = "Mooney",
            style = TextStyle(
                color = ColorProvider(WidgetColors.Primary),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun BalanceMedium(snapshot: WidgetDataSnapshot) {
    val ctx = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetColors.SurfaceContainer)
            .cornerRadius(WidgetTokens.CornerRadius)
            .padding(WidgetTokens.Padding)
            .clickable(actionStartActivity(WidgetIntents.openAppIntent(ctx, "balance", WidgetIntents.ROUTE_ASSETS)))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "NET WORTH",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.OnSurfaceMuted),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = "Mooney",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.Primary),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = formatMoney(snapshot.netWorthBase, snapshot.baseCurrencySymbol),
            style = TextStyle(
                color = ColorProvider(WidgetColors.OnSurface),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            MonthPill(
                label = "IN",
                amount = snapshot.monthIncomeBase,
                symbol = snapshot.baseCurrencySymbol,
                color = WidgetColors.Income
            )
            Spacer(GlanceModifier.width(8.dp))
            MonthPill(
                label = "OUT",
                amount = snapshot.monthSpendingBase,
                symbol = snapshot.baseCurrencySymbol,
                color = WidgetColors.Expense
            )
        }
    }
}

@Composable
private fun MonthPill(label: String, amount: Double, symbol: String, color: Color) {
    Column(
        modifier = GlanceModifier
            .background(color.copy(alpha = 0.12f))
            .cornerRadius(8.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(color),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = formatMoney(amount, symbol),
            style = TextStyle(
                color = ColorProvider(WidgetColors.OnSurface),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
