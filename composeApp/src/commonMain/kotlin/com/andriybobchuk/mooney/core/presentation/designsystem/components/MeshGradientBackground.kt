package com.andriybobchuk.mooney.core.presentation.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun MeshGradientBackground(
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val accent = MaterialTheme.colorScheme.primary

    // Light: soft pastel blue blocks. Dark: subtle teal glow
    val strong = if (isDark) accent.copy(alpha = 0.10f) else Color(0xFF93BBEC).copy(alpha = 0.45f)
    val medium = if (isDark) accent.copy(alpha = 0.07f) else Color(0xFFAFCDF5).copy(alpha = 0.35f)
    val soft = if (isDark) accent.copy(alpha = 0.05f) else Color(0xFFC5DDFB).copy(alpha = 0.30f)
    val transparent = Color.Transparent

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Block 1 — top left, strong
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(strong, transparent),
                startY = 0f,
                endY = h * 0.4f
            ),
            topLeft = Offset(0f, 0f),
            size = Size(w * 0.3f, h * 0.4f)
        )

        // Block 2 — top center-left, stronger blue
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(medium, transparent),
                startY = 0f,
                endY = h * 0.45f
            ),
            topLeft = Offset(w * 0.2f, 0f),
            size = Size(w * 0.25f, h * 0.45f)
        )

        // Block 3 — top center, softer
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(soft, transparent),
                startY = 0f,
                endY = h * 0.35f
            ),
            topLeft = Offset(w * 0.4f, 0f),
            size = Size(w * 0.25f, h * 0.35f)
        )

        // Block 4 — top right, subtle
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(medium, transparent),
                startY = 0f,
                endY = h * 0.3f
            ),
            topLeft = Offset(w * 0.7f, 0f),
            size = Size(w * 0.3f, h * 0.3f)
        )

        // Block 5 — bottom right accent
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(transparent, soft),
                startY = h * 0.7f,
                endY = h
            ),
            topLeft = Offset(w * 0.65f, h * 0.7f),
            size = Size(w * 0.35f, h * 0.3f)
        )
    }
}
