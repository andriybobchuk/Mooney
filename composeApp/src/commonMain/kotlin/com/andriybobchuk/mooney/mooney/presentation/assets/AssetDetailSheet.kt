package com.andriybobchuk.mooney.mooney.presentation.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.andriybobchuk.mooney.app.appColors
import com.andriybobchuk.mooney.core.presentation.CurrencyRateChart
import com.andriybobchuk.mooney.mooney.data.CategoryLocalization
import com.andriybobchuk.mooney.mooney.domain.AccountWithConversion
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.HistoricalRate
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.asset_detail_excluded_from_networth
import mooney.composeapp.generated.resources.asset_detail_liability_note
import mooney.composeapp.generated.resources.asset_detail_market_value
import mooney.composeapp.generated.resources.asset_detail_pct_of_networth
import mooney.composeapp.generated.resources.asset_detail_primary_note
import mooney.composeapp.generated.resources.asset_detail_purchase_price
import mooney.composeapp.generated.resources.asset_detail_unrealized
import mooney.composeapp.generated.resources.delete
import mooney.composeapp.generated.resources.edit
import mooney.composeapp.generated.resources.primary_account
import mooney.composeapp.generated.resources.set_as_primary
import androidx.compose.foundation.clickable
import kotlin.math.abs
import org.jetbrains.compose.resources.stringResource

