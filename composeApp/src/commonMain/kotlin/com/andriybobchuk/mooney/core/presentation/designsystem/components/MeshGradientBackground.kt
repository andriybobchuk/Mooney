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
    val blobColor1 = primary.copy(alpha = 0.06f)
    val blobColor2 = primary.copy(alpha = 0.04f)
    val transparent = Color.Transparent

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Top-left blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blobColor1, transparent),
                center = Offset(w * 0.15f, h * 0.2f),
                radius = w * 0.5f
            ),
            radius = w * 0.5f,
            center = Offset(w * 0.15f, h * 0.2f)
        )

        // Center-right blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blobColor2, transparent),
                center = Offset(w * 0.8f, h * 0.45f),
                radius = w * 0.45f
            ),
            radius = w * 0.45f,
            center = Offset(w * 0.8f, h * 0.45f)
        )

        // Bottom-center blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blobColor1, transparent),
                center = Offset(w * 0.4f, h * 0.75f),
                radius = w * 0.4f
            ),
            radius = w * 0.4f,
            center = Offset(w * 0.4f, h * 0.75f)
        )
    }
}
