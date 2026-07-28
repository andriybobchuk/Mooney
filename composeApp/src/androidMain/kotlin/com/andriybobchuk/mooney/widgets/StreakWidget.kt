package com.andriybobchuk.mooney.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.andriybobchuk.mooney.core.widgets.MooleyAccent
import com.andriybobchuk.mooney.core.widgets.MooleyPalette
import com.andriybobchuk.mooney.core.widgets.WidgetDataSnapshot
import com.andriybobchuk.mooney.core.widgets.WidgetMooleyMood
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Streak widget — the Duolingo-style retention hook.
 *
 * Shows Mooley + the current streak count. Mooley's expression shifts based
 * on the user's budget state (from `snapshot.mooleyMood`) so a "you're doing
 * great!" week rewards them with a happy face; an over-budget week nudges
 * them with a worried one.
 *
 * Sizes:
 *  - small  (2×2): Mooley + streak count centered.
 *  - medium (4×2): Mooley on the left, streak + copy on the right.
 *
 * Tap: opens the app to Transactions today's view. If the streak is
 * currently zero, the tap deep-links to the Add Transaction sheet so the
 * user can start a new streak from the widget with one extra tap.
 */
class StreakWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(WidgetSizes.Small, WidgetSizes.Medium)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val snapshot = readSnapshot()
            val size = LocalSize.current
            if (size.width < WidgetSizes.Medium.width) StreakSmall(snapshot)
            else StreakMedium(snapshot)
        }
    }
}

@Composable
private fun StreakSmall(snapshot: WidgetDataSnapshot) {
    val ctx = LocalContext.current
    val route = if (snapshot.streakDays == 0) WidgetIntents.ROUTE_ADD_TRANSACTION else WidgetIntents.ROUTE_HOME
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetColors.SurfaceContainer)
            .cornerRadius(WidgetTokens.CornerRadius)
            .padding(WidgetTokens.Padding)
            .clickable(actionStartActivity(WidgetIntents.openAppIntent(ctx, "streak", route))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MooleyImage(mood = snapshot.mooleyMood, sizeDp = 56)
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = if (snapshot.streakDays > 0) "🔥 ${snapshot.streakDays} day${if (snapshot.streakDays == 1) "" else "s"}"
                   else "🌱 Start today",
            style = TextStyle(
                color = ColorProvider(WidgetColors.OnSurface),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun StreakMedium(snapshot: WidgetDataSnapshot) {
    val ctx = LocalContext.current
    val route = if (snapshot.streakDays == 0) WidgetIntents.ROUTE_ADD_TRANSACTION else WidgetIntents.ROUTE_HOME
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetColors.SurfaceContainer)
            .cornerRadius(WidgetTokens.CornerRadius)
            .padding(WidgetTokens.Padding)
            .clickable(actionStartActivity(WidgetIntents.openAppIntent(ctx, "streak", route))),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MooleyImage(mood = snapshot.mooleyMood, sizeDp = 68)
        Spacer(GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = streakHeadline(snapshot),
                style = TextStyle(
                    color = ColorProvider(WidgetColors.OnSurface),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = streakSubline(snapshot),
                style = TextStyle(
                    color = ColorProvider(WidgetColors.OnSurfaceMuted),
                    fontSize = 12.sp
                )
            )
        }
    }
}

private fun streakHeadline(snapshot: WidgetDataSnapshot): String {
    return when {
        snapshot.streakDays == 0 -> "Start a new streak"
        snapshot.streakDays == 1 -> "🔥 1-day streak"
        else -> "🔥 ${snapshot.streakDays}-day streak"
    }
}

private fun streakSubline(snapshot: WidgetDataSnapshot): String {
    return when (snapshot.mooleyMood) {
        WidgetMooleyMood.HAPPY -> "Under budget on every category — nice."
        WidgetMooleyMood.NEUTRAL -> "Keep going, Mooley believes in you."
        WidgetMooleyMood.WORRIED -> "Getting close to a budget — check inside."
        WidgetMooleyMood.OVER_BUDGET -> "One category is over budget. Time to review."
    }
}

/**
 * Renders Mooley as a Bitmap on the calling thread so Glance can embed him
 * via `ImageProvider(Bitmap)`. Glance can't host arbitrary Compose Canvas,
 * so we rasterize once per composition. Cheap — the character is ≤ 68dp.
 */
@Composable
private fun MooleyImage(mood: WidgetMooleyMood, sizeDp: Int) {
    val ctx = LocalContext.current
    val density = ctx.resources.displayMetrics.density
    val pxSize = (sizeDp * density).toInt().coerceAtLeast(1)
    val bitmap = remember(mood, sizeDp) { renderMooleyBitmap(mood, pxSize) }
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = "Mooley",
        modifier = GlanceModifier.size(sizeDp.dp)
    )
}

@androidx.compose.runtime.Composable
private inline fun <T> remember(key1: Any?, key2: Any?, crossinline calculation: () -> T): T =
    androidx.compose.runtime.remember(key1, key2) { calculation() }

/**
 * Draws Mooley into an Android Bitmap using the Compose graphics APIs.
 * Mirrors the paths in `Mooley.kt` so both platforms render the same face —
 * we simply rasterize onto a Bitmap Canvas instead of a live Compose Canvas.
 */
