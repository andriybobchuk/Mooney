package com.andriybobchuk.mooney.core.presentation.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Mesh-gradient background — 5 large soft blobs distributed top to bottom so
 * the effect doesn't fade out mid-canvas. Used for premium / hero surfaces
 * (paywall, net-worth flex). Distinct from the lighter [MeshGradientBackground]
 * used as the empty-state backdrop on list screens.
 *
 * The caller controls sizing via [modifier]. Pass `Modifier.matchParentSize()`
 * inside a Box to fill the parent's intrinsic content size, or
 * `Modifier.fillMaxSize()` to expand to all available space.
 */
@Composable
fun EnhancedMeshBackground(modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val accent = MaterialTheme.colorScheme.primary
    val transparent = Color.Transparent

    val blob1 = if (isDark) accent.copy(alpha = 0.22f) else Color(0xFF7AACF0).copy(alpha = 0.50f)
    val blob2 = if (isDark) accent.copy(alpha = 0.17f) else Color(0xFF9FC5F5).copy(alpha = 0.40f)
    val blob3 = if (isDark) accent.copy(alpha = 0.20f) else Color(0xFFB8D5FA).copy(alpha = 0.36f)
    val blob4 = if (isDark) accent.copy(alpha = 0.15f) else Color(0xFF93BBEC).copy(alpha = 0.32f)
    val blob5 = if (isDark) accent.copy(alpha = 0.18f) else Color(0xFFAFCDF5).copy(alpha = 0.30f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Top-left
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob1, transparent),
                center = Offset(w * 0.10f, h * 0.05f),
                radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = Offset(w * 0.10f, h * 0.05f)
        )
        // Top-right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob2, transparent),
                center = Offset(w * 0.85f, h * 0.12f),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * 0.85f, h * 0.12f)
        )
        // Center
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob3, transparent),
                center = Offset(w * 0.50f, h * 0.40f),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * 0.50f, h * 0.40f)
        )
        // Bottom-left
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob4, transparent),
                center = Offset(w * 0.15f, h * 0.78f),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * 0.15f, h * 0.78f)
        )
        // Bottom-right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob5, transparent),
                center = Offset(w * 0.88f, h * 0.92f),
                radius = w * 0.50f
            ),
            radius = w * 0.50f,
            center = Offset(w * 0.88f, h * 0.92f)
        )
    }
}
