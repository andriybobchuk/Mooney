package com.andriybobchuk.mooney.widgets

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.LocalContext
import com.andriybobchuk.mooney.MainActivity
import com.andriybobchuk.mooney.core.widgets.AndroidWidgetSnapshotReader
import com.andriybobchuk.mooney.core.widgets.WidgetDataSnapshot

/**
 * Constants + helpers shared across every Mooney widget. Keeps deep-link
 * URIs, analytics tags, sizes, tokens, and colors in one place so a rename
 * doesn't mean five find-and-replaces.
 */
internal object WidgetIntents {
    const val ACTION_OPEN = "com.andriybobchuk.mooney.WIDGET_OPEN"
    const val EXTRA_ROUTE = "com.andriybobchuk.mooney.EXTRA_WIDGET_ROUTE"
    const val EXTRA_KIND = "com.andriybobchuk.mooney.EXTRA_WIDGET_KIND"

    // Routes recognized by MainActivity. String constants (not enum) so the
    // value that ships across process boundaries can't get broken by proguard.
    const val ROUTE_HOME = "home"
    const val ROUTE_ADD_TRANSACTION = "add_transaction"
    const val ROUTE_ANALYTICS = "analytics"
    const val ROUTE_ASSETS = "assets"

    /**
     * Builds an Intent that MainActivity's onCreate/onNewIntent inspects,
     * routes the user to the requested screen, and fires `widget_tapped`.
     * The [kind]/[route] pair also seeds a unique data URI so Android's
     * PendingIntent canonicalization doesn't collide across widgets.
     */
    fun openAppIntent(context: Context, kind: String, route: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_KIND, kind)
            putExtra(EXTRA_ROUTE, route)
            data = Uri.parse("mooney://widget/$kind/$route")
        }
    }
}

/**
 * Reads the shared snapshot on the Glance composition thread. Widgets should
 * always call this — never touch Room / Koin domain classes directly, because
 * Glance's provideGlance can run in a background process without our DI graph.
 */
@Composable
internal fun readSnapshot(): WidgetDataSnapshot {
    val context = LocalContext.current
    return AndroidWidgetSnapshotReader(context).read()
}

/**
 * Locale-safe compact money formatter used everywhere on widgets. Widgets are
 * tight on space; anything ≥ 10k gets shortened. Small amounts keep two
 * decimals so "$4.20" doesn't round to "$4".
 */
internal fun formatMoney(amount: Double, symbol: String): String {
    val absAmount = kotlin.math.abs(amount)
    val display = when {
        absAmount >= 1_000_000.0 -> "%.1fM".format(amount / 1_000_000.0)
        absAmount >= 10_000.0 -> "%.1fk".format(amount / 1000.0)
        absAmount < 100.0 -> "%.2f".format(amount)
        else -> "%.0f".format(amount)
    }
    return "$symbol$display"
}

/** DpSize breakpoints Glance uses for the `SizeMode.Responsive` picker. */
internal object WidgetSizes {
    // Approx 2×2 grid cells on a standard launcher.
    val Small = DpSize(width = 140.dp, height = 140.dp)
    // Approx 4×2 grid cells — the "banner" shape most users pick.
    val Medium = DpSize(width = 250.dp, height = 140.dp)
    // Approx 4×4 grid cells — full-height, room for lists + chart.
    val Large = DpSize(width = 250.dp, height = 250.dp)
    // Thin 2×1 — enough for a single-icon quick-add.
    val Thin = DpSize(width = 140.dp, height = 70.dp)
}

/** Corner radii, padding, colors — kept together so a re-skin is one file. */
internal object WidgetTokens {
    val CornerRadius = 20.dp
    val Padding = 12.dp
    val InnerPadding = 8.dp
}

/**
 * Widget palette. Glance can't consume MaterialTheme's colorScheme directly,
 * so we hand-pick the exact tints we need. These match AppTheme.Blue so
 * widgets and the main app read as the same brand.
 */
internal object WidgetColors {
    val SurfaceContainer = Color(0xFF12141A)
    val Surface = Color(0xFF1B1E27)
    val OnSurface = Color(0xFFF2F3F7)
    val OnSurfaceMuted = Color(0xFF8A8F9F)
    val Primary = Color(0xFF3562F6)
    val PrimaryOn = Color(0xFFFFFFFF)
    val Income = Color(0xFF16A34A)
    val Expense = Color(0xFFDC2626)
    val Warning = Color(0xFFE0A80B)
    val Accent = Color(0xFF60A5FA)
}
