package com.andriybobchuk.mooney.core.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.border
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.testing.TestTags
import com.andriybobchuk.mooney.core.testing.mooneyTestTag
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.auto_renews_generic
import mooney.composeapp.generated.resources.get_mooney_pro
import mooney.composeapp.generated.resources.paywall_tier_monthly
import mooney.composeapp.generated.resources.paywall_tier_monthly_desc
import mooney.composeapp.generated.resources.paywall_tier_weekly
import mooney.composeapp.generated.resources.paywall_tier_weekly_desc
import mooney.composeapp.generated.resources.paywall_benefit_accounts_sub
import mooney.composeapp.generated.resources.paywall_benefit_accounts_title
import mooney.composeapp.generated.resources.paywall_benefit_categories_sub
import mooney.composeapp.generated.resources.paywall_benefit_categories_title
import mooney.composeapp.generated.resources.paywall_benefit_lock_sub
import mooney.composeapp.generated.resources.paywall_benefit_lock_title
import mooney.composeapp.generated.resources.paywall_benefit_noads_sub
import mooney.composeapp.generated.resources.paywall_benefit_noads_title
import mooney.composeapp.generated.resources.paywall_subtitle_more_power
import mooney.composeapp.generated.resources.paywall_title_no_limits
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
    APP_LOCK,
    GENERIC
}

private data class BenefitItem(val title: String, val subtitle: String)

// Honest list — every benefit ships in the app today. The "no ads" item
// only shows up when ads are actually enabled for this platform; promising
// "no ads" to a user who never sees ads anyway is a false claim.
@Composable
private fun rememberBenefits(): List<BenefitItem> = buildList {
    // Free-tier caps come from Remote Config so the numbers we advertise here
    // stay in lockstep with what the app actually enforces. See
    // PremiumConfig / RemoteConfigKeys.
    add(
        BenefitItem(
            stringResource(Res.string.paywall_benefit_accounts_title),
            stringResource(Res.string.paywall_benefit_accounts_sub, PremiumConfig.maxFreeAccounts)
        )
    )
    add(
        BenefitItem(
            stringResource(Res.string.paywall_benefit_categories_title),
            stringResource(Res.string.paywall_benefit_categories_sub, PremiumConfig.maxFreeCustomCategories)
        )
    )
    if (com.andriybobchuk.mooney.mooney.domain.FeatureFlags.adsEnabled) {
        add(BenefitItem(stringResource(Res.string.paywall_benefit_noads_title), stringResource(Res.string.paywall_benefit_noads_sub)))
    }
    add(BenefitItem(stringResource(Res.string.paywall_benefit_lock_title), stringResource(Res.string.paywall_benefit_lock_sub)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    trigger: PaywallTrigger = PaywallTrigger.GENERIC,
    onDismiss: () -> Unit,
    onSubscribe: (productId: String) -> Unit,
    onRestore: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val premiumManager = koinInject<PremiumManager>()
    val analyticsTracker = koinInject<com.andriybobchuk.mooney.core.analytics.AnalyticsTracker>()
    val prices by premiumManager.pricesFlow.collectAsState()
    // Default to monthly — historically the higher-LTV choice, and the user's
    // last-selected tier lasts only within this sheet mount (rememberSaveable
    // is per-instance, not persisted across app launches).
    var selectedProductId by rememberSaveable { mutableStateOf(PRODUCT_ID_MONTHLY) }
    // Timestamp captured at first composition so we can bucket the time
    // between paywall view and subscribe tap (or dismiss). Uses a plain
    // remember so it survives recompositions but resets on remount.
    val paywallShownAtMs = remember { kotlinx.datetime.Clock.System.now().toEpochMilliseconds() }
    LaunchedEffect(Unit) {
        premiumManager.refreshPrices()
        // Fires once per paywall mount — trigger label feeds the funnel chart
        // so we know which entry path actually converts.
        analyticsTracker.trackEvent(
            com.andriybobchuk.mooney.core.analytics.AnalyticsEvent.PaywallViewed(trigger.name)
        )
    }

    val wrappedOnDismiss: () -> Unit = {
        analyticsTracker.trackEvent(
            com.andriybobchuk.mooney.core.analytics.AnalyticsEvent.PaywallDismissed(trigger.name)
        )
        onDismiss()
    }
    val wrappedOnSubscribe: () -> Unit = {
        val elapsedSeconds = ((kotlinx.datetime.Clock.System.now()
            .toEpochMilliseconds() - paywallShownAtMs) / 1000L).toInt()
        analyticsTracker.trackEvent(
            com.andriybobchuk.mooney.core.analytics.AnalyticsEvent.SubscribeTap(
                productId = selectedProductId,
                trigger = trigger.name,
                timeSinceViewBucket = bucketDecisionSeconds(elapsedSeconds)
            )
        )
        onSubscribe(selectedProductId)
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
                .mooneyTestTag(TestTags.PAYWALL_SHEET)
        ) {
            PaywallMeshBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp)
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // App icon with PRO badge — same vibe as the onboarding hero
                // but compact, so the headline still leads.
                MooneyProIconBadge()

                Spacer(modifier = Modifier.height(36.dp))

                // Headline — single-line on phones; reduced from 30sp so it
                // wraps less aggressively on narrow viewports.
                Text(
                    text = stringResource(Res.string.paywall_title_no_limits),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(Res.string.paywall_subtitle_more_power),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Benefits — honest, only the two things Pro actually unlocks today.
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rememberBenefits().forEach { benefit -> BenefitRow(benefit) }
                }

                // Minimum 28dp of air between the last benefit and the first
                // tier card even on short devices where the weighted spacer
                // collapses to zero. The remaining weight pushes the cards
                // toward the bottom when there IS spare room.
                Spacer(modifier = Modifier.height(28.dp))
                Spacer(modifier = Modifier.weight(1f))

                // Tier selector — StoreKit/Play fetches take a beat on cold
                // start; show a small spinner in that window instead of two
                // blank cards, so users read "loading" not "unavailable".
                if (prices.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        strokeWidth = 2.dp
                    )
                } else {
                    TierSelector(
                        prices = prices,
                        selectedProductId = selectedProductId,
                        onSelect = { selectedProductId = it }
                    )
                }

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
        // Accent-filled circle with a white checkmark — replaces the emoji
        // per-item. Uses `primary` so the row reads as "unlocked" in both
        // light and dark schemes.
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(12.dp)
            )
        }
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
    onRestore: () -> Unit,
    enabled: Boolean
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val mutedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
    val linkColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)

    // Minimum legally required footer for a subscription IAP:
    //   1. Restore purchases (Apple 3.1.1)
    //   2. Auto-renewal disclosure (Apple 3.1.2)
    //   3. Terms of Use / EULA (Apple 3.1.2)
    //   4. Privacy Policy (Apple 5.1.1 / Play Data Safety)
    // The redundant "Mooney Pro · Monthly · $X" restatement was dropped —
    // price/duration are already shown large above the CTA.
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
            text = stringResource(Res.string.auto_renews_generic),
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

