package com.andriybobchuk.mooney.core.premium

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.auto_renews_monthly
import mooney.composeapp.generated.resources.get_mooney_pro
import mooney.composeapp.generated.resources.paywall_acct_headline
import mooney.composeapp.generated.resources.paywall_acct_sub
import mooney.composeapp.generated.resources.paywall_benefit_accounts_sub
import mooney.composeapp.generated.resources.paywall_benefit_accounts_title
import mooney.composeapp.generated.resources.paywall_benefit_categories_sub
import mooney.composeapp.generated.resources.paywall_benefit_categories_title
import mooney.composeapp.generated.resources.paywall_benefit_noads_sub
import mooney.composeapp.generated.resources.paywall_benefit_noads_title
import mooney.composeapp.generated.resources.paywall_cat_headline
import mooney.composeapp.generated.resources.paywall_cat_sub
import mooney.composeapp.generated.resources.paywall_founder_badge
import mooney.composeapp.generated.resources.paywall_founder_locked
import mooney.composeapp.generated.resources.paywall_generic_headline
import mooney.composeapp.generated.resources.paywall_price_will_rise
import mooney.composeapp.generated.resources.paywall_generic_sub
import mooney.composeapp.generated.resources.paywall_price_with
import mooney.composeapp.generated.resources.paywall_price_without
import mooney.composeapp.generated.resources.paywall_settings_headline
import mooney.composeapp.generated.resources.paywall_settings_sub
import mooney.composeapp.generated.resources.privacy_policy
import mooney.composeapp.generated.resources.restore_purchases_link
import mooney.composeapp.generated.resources.terms_of_use
import org.jetbrains.compose.resources.stringResource
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

@Composable
private fun heroFor(trigger: PaywallTrigger): PaywallHero = when (trigger) {
    PaywallTrigger.ACCOUNT_LIMIT -> PaywallHero(
        headline = stringResource(Res.string.paywall_acct_headline),
        sub = stringResource(Res.string.paywall_acct_sub)
    )
    PaywallTrigger.CATEGORY_LIMIT -> PaywallHero(
        headline = stringResource(Res.string.paywall_cat_headline),
        sub = stringResource(Res.string.paywall_cat_sub)
    )
    PaywallTrigger.SETTINGS_BANNER -> PaywallHero(
        headline = stringResource(Res.string.paywall_settings_headline),
        sub = stringResource(Res.string.paywall_settings_sub)
    )
    PaywallTrigger.GENERIC -> PaywallHero(
        headline = stringResource(Res.string.paywall_generic_headline),
        sub = stringResource(Res.string.paywall_generic_sub)
    )
}

private data class BenefitItem(val emoji: String, val title: String, val subtitle: String)

// Be honest — these are literally the only things Pro unlocks today. More may
// come later; until then, no fake benefits to inflate perceived value.
@Composable
private fun rememberBenefits(): List<BenefitItem> = listOf(
    BenefitItem("👛", stringResource(Res.string.paywall_benefit_accounts_title), stringResource(Res.string.paywall_benefit_accounts_sub)),
    BenefitItem("🏷️", stringResource(Res.string.paywall_benefit_categories_title), stringResource(Res.string.paywall_benefit_categories_sub)),
    BenefitItem("🚫", stringResource(Res.string.paywall_benefit_noads_title), stringResource(Res.string.paywall_benefit_noads_sub))
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
    val analyticsTracker = koinInject<com.andriybobchuk.mooney.core.analytics.AnalyticsTracker>()
    val price by premiumManager.monthlyPriceFlow.collectAsState()
    LaunchedEffect(Unit) {
        premiumManager.refreshMonthlyPrice()
        // Fires once per paywall mount — trigger label feeds the funnel chart
        // so we know which entry path actually converts.
        analyticsTracker.trackEvent(
            com.andriybobchuk.mooney.core.analytics.AnalyticsEvent.PaywallViewed(trigger.name)
        )
    }

    val hero = heroFor(trigger)

    val wrappedOnDismiss: () -> Unit = {
        analyticsTracker.trackEvent(
            com.andriybobchuk.mooney.core.analytics.AnalyticsEvent.PaywallDismissed(trigger.name)
        )
        onDismiss()
    }
    val wrappedOnSubscribe: () -> Unit = {
        analyticsTracker.trackEvent(
            com.andriybobchuk.mooney.core.analytics.AnalyticsEvent.SubscribeTap(
                productId = PRODUCT_ID_MONTHLY,
                trigger = trigger.name
            )
        )
        onSubscribe()
    }

    ModalBottomSheet(
        onDismissRequest = wrappedOnDismiss,
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
                androidx.compose.material3.IconButton(onClick = wrappedOnDismiss) {
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

                Spacer(modifier = Modifier.height(20.dp))

                // Founder-pricing pill — honest scarcity framing without a fake
                // strikethrough. Apple Guideline 3.1.1 ("Misleading Customers")
                // prohibits inflated reference prices unless the SKU actually
                // sold at the higher price; "founder pricing" is compliant.
                Row(
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.paywall_founder_badge),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(Res.string.paywall_founder_locked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Benefits — honest, only the two things Pro actually unlocks today.
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rememberBenefits().forEach { benefit -> BenefitRow(benefit) }
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
                    onClick = wrappedOnSubscribe,
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
                            text = stringResource(Res.string.get_mooney_pro),
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
    val priceLine = if (price != null) stringResource(Res.string.paywall_price_with, price) else stringResource(Res.string.paywall_price_without)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.restore_purchases_link),
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
        Text(
            text = stringResource(Res.string.paywall_price_will_rise),
            style = MaterialTheme.typography.labelSmall,
            color = mutedColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.auto_renews_monthly),
            style = MaterialTheme.typography.labelSmall,
            color = mutedColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(Res.string.terms_of_use),
                style = MaterialTheme.typography.labelSmall,
                color = linkColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://www.apple.com/legal/internet-services/itunes/dev/stdeula/")
                }
            )
            Text(
                text = stringResource(Res.string.privacy_policy),
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
