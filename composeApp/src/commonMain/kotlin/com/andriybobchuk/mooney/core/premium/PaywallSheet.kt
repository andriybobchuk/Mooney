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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSubscribe: () -> Unit,
    onRestore: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // The paywall self-loads its price from PremiumManager so every call site
    // gets a working price without having to plumb it through every ViewModel.
    val premiumManager = koinInject<PremiumManager>()
    val price by premiumManager.monthlyPriceFlow.collectAsState()
    LaunchedEffect(Unit) { premiumManager.refreshMonthlyPrice() }

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
                    "Unlimited custom categories"
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

                // Price — show real value if loaded, otherwise a short placeholder.
                // We deliberately avoid an indefinite spinner here: Apple's review
                // flagged "page loaded indefinitely" because the IAP product wasn't
                // approved yet and the spinner never resolved.
                // The "/ month" suffix is required by App Store Guideline 3.1.2 —
                // auto-renewable subscriptions must show duration alongside price.
                Text(
                    text = if (price != null) "$price / month" else "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // CTA — always enabled (except while purchasing) so reviewers
                // and users are never stuck if price loading fails. StoreKit
                // will surface a native error if the product is unavailable.
                Button(
                    onClick = onSubscribe,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Subscribe",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onRestore, enabled = !isLoading) {
                    Text(
                        text = "Restore purchases",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }

                // App Store Guideline 3.1.2 — auto-renewable subscriptions must
                // disclose the subscription title, length, and price near the
                // subscribe button, and link to Terms of Use + Privacy Policy.
                Spacer(modifier = Modifier.height(8.dp))
                SubscriptionLegalFooter(price = price)
            }
        }
    }
}

@Composable
private fun SubscriptionLegalFooter(price: String?) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val disclosureColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
    val linkColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    val priceLine = if (price != null) "Mooney Pro · Monthly · $price / month" else "Mooney Pro · Monthly subscription"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = priceLine,
            style = MaterialTheme.typography.labelSmall,
            color = disclosureColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Auto-renews monthly. Cancel anytime in your App Store account.",
            style = MaterialTheme.typography.labelSmall,
            color = disclosureColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(
                onClick = {
                    uriHandler.openUri("https://www.apple.com/legal/internet-services/itunes/dev/stdeula/")
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(
                    text = "Terms of Use",
                    style = MaterialTheme.typography.labelSmall,
                    color = linkColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            TextButton(
                onClick = {
                    uriHandler.openUri("https://andriybobchuk.github.io/Mooney/privacy-policy.html")
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.labelSmall,
                    color = linkColor,
                    fontWeight = FontWeight.SemiBold
                )
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
