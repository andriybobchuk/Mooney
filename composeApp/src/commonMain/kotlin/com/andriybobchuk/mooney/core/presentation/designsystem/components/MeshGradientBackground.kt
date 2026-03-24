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

    // In light mode: soft blue blobs. In dark mode: deeper teal/cyan blobs with higher alpha
    val blob1 = if (isDark) accent.copy(alpha = 0.08f) else accent.copy(alpha = 0.14f)
    val blob2 = if (isDark) accent.copy(alpha = 0.05f) else accent.copy(alpha = 0.10f)
    val blob3 = if (isDark) accent.copy(alpha = 0.10f) else accent.copy(alpha = 0.12f)
    val transparent = Color.Transparent

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob1, transparent),
                center = Offset(w * 0.1f, h * 0.15f),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * 0.1f, h * 0.15f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob2, transparent),
                center = Offset(w * 0.85f, h * 0.45f),
                radius = w * 0.5f
            ),
            radius = w * 0.5f,
            center = Offset(w * 0.85f, h * 0.45f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob3, transparent),
                center = Offset(w * 0.3f, h * 0.8f),
                radius = w * 0.45f
            ),
            radius = w * 0.45f,
            center = Offset(w * 0.3f, h * 0.8f)
        )
    }
}
