package com.andriybobchuk.mooney.core.widgets

import kotlinx.serialization.Serializable

/**
 * The complete data blob every home-screen widget consumes.
 *
 * ## Contract
 *
 * The main app computes this whenever the underlying data changes and writes
 * it to a shared, platform-specific location:
 *  - Android: `context.filesDir/widget-snapshot.json`
 *  - iOS:     App-Group container under `UserDefaults(suiteName: "group.com.andriybobchuk.mooney.widgets")`
 *
 * Both platforms' widget hosts read this file/entry, parse it, and render.
 * The main app never talks to widgets directly — the file is the only bridge.
 * This keeps the widget-tier ignorant of Koin, Room, ViewModels, and any of
 * the rest of the domain machinery, which is essential because iOS widgets run
 * in a separate process (WidgetKit extension) that has no access to our KMP
 * framework state.
 *
 * ## Versioning
 *
 * `schemaVersion` lets an older widget process gracefully skip newer fields
 * (WidgetKit + Glance both keep the old extension around briefly after the
 * host app updates; a schema bump without a version guard would crash them).
 * Bump this integer any time you change field shapes; add a `when` guard on
 * the reader side so the widget can fall back to a "please open the app"
 * placeholder rather than crashing.
 *
 * ## Cardinality caps
 *
 * The lists here are intentionally tiny (top-3 categories, most recent day).
 * WidgetKit imposes a 30 MB memory ceiling on extensions and Glance has a
 * ~500 KB serialized-state ceiling on Android — a snapshot that carries every
 * category or transaction would blow both.
 */
@Serializable
data class WidgetDataSnapshot(
    /** Bump when field shapes change. Widgets check this and downgrade gracefully. */
    val schemaVersion: Int = SCHEMA_VERSION,

    // ───── Money summary ─────
    /** Total net worth converted to the base currency. */
    val netWorthBase: Double,
    /** Symbol of the base currency, e.g. "$", "€", "zł". */
    val baseCurrencySymbol: String,
    /** ISO code of the base currency (e.g. "USD") for locale-safe formatting. */
    val baseCurrencyCode: String,

    // ───── Spending ─────
    /** Total expense in base currency for today (local day). */
    val todaySpendingBase: Double,
    /** Top single category the user spent on today. Null if no expenses today. */
    val todayTopCategory: WidgetCategoryLine? = null,
    /** Cumulative expense in base currency for the current calendar month. */
    val monthSpendingBase: Double,
    /** Cumulative income in base currency for the current calendar month. */
    val monthIncomeBase: Double,

    // ───── Budget progress ─────
    /**
     * Top-priority budget lines — those closest to (or over) their monthly cap.
     * Sort key: percent-of-limit descending. Capped at 3 items. Only categories
     * with a monthlyLimit set are included.
     */
    val budgetLines: List<WidgetBudgetLine> = emptyList(),

    // ───── Engagement ─────
    /** Consecutive days with ≥1 transaction, ending today. 0 if broken. */
    val streakDays: Int,
    /**
     * Mooley's current mood — drives the character render on the streak widget
     * and the onboarding illustration. Derived on the writer side so the widget
     * doesn't need business logic.
     */
    val mooleyMood: WidgetMooleyMood,

    // ───── Meta ─────
    /** Wall-clock ms at write time. Widgets show "Updated 3m ago" using this. */
    val lastUpdatedMs: Long,
    /**
     * Monotonically-increasing counter bumped every write. Widget hosts on
     * iOS/Android use this as a cache-buster — if the counter matches the last
     * one they rendered, they skip the redraw. Cheaper than diffing the whole
     * snapshot.
     */
    val updateGeneration: Long
) {
    companion object {
        /** Bump alongside any field-shape change so readers know to guard. */
        const val SCHEMA_VERSION: Int = 1

        /** Safe empty snapshot to render before the writer's first pass. */
        val Empty = WidgetDataSnapshot(
            netWorthBase = 0.0,
            baseCurrencySymbol = "$",
            baseCurrencyCode = "USD",
            todaySpendingBase = 0.0,
            todayTopCategory = null,
            monthSpendingBase = 0.0,
            monthIncomeBase = 0.0,
            budgetLines = emptyList(),
            streakDays = 0,
            mooleyMood = WidgetMooleyMood.NEUTRAL,
            lastUpdatedMs = 0L,
            updateGeneration = 0L
        )
    }
}

@Serializable
data class WidgetCategoryLine(
    val id: String,
    val title: String,
    val emoji: String,
    /** Amount spent on this category today (or the requested window). Base currency. */
    val amountBase: Double
)

@Serializable
data class WidgetBudgetLine(
    val id: String,
    val title: String,
    val emoji: String,
    val spentBase: Double,
    val limitBase: Double
) {
    /** Fraction 0.0…1.5+ (capped display in the widget itself). */
    val progress: Float get() = if (limitBase > 0.0) (spentBase / limitBase).toFloat() else 0f
    /** True when spent > limit. Widget uses this to switch to the warning tint. */
    val isOverBudget: Boolean get() = spentBase > limitBase
}

/**
 * Mooley's expressive state — the character on the Streak widget morphs based
 * on this. The mapping from user financial reality to mood is done in the
 * writer, not the widget, so the widget is a dumb consumer.
 *
 *  - HAPPY       — under budget on all tracked categories AND streak ≥ 3 days
 *  - NEUTRAL     — default; user just started or nothing especially notable
 *  - WORRIED     — approaching a budget cap (≥80% on any tracked category)
 *  - OVER_BUDGET — spent > limit on at least one tracked category
 *
 * Keep the enum small — cardinality matters for the analytics `mood` param.
 */
@Serializable
enum class WidgetMooleyMood {
    HAPPY,
    NEUTRAL,
    WORRIED,
    OVER_BUDGET
}
