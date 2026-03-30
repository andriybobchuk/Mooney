package com.andriybobchuk.mooney.core.premium

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    onDismiss: () -> Unit,
    onSubscribe: () -> Unit,
    onRestore: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Box {
            PaywallMeshBackground()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Unlock Mooney Pro",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Get the full experience",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Benefits
                val benefits = listOf(
                    "Unlimited accounts",
                    "Unlimited custom categories",
                    "Multi-currency support",
                    "Advanced analytics & trends",
                    "Data export & import",
                    "Priority support"
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    benefits.forEach { benefit ->
                        BenefitRow(text = benefit)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Price
                Text(
                    text = "$2.49 / month",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "or $17.99/year (save 40%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // CTA
                Button(
                    onClick = onSubscribe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    )
                ) {
                    Text(
                        text = "Start Free Trial",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onRestore) {
                    Text(
                        text = "Restore purchases",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun PaywallMeshBackground() {
    val isDark = isSystemInDarkTheme()
    val accent = MaterialTheme.colorScheme.primary
    val transparent = Color.Transparent

    val blob1 = if (isDark) accent.copy(alpha = 0.20f) else Color(0xFF7AACF0).copy(alpha = 0.50f)
    val blob2 = if (isDark) accent.copy(alpha = 0.15f) else Color(0xFF9FC5F5).copy(alpha = 0.40f)
    val blob3 = if (isDark) accent.copy(alpha = 0.18f) else Color(0xFFB8D5FA).copy(alpha = 0.35f)

    Canvas(modifier = Modifier.fillMaxWidth().height(400.dp)) {
        val w = size.width
        val h = size.height

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob1, transparent),
                center = Offset(w * 0.1f, h * 0.05f),
                radius = w * 0.6f
            ),
            radius = w * 0.6f,
            center = Offset(w * 0.1f, h * 0.05f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob2, transparent),
                center = Offset(w * 0.85f, h * 0.15f),
                radius = w * 0.5f
            ),
            radius = w * 0.5f,
            center = Offset(w * 0.85f, h * 0.15f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob3, transparent),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = w * 0.45f
            ),
            radius = w * 0.45f,
            center = Offset(w * 0.5f, h * 0.45f)
        )
    }
}
