package com.andriybobchuk.mooney.widgets

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import org.koin.core.context.GlobalContext

/**
 * Each Glance widget needs its own AppWidgetProvider registered in the
 * manifest — the receiver is the OS-visible surface, the `GlanceAppWidget`
 * class is the composition logic. Keeping them in one file makes the
 * one-to-one mapping obvious.
 *
 * Each receiver also fires analytics on lifecycle events:
 *  - onEnabled  → widget_added (first instance of this kind was placed)
 *  - onDisabled → widget_removed (last instance was removed)
 *
 * onEnabled/onDisabled are per-widget-kind, not per-instance, so Google's
 * AppWidgetManager guarantees they fire exactly at the "did the user start
 * caring about this kind of widget?" moments — perfect for analytics.
 */
private fun fireLifecycleEvent(event: AnalyticsEvent) {
    try {
        GlobalContext.getOrNull()?.get<AnalyticsTracker>()?.trackEvent(event)
    } catch (_: Throwable) {
        // Analytics is best-effort; a Koin miss here should never crash.
    }
}

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget()
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        fireLifecycleEvent(AnalyticsEvent.WidgetAdded(kind = "balance", size = "auto"))
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        fireLifecycleEvent(AnalyticsEvent.WidgetRemoved(kind = "balance", size = "auto"))
    }
}

class TodaySpendingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodaySpendingWidget()
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        fireLifecycleEvent(AnalyticsEvent.WidgetAdded(kind = "today", size = "medium"))
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        fireLifecycleEvent(AnalyticsEvent.WidgetRemoved(kind = "today", size = "medium"))
    }
}

class BudgetProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetProgressWidget()
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        fireLifecycleEvent(AnalyticsEvent.WidgetAdded(kind = "budget", size = "auto"))
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        fireLifecycleEvent(AnalyticsEvent.WidgetRemoved(kind = "budget", size = "auto"))
    }
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        fireLifecycleEvent(AnalyticsEvent.WidgetAdded(kind = "streak", size = "auto"))
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        fireLifecycleEvent(AnalyticsEvent.WidgetRemoved(kind = "streak", size = "auto"))
    }
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        fireLifecycleEvent(AnalyticsEvent.WidgetAdded(kind = "quick_add", size = "thin"))
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        fireLifecycleEvent(AnalyticsEvent.WidgetRemoved(kind = "quick_add", size = "thin"))
    }
}
