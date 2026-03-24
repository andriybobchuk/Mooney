package com.andriybobchuk.mooney.core.presentation.designsystem.components

import androidx.compose.foundation.Canvas
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
    val primary = MaterialTheme.colorScheme.primary
    val blob1 = primary.copy(alpha = 0.14f)
    val blob2 = primary.copy(alpha = 0.10f)
    val transparent = Color.Transparent

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Top-left blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob1, transparent),
                center = Offset(w * 0.1f, h * 0.15f),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * 0.1f, h * 0.15f)
        )

        // Center-right blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob2, transparent),
                center = Offset(w * 0.85f, h * 0.45f),
                radius = w * 0.5f
            ),
            radius = w * 0.5f,
            center = Offset(w * 0.85f, h * 0.45f)
        )

        // Bottom-left blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob1, transparent),
                center = Offset(w * 0.3f, h * 0.8f),
                radius = w * 0.45f
            ),
            radius = w * 0.45f,
            center = Offset(w * 0.3f, h * 0.8f)
        )
    }
}
