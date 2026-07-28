package com.andriybobchuk.mooney.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
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
 * Today's spending widget — the "how much have I already burned today?" glance.
 *
 * Renders today's expense total + the top single category (emoji + name +
 * amount). If the user hasn't spent today, shows a friendly "$0 spent — nice!"
 * because empty state should feel like a win, not a hole.
 *
 * Sizes: medium (4×2) only — the top-category row needs horizontal space to
 * read cleanly.
 *
 * Tap: opens the app to Transactions with today's date preselected.
 */
class TodaySpendingWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(WidgetSizes.Medium))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { TodaySpendingContent(readSnapshot()) }
    }
}

@Composable
private fun TodaySpendingContent(snapshot: WidgetDataSnapshot) {
    val ctx = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetColors.SurfaceContainer)
            .cornerRadius(WidgetTokens.CornerRadius)
            .padding(WidgetTokens.Padding)
            .clickable(actionStartActivity(WidgetIntents.openAppIntent(ctx, "today", WidgetIntents.ROUTE_HOME)))
    ) {
        Text(
            text = "SPENT TODAY",
            style = TextStyle(
                color = ColorProvider(WidgetColors.OnSurfaceMuted),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(4.dp))
        if (snapshot.todaySpendingBase <= 0.0 && snapshot.todayTopCategory == null) {
            EmptyTodayState(snapshot)
        } else {
            Text(
                text = formatMoney(snapshot.todaySpendingBase, snapshot.baseCurrencySymbol),
                style = TextStyle(
                    color = ColorProvider(WidgetColors.OnSurface),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            snapshot.todayTopCategory?.let { top ->
                Spacer(GlanceModifier.height(6.dp))
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(WidgetColors.Surface)
                        .cornerRadius(10.dp)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = top.emoji.takeIf { it.isNotEmpty() } ?: "💸",
                        style = TextStyle(fontSize = 14.sp)
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        text = top.title,
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.OnSurface),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        text = formatMoney(top.amountBase, snapshot.baseCurrencySymbol),
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.Expense),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTodayState(snapshot: WidgetDataSnapshot) {
    Text(
        text = formatMoney(0.0, snapshot.baseCurrencySymbol) + " · a clean day",
        style = TextStyle(
            color = ColorProvider(WidgetColors.Income),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    )
    Spacer(GlanceModifier.height(2.dp))
    Text(
        text = "Log the first coffee to start your streak.",
        style = TextStyle(
            color = ColorProvider(WidgetColors.OnSurfaceMuted),
            fontSize = 11.sp
        )
    )
}