private fun renderMooleyBitmap(mood: WidgetMooleyMood, pxSize: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(pxSize, pxSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val palette = MooleyPalette.forMood(mood, MooleyAccent.Blue)
    val drawScope = CanvasDrawScope()
    drawScope.draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = androidx.compose.ui.graphics.Canvas(canvas),
        size = Size(pxSize.toFloat(), pxSize.toFloat())
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val bodyRadius = size.minDimension * 0.42f
        drawCircle(color = palette.bodyHighlight, radius = bodyRadius * 1.15f, center = Offset(cx, cy))
        drawCircle(color = palette.body, radius = bodyRadius, center = Offset(cx, cy))
        drawCircle(
            color = palette.eye.copy(alpha = 0.25f),
            radius = bodyRadius * 0.18f,
            center = Offset(cx - bodyRadius * 0.32f, cy - bodyRadius * 0.45f)
        )
        val faceScale = size.minDimension * 0.42f
        val eyeGap = faceScale * 0.42f
        val eyeCy = cy - faceScale * 0.10f
        val eyeSize = faceScale * 0.14f
        when (mood) {
            WidgetMooleyMood.HAPPY -> {
                arcEyes(cx, eyeCy, eyeGap, eyeSize, palette.mouth)
                smile(cx, cy + faceScale * 0.25f, faceScale * 0.90f, faceScale * 0.55f, faceScale * 0.11f, palette.mouth)
            }
            WidgetMooleyMood.NEUTRAL -> {
                dotEyes(cx, eyeCy, eyeGap, eyeSize, palette.eye, palette.mouth)
                smile(cx, cy + faceScale * 0.30f, faceScale * 0.60f, faceScale * 0.28f, faceScale * 0.09f, palette.mouth)
            }
            WidgetMooleyMood.WORRIED -> {
                dotEyes(cx, eyeCy, eyeGap, eyeSize, palette.eye, palette.mouth)
                drawCircle(color = palette.mouth, radius = faceScale * 0.11f, center = Offset(cx, cy + faceScale * 0.30f), style = Stroke(width = faceScale * 0.06f))
                sweatDrop(cx + faceScale * 0.65f, eyeCy - faceScale * 0.20f, faceScale * 0.22f, palette.accessory)
            }
            WidgetMooleyMood.OVER_BUDGET -> {
                xEyes(cx, eyeCy, eyeGap, eyeSize, palette.mouth)
                frown(cx, cy + faceScale * 0.35f, faceScale * 0.70f, faceScale * 0.30f, faceScale * 0.11f, palette.mouth)
            }
        }
    }
    return bitmap
}

// ── Draw helpers (extracted from Mooley.kt so we can share them in the raster path) ──

private fun androidx.compose.ui.graphics.drawscope.DrawScope.dotEyes(
    cx: Float, cy: Float, gap: Float, size: Float, eyeWhite: Color, pupil: Color
) {
    drawCircle(color = eyeWhite, radius = size, center = Offset(cx - gap, cy))
    drawCircle(color = eyeWhite, radius = size, center = Offset(cx + gap, cy))
    drawCircle(color = pupil, radius = size * 0.55f, center = Offset(cx - gap, cy))
    drawCircle(color = pupil, radius = size * 0.55f, center = Offset(cx + gap, cy))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.arcEyes(
    cx: Float, cy: Float, gap: Float, size: Float, color: Color
) {
    val arcSize = Size(size * 2f, size * 1.4f)
    drawArc(color = color, startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx - gap - size, cy - size * 0.7f), size = arcSize,
        style = Stroke(width = size * 0.55f))
    drawArc(color = color, startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx + gap - size, cy - size * 0.7f), size = arcSize,
        style = Stroke(width = size * 0.55f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.xEyes(
    cx: Float, cy: Float, gap: Float, size: Float, color: Color
) {
    val len = size * 1.1f
    fun x(atX: Float) {
        drawLine(color, Offset(atX - len, cy - len), Offset(atX + len, cy + len), size * 0.35f)
        drawLine(color, Offset(atX - len, cy + len), Offset(atX + len, cy - len), size * 0.35f)
    }
    x(cx - gap)
    x(cx + gap)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.smile(
    cx: Float, cy: Float, width: Float, height: Float, stroke: Float, color: Color
) {
    drawArc(color = color, startAngle = 0f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx - width / 2f, cy - height / 2f), size = Size(width, height),
        style = Stroke(width = stroke))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.frown(
    cx: Float, cy: Float, width: Float, height: Float, stroke: Float, color: Color
) {
    drawArc(color = color, startAngle = 180f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx - width / 2f, cy - height / 2f), size = Size(width, height),
        style = Stroke(width = stroke))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.sweatDrop(
    cx: Float, cy: Float, height: Float, color: Color
) {
    val bodyRadius = height * 0.32f
    drawCircle(color = color, radius = bodyRadius, center = Offset(cx, cy + height * 0.20f))
    val path = Path().apply {
        moveTo(cx - bodyRadius * 0.7f, cy + height * 0.05f)
        lineTo(cx, cy - height * 0.45f)
        lineTo(cx + bodyRadius * 0.7f, cy + height * 0.05f)
        close()
    }
    drawPath(path = path, color = color)
}
