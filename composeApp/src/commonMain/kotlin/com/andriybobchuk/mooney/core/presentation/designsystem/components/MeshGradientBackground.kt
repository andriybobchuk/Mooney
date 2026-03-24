package com.andriybobchuk.mooney.core.presentation.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun MeshGradientBackground(
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val accent = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background

    val blob1 = if (isDark) accent.copy(alpha = 0.10f) else Color(0xFF93BBEC).copy(alpha = 0.30f)
    val blob2 = if (isDark) accent.copy(alpha = 0.07f) else Color(0xFFAFCDF5).copy(alpha = 0.25f)
    val blob3 = if (isDark) accent.copy(alpha = 0.08f) else Color(0xFFC5DDFB).copy(alpha = 0.20f)
    val transparent = Color.Transparent

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Fill with theme background
        drawRect(color = bg)

        // Top-left blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob1, transparent),
                center = Offset(w * 0.15f, h * 0.12f),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * 0.15f, h * 0.12f)
        )

        // Top-center blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob2, transparent),
                center = Offset(w * 0.55f, h * 0.08f),
                radius = w * 0.45f
            ),
            radius = w * 0.45f,
            center = Offset(w * 0.55f, h * 0.08f)
        )

        // Right blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob3, transparent),
                center = Offset(w * 0.85f, h * 0.35f),
                radius = w * 0.4f
            ),
            radius = w * 0.4f,
            center = Offset(w * 0.85f, h * 0.35f)
        )
    }
}