@Composable
fun AssetDetailSheet(
    asset: AccountWithConversion,
    baseCurrency: Currency,
    historicalRates: List<HistoricalRate>,
    currentRate: Double?,
    percentile: Int?,
    /** Total net worth in base currency. Drives the "% of net worth" line. */
    baseNetWorth: Double,
    onEdit: () -> Unit,
    onSetPrimary: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val isForeign = asset.originalCurrency != baseCurrency
    val supportsMarketValue = asset.assetCategoryId == "VEHICLE" ||
        asset.assetCategoryId == "REAL_ESTATE"
    val isCashLike = asset.assetCategoryId == "CASH" ||
        asset.assetCategoryId == "BANK_ACCOUNT"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Type badge — small pill with the category icon and localized name.
        // Anchors the sheet so the user knows what kind of account they're
        // looking at without reading the value first.
        val categoryLabel = assetCategoryDisplayName(asset)
        AssetTypeBadge(
            icon = assetCategoryIcon(asset.assetCategoryId),
            label = categoryLabel
        )

        Spacer(Modifier.height(12.dp))

        // Account name (secondary)
        Text(
            text = asset.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))

        // Big bold value
        Text(
            text = "${asset.originalAmount.formatWithCommas()} ${asset.originalCurrency.symbol}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Estimated value in base currency
        if (isForeign) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "≈ ${asset.baseCurrencyAmount.formatWithCommas()} ${baseCurrency.symbol}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Contextual chips (primary / liability / excluded / % of net worth).
        // Each one is opt-in based on the account's own state — none shows
        // filler noise.
        Spacer(Modifier.height(12.dp))
        FlowLikeRow {
            if (baseNetWorth > 0.0 && asset.includeInNetWorth) {
                val pct = (asset.baseCurrencyAmount / baseNetWorth * 100.0)
                val pctStr = if (pct >= 10.0) pct.toInt().toString() else formatOneDecimal(pct)
                DetailChip(stringResource(Res.string.asset_detail_pct_of_networth, pctStr))
            }
            if (asset.isPrimary) {
                DetailChip(
                    text = stringResource(Res.string.asset_detail_primary_note),
                    accent = MaterialTheme.colorScheme.primary
                )
            }
            if (!asset.includeInNetWorth) {
                DetailChip(
                    text = stringResource(Res.string.asset_detail_excluded_from_networth),
                    accent = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (asset.isLiability) {
                DetailChip(
                    text = stringResource(Res.string.asset_detail_liability_note),
                    accent = MaterialTheme.colorScheme.error
                )
            }
        }

        // Cost-basis block for Vehicle & Real Estate — surfaces purchase
        // price and the user-set market value, with an unrealized delta.
        if (supportsMarketValue) {
            Spacer(Modifier.height(20.dp))
            CostBasisBlock(
                purchasePrice = asset.originalAmount,
                marketValue = asset.currentMarketValue,
                currency = asset.originalCurrency
            )
        }

        // Chart — Cash / Bank only, and only when we have foreign-currency
        // history worth showing. Illiquid categories don't get an FX chart
        // because it isn't the meaningful signal there.
        if (isCashLike && isForeign && historicalRates.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            CurrencyRateChart(
                chartCurrency = asset.originalCurrency,
                baseCurrency = baseCurrency,
                currentRate = currentRate,
                historicalRates = historicalRates,
                percentile = percentile,
                assetAmount = asset.originalAmount,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(24.dp))

        // Action rows — Edit (default), Set as Primary (unless already
        // primary), Delete (destructive). Kept flat so every account
        // management operation is one tap deep instead of needing a
        // long-press menu.
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(Res.string.edit), modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(Modifier.height(8.dp))

        // Set as primary row — disabled when already primary, still shown
        // (with a filled tint) so users see the current role at a glance.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (asset.isPrimary) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                .clickable(enabled = !asset.isPrimary, onClick = onSetPrimary)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (asset.isPrimary) {
                    "✓ " + stringResource(Res.string.primary_account)
                } else {
                    stringResource(Res.string.set_as_primary)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (asset.isPrimary) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .clickable(onClick = onDelete)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(Res.string.delete),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AssetTypeBadge(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DetailChip(text: String, accent: androidx.compose.ui.graphics.Color? = null) {
    val color = accent ?: MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun FlowLikeRow(content: @Composable () -> Unit) {
    // Simple horizontal row with wrap-behavior approximated by spacedBy.
    // Chips are short enough that a plain Row rarely overflows; for the rare
    // case that it does, the app cap width (600dp) contains it.
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

@Composable
private fun CostBasisBlock(
    purchasePrice: Double,
    marketValue: Double?,
    currency: Currency
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(16.dp)
    ) {
        LabelValueRow(
            label = stringResource(Res.string.asset_detail_purchase_price),
            value = "${purchasePrice.formatWithCommas()} ${currency.symbol}"
        )
        if (marketValue != null) {
            Spacer(Modifier.height(8.dp))
            LabelValueRow(
                label = stringResource(Res.string.asset_detail_market_value),
                value = "${marketValue.formatWithCommas()} ${currency.symbol}"
            )
            val delta = marketValue - purchasePrice
            val pct = if (purchasePrice > 0.0) (delta / purchasePrice * 100.0) else 0.0
            val sign = if (delta >= 0) "+" else "−"
            val magnitude = abs(delta).formatWithCommas()
            val pctStr = if (abs(pct) >= 10.0) pct.toInt().toString() else formatOneDecimal(pct)
            val color = if (delta >= 0) {
                MaterialTheme.appColors.incomeColor
            } else {
                MaterialTheme.appColors.expenseColor
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    Res.string.asset_detail_unrealized,
                    "$sign$magnitude ${currency.symbol} ($sign$pctStr%)"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Formats a Double to one decimal place. `String.format` isn't in
 * `kotlin.common` (it's JVM-only), so this manual round is what we get.
 */
private fun formatOneDecimal(value: Double): String {
    val rounded = kotlin.math.round(value * 10.0) / 10.0
    // Simple string with trailing zero preserved for readability. For values
    // near .0 (12.0), the raw toString already prints "12.0", which is fine.
    return rounded.toString()
}

/**
 * Localized asset-category display name. Prefers the app's string resource
 * when the id is a known seed category; falls back to the enum's displayName
 * for anything else so custom user categories still read cleanly.
 */
@Composable
private fun assetCategoryDisplayName(asset: AccountWithConversion): String {
    val resId = CategoryLocalization.assetTitleResIds[asset.assetCategoryId]
    return if (resId != null) stringResource(resId) else asset.assetCategory.displayName
}