@Composable
private fun TierSelector(
    prices: Map<String, String>,
    selectedProductId: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Iterate in ALL_PRODUCT_IDS order (weekly first, monthly second) so
        // the tier ordering is authoritative and matches what the paywall
        // funnel analytics expect. Missing prices skip silently — if only one
        // SKU resolves (partial fetch, product mid-review), the user still
        // sees one usable card.
        ALL_PRODUCT_IDS.forEach { productId ->
            val price = prices[productId] ?: return@forEach
            TierCard(
                productId = productId,
                localizedPrice = price,
                isSelected = productId == selectedProductId,
                onClick = { onSelect(productId) }
            )
        }
    }
}

@Composable
private fun TierCard(
    productId: String,
    localizedPrice: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Selected card gets a heavier border + on-primary text; unselected uses
    // a low-alpha border so both cards are legible without a "which one is
    // active?" moment.
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
    }
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val titleRes = when (productId) {
        PRODUCT_ID_WEEKLY -> Res.string.paywall_tier_weekly
        else -> Res.string.paywall_tier_monthly
    }
    val descRes = when (productId) {
        PRODUCT_ID_WEEKLY -> Res.string.paywall_tier_weekly_desc
        else -> Res.string.paywall_tier_monthly_desc
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(descRes, localizedPrice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )
        }
        Text(
            text = localizedPrice,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/**
 * Buckets the seconds between paywall-shown and subscribe-tap. Small enough
 * ranges to distinguish impulse vs deliberation; wide enough that we don't
 * blow up event-property cardinality in Firebase.
 */
private fun bucketDecisionSeconds(seconds: Int): String = when {
    seconds < 5 -> "0-5"
    seconds < 15 -> "5-15"
    seconds < 60 -> "15-60"
    seconds < 300 -> "60-300"
    else -> "300+"
}
