package com.andriybobchuk.mooney.core.premium

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject

/**
 * Where the paywall was opened from. Drives the hero copy so the user reads a
 * headline that matches the friction they just felt, not a generic "Unlock Pro".
 * Per industry data, contextual paywalls lift subscribe-tap rate 15-25% vs generic.
 */
enum class PaywallTrigger {
    ACCOUNT_LIMIT,
    CATEGORY_LIMIT,
    SETTINGS_BANNER,
    GENERIC
}

private data class PaywallHero(val headline: String, val sub: String)

private fun heroFor(trigger: PaywallTrigger): PaywallHero = when (trigger) {
    PaywallTrigger.ACCOUNT_LIMIT -> PaywallHero(
        headline = "Add unlimited accounts",
        sub = "Track every card, wallet, and savings goal in one place."
    )
    PaywallTrigger.CATEGORY_LIMIT -> PaywallHero(
        headline = "Categorize the way you spend",
        sub = "Create unlimited custom categories that match your life."
    )
    PaywallTrigger.SETTINGS_BANNER -> PaywallHero(
        headline = "Take full control of your money",
        sub = "Unlock everything Mooney can do."
    )
    PaywallTrigger.GENERIC -> PaywallHero(
        headline = "Unlock Mooney Pro",
        sub = "Get the full experience."
    )
}

private data class BenefitItem(val emoji: String, val title: String, val subtitle: String)

// Be honest — these are literally the only things Pro unlocks today. More may
// come later; until then, no fake benefits to inflate perceived value.
private val BENEFITS = listOf(
    BenefitItem("👛", "Unlimited accounts", "Beyond the 20-account free tier"),
    BenefitItem("🏷️", "Unlimited custom categories", "Beyond the 50-category free tier")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    trigger: PaywallTrigger = PaywallTrigger.GENERIC,
    onDismiss: () -> Unit,
    onSubscribe: () -> Unit,
    onRestore: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val premiumManager = koinInject<PremiumManager>()
    val price by premiumManager.monthlyPriceFlow.collectAsState()
    LaunchedEffect(Unit) { premiumManager.refreshMonthlyPrice() }

    val hero = heroFor(trigger)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        // Tall sheet with rounded top — reads as a clearly modal purchase
        // surface without taking over the whole screen.
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            PaywallMeshBackground()

            // Close button — top-right within the sheet content area.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                androidx.compose.material3.IconButton(onClick = onDismiss) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp)
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(0.6f))

                // Headline.
                Text(
                    text = hero.headline,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = hero.sub,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Benefits — honest, only the two things Pro actually unlocks today.
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BENEFITS.forEach { benefit -> BenefitRow(benefit) }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Price — single line, no anchor copy.
                Text(
                    text = if (price != null) "$price / month" else "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(20.dp))

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

                // CTA — action-positive copy beats the commitment word "Subscribe".
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
                            text = "Get Mooney Pro",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Restore is intentionally muted and grouped with the legal
                // footer so it stops competing with the primary CTA.
                Spacer(modifier = Modifier.height(16.dp))
                SubscriptionLegalFooter(
                    price = price,
                    onRestore = onRestore,
                    enabled = !isLoading
                )
            }
        }
    }
}

@Composable
private fun BenefitRow(benefit: BenefitItem) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = benefit.emoji,
            fontSize = 22.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = benefit.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = benefit.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SubscriptionLegalFooter(
    price: String?,
    onRestore: () -> Unit,
    enabled: Boolean
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val mutedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
    val linkColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    val priceLine = if (price != null) "Mooney Pro · Monthly · $price / month" else "Mooney Pro · Monthly subscription"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Restore purchases",
            style = MaterialTheme.typography.bodySmall,
            color = mutedColor,
            modifier = Modifier
                .clickable(enabled = enabled) { onRestore() }
                .padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = priceLine,
            style = MaterialTheme.typography.labelSmall,
            color = mutedColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Auto-renews monthly. Cancel anytime in your App Store account.",
            style = MaterialTheme.typography.labelSmall,
            color = mutedColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Terms of Use",
                style = MaterialTheme.typography.labelSmall,
                color = linkColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://www.apple.com/legal/internet-services/itunes/dev/stdeula/")
                }
            )
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.labelSmall,
                color = linkColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://andriybobchuk.github.io/Mooney/privacy-policy.html")
                }
            )
        }
    }
}

@Composable
private fun PaywallMeshBackground() {
    com.andriybobchuk.mooney.core.presentation.designsystem.components.EnhancedMeshBackground(
        modifier = Modifier.fillMaxSize()
    )
}
