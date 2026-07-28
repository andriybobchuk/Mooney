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
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * Quick-add widget — one-tap deep-link into the "Add Transaction" sheet.
 *
 * The whole surface is a huge tap target. No data reads, no state to keep.
 * Purpose: reduce the "log a coffee" flow from 3 taps to 1 (widget → save).
 * Same pattern as YNAB's + widget or Cash App's Pay widget.
 *
 * Sizes: thin (2×1) only — a bigger surface would waste the user's grid.
 */
class QuickAddWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(WidgetSizes.Thin))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { QuickAddContent() }
    }
}

@Composable
private fun QuickAddContent() {
    val ctx = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetColors.Primary)
            .cornerRadius(WidgetTokens.CornerRadius)
            .padding(WidgetTokens.Padding)
            .clickable(actionStartActivity(WidgetIntents.openAppIntent(ctx, "quick_add", WidgetIntents.ROUTE_ADD_TRANSACTION))),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "＋",
            style = TextStyle(
                color = ColorProvider(WidgetColors.PrimaryOn),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.width(10.dp))
        Column {
            Text(
                text = "Log a spend",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.PrimaryOn),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Mooney → tap",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.PrimaryOn.copy(alpha = 0.8f)),
                    fontSize = 11.sp
                )
            )
        }
    }
}
