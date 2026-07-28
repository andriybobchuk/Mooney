package com.andriybobchuk.mooney.core.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Mooley — the Mooney mascot.
 *
 * A friendly rounded blob face rendered as pure Compose paths so it ships
 * with the app (zero asset overhead), scales cleanly to any widget size, and
 * changes expression based on the user's current financial mood. Four moods:
 *
 *  - HAPPY:       eyes as arcs curving up ^ ^, mouth as a wide smile
 *  - NEUTRAL:     dot eyes • •, gentle smile
 *  - WORRIED:     tilted eyebrows, small O mouth, single sweat drop
 *  - OVER_BUDGET: X eyes >< ><, downturned mouth, brow furrow
 *
 * Two accent colors: `MooleyAccent.Blue` (default, primary brand) and
 * `MooleyAccent.Green` (positive-vibes; used on the "you're saving!" surface).
 *
 * Rendering path: single Canvas so it's cheap enough to embed in a Glance
 * widget. On iOS we'll port these same shapes to SwiftUI's `Path` API — the
 * geometry lives in [MooleyGeometry] so both platforms can compute identical
 * proportions without duplicating math.
 *
 * ## Character-brand rationale
 *
 * A cute mascot on the home screen widget massively lifts recall + word of
 * mouth. Duolingo's owl, Snap's ghost, Robinhood's confetti — every retention
 * champion in the last decade shipped a distinct visual identity that lived
 * outside the main UI shell. Mooley's job is to do the same for Mooney.
 */
@Composable
fun Mooley(
    mood: WidgetMooleyMood,
    modifier: Modifier = Modifier,
    accent: MooleyAccent = MooleyAccent.Blue
) {
    val palette = remember(accent, mood) { MooleyPalette.forMood(mood, accent) }
    Canvas(modifier = modifier) {
        drawMooleyBody(size, palette)
        drawMooleyFace(size, palette, mood)
    }
}

enum class MooleyAccent {
    Blue,
    Green
}

/**
 * All colors Mooley uses at once. Precomputed so a recomposition doesn't
 * churn through the `when` chains for every draw call.
 */
