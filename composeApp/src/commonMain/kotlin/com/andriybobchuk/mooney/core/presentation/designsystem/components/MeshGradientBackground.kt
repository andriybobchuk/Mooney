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
    val blob1 = primary.copy(alpha = 0.12f)
    val blob2 = primary.copy(alpha = 0.08f)
    val blob3 = primary.copy(alpha = 0.15f)
    val transparent = Color.Transparent

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Large top-left blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob3, transparent),
                center = Offset(w * 0.1f, h * 0.15f),
                radius = w * 0.6f
            ),
            radius = w * 0.6f,
            center = Offset(w * 0.1f, h * 0.15f)
        )

        // Medium center-right blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob1, transparent),
                center = Offset(w * 0.85f, h * 0.35f),
                radius = w * 0.5f
            ),
            radius = w * 0.5f,
            center = Offset(w * 0.85f, h * 0.35f)
        )

        // Bottom-left blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob2, transparent),
                center = Offset(w * 0.25f, h * 0.7f),
                radius = w * 0.45f
            ),
            radius = w * 0.45f,
            center = Offset(w * 0.25f, h * 0.7f)
        )

        // Bottom-right accent blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob3, transparent),
                center = Offset(w * 0.75f, h * 0.8f),
                radius = w * 0.35f
            ),
            radius = w * 0.35f,
            center = Offset(w * 0.75f, h * 0.8f)
        )
    }
}