data class MooleyPalette(
    val body: Color,
    val bodyHighlight: Color,
    val eye: Color,
    val mouth: Color,
    val blush: Color,
    val accessory: Color
) {
    companion object {
        fun forMood(mood: WidgetMooleyMood, accent: MooleyAccent): MooleyPalette {
            val bodyBase = when (accent) {
                // Mooney's primary blue — matches AppTheme.Blue's `primary`.
                MooleyAccent.Blue -> Color(0xFF3562F6)
                // Mooney's happy-green — matches the incomeColor extended color.
                MooleyAccent.Green -> Color(0xFF16A34A)
            }
            val body = when (mood) {
                WidgetMooleyMood.HAPPY, WidgetMooleyMood.NEUTRAL -> bodyBase
                // Slight warm-shift so worried Mooley reads as concerned, not sick.
                WidgetMooleyMood.WORRIED -> Color(0xFFE0A80B)
                WidgetMooleyMood.OVER_BUDGET -> Color(0xFFDC2626)
            }
            val bodyHighlight = body.copy(alpha = 0.35f)
            val eye = Color.White
            val mouth = Color(0xFF1F2937)
            val blush = when (mood) {
                WidgetMooleyMood.HAPPY -> Color(0xFFF9A8D4).copy(alpha = 0.7f)
                else -> Color.Transparent
            }
            val accessory = when (mood) {
                // Sweat drop uses a light blue regardless of body tint.
                WidgetMooleyMood.WORRIED -> Color(0xFF60A5FA)
                else -> Color.Transparent
            }
            return MooleyPalette(body, bodyHighlight, eye, mouth, blush, accessory)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Drawing primitives
// ──────────────────────────────────────────────────────────────────────────

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMooleyBody(
    size: Size,
    palette: MooleyPalette
) {
    // Round body — slight vertical squash so it reads as a "little guy"
    // rather than a plain circle.
    val cx = size.width / 2f
    val cy = size.height / 2f
    val bodyRadius = size.minDimension * 0.42f

    // Soft outer glow — makes Mooley pop off dark widget backgrounds.
    drawCircle(
        color = palette.bodyHighlight,
        radius = bodyRadius * 1.15f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = palette.body,
        radius = bodyRadius,
        center = Offset(cx, cy)
    )

    // Small top highlight — the "shine" that sells the round volume.
    drawCircle(
        color = palette.eye.copy(alpha = 0.25f),
        radius = bodyRadius * 0.18f,
        center = Offset(cx - bodyRadius * 0.32f, cy - bodyRadius * 0.45f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMooleyFace(
    size: Size,
    palette: MooleyPalette,
    mood: WidgetMooleyMood
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val faceScale = size.minDimension * 0.42f

    val eyeGap = faceScale * 0.42f
    val eyeCy = cy - faceScale * 0.10f
    val eyeSize = faceScale * 0.14f

    // Blush (drawn behind everything else on the face).
    if (palette.blush != Color.Transparent) {
        drawCircle(
            color = palette.blush,
            radius = faceScale * 0.14f,
            center = Offset(cx - faceScale * 0.55f, cy + faceScale * 0.10f)
        )
        drawCircle(
            color = palette.blush,
            radius = faceScale * 0.14f,
            center = Offset(cx + faceScale * 0.55f, cy + faceScale * 0.10f)
        )
    }

    when (mood) {
        WidgetMooleyMood.HAPPY -> {
            // ^ ^ arc-shaped happy eyes.
            drawArcEyes(cx, eyeCy, eyeGap, eyeSize, palette.mouth)
            // Wide crescent smile.
            drawCrescentSmile(
                cx = cx,
                cy = cy + faceScale * 0.25f,
                width = faceScale * 0.90f,
                height = faceScale * 0.55f,
                stroke = faceScale * 0.11f,
                color = palette.mouth
            )
        }
        WidgetMooleyMood.NEUTRAL -> {
            drawDotEyes(cx, eyeCy, eyeGap, eyeSize, palette.eye, palette.mouth)
            // Gentle upturned line.
            drawCrescentSmile(
                cx = cx,
                cy = cy + faceScale * 0.30f,
                width = faceScale * 0.60f,
                height = faceScale * 0.28f,
                stroke = faceScale * 0.09f,
                color = palette.mouth
            )
        }
        WidgetMooleyMood.WORRIED -> {
            drawDotEyes(cx, eyeCy, eyeGap, eyeSize, palette.eye, palette.mouth)
            drawWorriedBrows(cx, eyeCy - eyeSize * 2f, eyeGap, faceScale, palette.mouth)
            // Small O mouth.
            drawCircle(
                color = palette.mouth,
                radius = faceScale * 0.11f,
                center = Offset(cx, cy + faceScale * 0.30f),
                style = Stroke(width = faceScale * 0.06f)
            )
            drawSweatDrop(cx + faceScale * 0.65f, eyeCy - faceScale * 0.20f, faceScale * 0.22f, palette.accessory)
        }
        WidgetMooleyMood.OVER_BUDGET -> {
            drawXEyes(cx, eyeCy, eyeGap, eyeSize, palette.mouth)
            // Downturned mouth (frown = crescent flipped).
            drawFrown(
                cx = cx,
                cy = cy + faceScale * 0.35f,
                width = faceScale * 0.70f,
                height = faceScale * 0.30f,
                stroke = faceScale * 0.11f,
                color = palette.mouth
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDotEyes(
    cx: Float, cy: Float, gap: Float, size: Float, eyeWhite: Color, pupil: Color
) {
    drawCircle(color = eyeWhite, radius = size, center = Offset(cx - gap, cy))
    drawCircle(color = eyeWhite, radius = size, center = Offset(cx + gap, cy))
    drawCircle(color = pupil, radius = size * 0.55f, center = Offset(cx - gap, cy))
    drawCircle(color = pupil, radius = size * 0.55f, center = Offset(cx + gap, cy))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArcEyes(
    cx: Float, cy: Float, gap: Float, size: Float, color: Color
) {
    val stroke = Stroke(width = size * 0.55f, pathEffect = null)
    val arcSize = Size(size * 2f, size * 1.4f)
    drawArc(
        color = color,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx - gap - size, cy - size * 0.7f),
        size = arcSize,
        style = stroke
    )
    drawArc(
        color = color,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx + gap - size, cy - size * 0.7f),
        size = arcSize,
        style = stroke
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawXEyes(
    cx: Float, cy: Float, gap: Float, size: Float, color: Color
) {
    val len = size * 1.1f
    val stroke = Stroke(width = size * 0.35f)
    fun x(atX: Float) {
        drawLine(color, Offset(atX - len, cy - len), Offset(atX + len, cy + len), stroke.width)
        drawLine(color, Offset(atX - len, cy + len), Offset(atX + len, cy - len), stroke.width)
    }
    x(cx - gap)
    x(cx + gap)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWorriedBrows(
    cx: Float, cy: Float, gap: Float, faceScale: Float, color: Color
) {
    val len = faceScale * 0.28f
    val rise = faceScale * 0.10f
    // Left brow: outer-high, inner-low → concerned tilt.
    drawLine(
        color,
        start = Offset(cx - gap - len / 2f, cy),
        end = Offset(cx - gap + len / 2f, cy + rise),
        strokeWidth = faceScale * 0.06f
    )
    // Right brow mirrors: outer-high, inner-low.
    drawLine(
        color,
        start = Offset(cx + gap + len / 2f, cy),
        end = Offset(cx + gap - len / 2f, cy + rise),
        strokeWidth = faceScale * 0.06f
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrescentSmile(
    cx: Float, cy: Float, width: Float, height: Float, stroke: Float, color: Color
) {
    drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(cx - width / 2f, cy - height / 2f),
        size = Size(width, height),
        style = Stroke(width = stroke)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFrown(
    cx: Float, cy: Float, width: Float, height: Float, stroke: Float, color: Color
) {
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(cx - width / 2f, cy - height / 2f),
        size = Size(width, height),
        style = Stroke(width = stroke)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSweatDrop(
    cx: Float, cy: Float, height: Float, color: Color
) {
    // Teardrop: a circle body with a triangular tip on top.
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

/**
 * Geometry constants shared with the iOS SwiftUI port. When we port Mooley
 * to SwiftUI, the Swift side should honor these ratios so both platforms
 * render an identical character.
 */
object MooleyGeometry {
    const val BODY_RADIUS_RATIO: Float = 0.42f
    const val EYE_GAP_RATIO: Float = 0.42f
    const val EYE_SIZE_RATIO: Float = 0.14f
    const val EYE_VERTICAL_OFFSET_RATIO: Float = 0.10f
    const val SMILE_HEIGHT_RATIO: Float = 0.55f
    const val SMILE_WIDTH_RATIO: Float = 0.90f
}

