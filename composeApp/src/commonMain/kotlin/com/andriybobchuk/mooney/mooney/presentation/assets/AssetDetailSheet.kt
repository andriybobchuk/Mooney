package com.andriybobchuk.mooney.mooney.presentation.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.app.appColors
import com.andriybobchuk.mooney.core.presentation.CurrencyRateChart
import com.andriybobchuk.mooney.mooney.data.CategoryLocalization
import com.andriybobchuk.mooney.mooney.domain.AccountWithConversion
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.HistoricalRate
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.asset_detail_add_photo
import mooney.composeapp.generated.resources.asset_detail_change_photo
import mooney.composeapp.generated.resources.asset_detail_include_in_networth
import mooney.composeapp.generated.resources.asset_detail_liability_note
import mooney.composeapp.generated.resources.asset_detail_remove_photo
import mooney.composeapp.generated.resources.asset_detail_market_value
import mooney.composeapp.generated.resources.asset_detail_pct_of_networth
import mooney.composeapp.generated.resources.asset_detail_primary_note
import mooney.composeapp.generated.resources.asset_detail_purchase_price
import mooney.composeapp.generated.resources.asset_detail_unrealized
import mooney.composeapp.generated.resources.delete
import mooney.composeapp.generated.resources.edit
import mooney.composeapp.generated.resources.reconcile_button
import androidx.compose.foundation.clickable
import com.andriybobchuk.mooney.mooney.data.localizedCategoryTitle
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Transaction
import kotlin.math.abs
import kotlinx.datetime.LocalDate
import mooney.composeapp.generated.resources.asset_detail_no_recent
import mooney.composeapp.generated.resources.asset_detail_recent_transactions
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
    /** Full transaction list, filtered inside the sheet to this account's
     *  recent 15 for display. */
    allTransactions: List<Transaction>,
    /** Loads image bytes for the hero header. iOS reads from Documents;
     *  Android stub returns null for now. */
    imageStorage: com.andriybobchuk.mooney.core.data.ImageStorage,
    /** True while the picker is open OR the chosen image is being saved. */
    isPickingImage: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit = {},
    onReconcile: () -> Unit = {},
    onToggleIncludeInNetWorth: (Boolean) -> Unit = {},
    onPickImage: () -> Unit = {},
    onRemoveImage: () -> Unit = {},
    canReconcile: Boolean = true
) {
    val isForeign = asset.originalCurrency != baseCurrency
    val supportsMarketValue = asset.assetCategoryId == "VEHICLE" ||
        asset.assetCategoryId == "REAL_ESTATE"
    val isCashLike = asset.assetCategoryId == "CASH" ||
        asset.assetCategoryId == "BANK_ACCOUNT"
    // Include transactions where this account is EITHER the source OR the
    // destination of a transfer. Transfers are stored with
    // `account = source` and `subcategory.id = "transfer_to_<destId>"`, so
    // naive `account.id == asset.id` filtering misses every incoming
    // transfer on the destination account's sheet.
    val recentTxs = allTransactions
        .asSequence()
        .filter { tx ->
            tx.account.id == asset.id || isIncomingTransferTo(tx, asset.id)
        }
        .sortedByDescending { it.date }
        .take(15)
        .toList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // ---- HERO: full-bleed image at the top edge of the sheet ----
        // Only for market-value asset types. The sheet is mounted with
        // dragHandle=null when applicable so this image hits y=0 of the
        // sheet with no white strip above.
        if (supportsMarketValue && asset.imagePath != null) {
            ImageHero(
                imagePath = asset.imagePath,
                imageStorage = imageStorage,
                isPickingImage = isPickingImage,
                onPickImage = onPickImage,
                onRemoveImage = onRemoveImage
            )
        }

        // ---- TITLE / CATEGORY / AMOUNT ----
        // Tight stack below the image (or at the sheet top when there's no
        // image). The compact spacing closes the gap to the action buttons
        // the user flagged as "way too much distance from the titles".
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = assetCategoryDisplayName(asset),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "${asset.originalAmount.formatWithCommas()} ${asset.originalCurrency.symbol}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isForeign) {
                    Text(
                        text = "≈ ${asset.baseCurrencyAmount.formatWithCommas()} ${baseCurrency.symbol}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // "Add photo" affordance in the top-right of the sheet when the
            // account is a market-value type that doesn't have a photo yet.
            // Industry pattern: compact circular icon-button in the corner.
            if (supportsMarketValue && asset.imagePath == null) {
                AddPhotoCornerButton(
                    isLoading = isPickingImage,
                    onClick = onPickImage
                )
            }
        }

        // Everything below shares the standard 20dp horizontal padding.
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {

        // ---- Contextual chips ----
        // When the hero rendered as an image, we're already visually anchored
        // and chips sit directly below. Without an image hero, the parent
        // Column already padded 20dp top so just use the standard 14dp gap.
        Spacer(Modifier.height(if (supportsMarketValue && asset.imagePath != null) 16.dp else 14.dp))
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
            if (asset.isLiability) {
                DetailChip(
                    text = stringResource(Res.string.asset_detail_liability_note),
                    accent = MaterialTheme.colorScheme.error
                )
            }
        }

        // ---- ACTIONS (at TOP so the user never has to scroll past the
        //      transactions list to reach them) ----
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(Res.string.edit), modifier = Modifier.padding(vertical = 4.dp))
            }
            if (canReconcile) {
                OutlinedButton(
                    onClick = onReconcile,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        stringResource(Res.string.reconcile_button),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        // ---- Inline settings: "Include in net worth" toggle ----
        Spacer(Modifier.height(10.dp))
        InlineToggleRow(
            label = stringResource(Res.string.asset_detail_include_in_networth),
            checked = asset.includeInNetWorth,
            onCheckedChange = onToggleIncludeInNetWorth
        )

        // Image controls now live in the hero itself (edit menu in the
        // top-right of the image) + the corner "Add photo" button in the
        // title row when no image exists — no inline row needed.

        // ---- Cost basis for market-value assets ----
        if (supportsMarketValue) {
            Spacer(Modifier.height(16.dp))
            CostBasisBlock(
                purchasePrice = asset.originalAmount,
                marketValue = asset.currentMarketValue,
                currency = asset.originalCurrency
            )
        }

        // ---- FX chart for foreign cash-like accounts ----
        if (isCashLike && isForeign && historicalRates.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
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

        // ---- RECENT TRANSACTIONS ----
        Spacer(Modifier.height(22.dp))
        SectionHeader(stringResource(Res.string.asset_detail_recent_transactions))
        Spacer(Modifier.height(8.dp))
        if (recentTxs.isEmpty()) {
            Text(
                text = stringResource(Res.string.asset_detail_no_recent),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 14.dp, vertical = 16.dp)
            )
        } else {
            // No container background, no dividers — rows breathe on their own
            // via consistent vertical padding. Keeps the list reading like a
            // scannable timeline instead of a boxed table.
            Column(modifier = Modifier.fillMaxWidth()) {
                recentTxs.forEach { tx ->
                    RecentTxRow(
                        tx = tx,
                        viewedAccountId = asset.id,
                        allAccounts = allTransactions
                    )
                }
            }
        }

        // ---- DESTRUCTIVE (bottom, below everything else) ----
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .clickable(onClick = onDelete)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                stringResource(Res.string.delete),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        Spacer(Modifier.height(16.dp))
        } // end: inner Column (bottom-pad-20)
    }
}

/**
 * Full-bleed image hero for market-value accounts. Image fills the top
 * edge of the sheet (sheet is mounted with `dragHandle = null` so there's
 * no white strip above). Edit menu lives in a circular button in the
 * top-right corner — iOS Photos / Google Photos / most galleries use this
 * exact pattern. Dropdown menu offers Replace / Remove.
 *
 * No text is overlaid on the image anymore — title + amount live in the
 * regular sheet content area below, which the user asked for because
 * overlaid text wasn't readable enough against varied photo content.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ImageHero(
    imagePath: String,
    imageStorage: com.andriybobchuk.mooney.core.data.ImageStorage,
    isPickingImage: Boolean,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit
) {
    val bitmapState = androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = imagePath
    ) {
        value = runCatching {
            val bytes = imageStorage.loadImage(imagePath) ?: return@runCatching null
            com.andriybobchuk.mooney.core.platform.decodeImageBitmap(bytes)
        }.getOrNull()
    }
    val bitmap = bitmapState.value
    var menuExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    // Hero extends BEHIND the status bar (image fills to the sheet's
    // literal top edge) but the edit button + loading spinner are inset
    // via statusBarsPadding() so they sit BELOW the status bar glyphs.
    // Without this, when the sheet expands to full-height the top ~44dp
    // of the image + the edit button are hidden under the system bar.
    //
    // Height = 200dp of image + the status bar height on top. The image
    // itself still measures 200dp — we just add the inset ABOVE it so the
    // visible content area is unaffected.
    val statusBarTop = androidx.compose.foundation.layout.WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp + statusBarTop)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Loading overlay while picking / saving the next image.
        if (isPickingImage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White)
            }
        }
        // Edit menu — circular 36dp button, semi-opaque black bg, white
        // icon. Industry-standard "image edit" corner affordance.
        // statusBarsPadding so the button is tappable + visible even when
        // the sheet is in its full-height state and the status bar is
        // overlaying the top of the hero.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
                    .clickable(enabled = !isPickingImage) { menuExpanded = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.asset_detail_change_photo)) },
                    onClick = {
                        menuExpanded = false
                        onPickImage()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(Res.string.asset_detail_remove_photo),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onRemoveImage()
                    }
                )
            }
        }
    }
}

/**
 * Compact circular "+" icon button rendered in the top-right of the title
 * row when a market-value account has no photo yet. Mirrors the iOS
 * Photos / Contacts "add" affordance. Shows a progress spinner inside
 * when the picker is up.
 */
@Composable
private fun AddPhotoCornerButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.AddAPhoto,
                contentDescription = stringResource(Res.string.asset_detail_add_photo),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Compact label + Switch row for inline settings. Full-width tap target
 * so the user can hit anywhere on the row to toggle, not just the switch
 * itself.
 */
@Composable
private fun InlineToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun RecentTxRow(
    tx: Transaction,
    viewedAccountId: Int,
    allAccounts: List<Transaction>
) {
    val isTransfer = tx.subcategory.type == CategoryType.TRANSFER
    val isIncomingTransfer = isTransfer && tx.account.id != viewedAccountId
    val amountColor = when {
        isIncomingTransfer -> MaterialTheme.appColors.incomeColor
        tx.subcategory.type == CategoryType.INCOME -> MaterialTheme.appColors.incomeColor
        tx.subcategory.type == CategoryType.EXPENSE -> MaterialTheme.appColors.expenseColor
        tx.subcategory.type == CategoryType.TRANSFER -> MaterialTheme.appColors.expenseColor // outgoing
        else -> MaterialTheme.colorScheme.onSurface
    }
    val sign = when {
        isIncomingTransfer -> "+"
        tx.subcategory.type == CategoryType.INCOME -> "+"
        tx.subcategory.type == CategoryType.EXPENSE -> "−"
        tx.subcategory.type == CategoryType.TRANSFER -> "−" // outgoing
        else -> ""
    }
    // For incoming cross-currency transfers, the credited amount lives on
    // destinationAmount (in the destination's own currency). Everything
    // else uses the base amount in the source account's currency.
    val displayAmount = if (isIncomingTransfer && tx.destinationAmount != null) {
        tx.destinationAmount
    } else {
        tx.amount
    }
    val displayCurrencySymbol = if (isIncomingTransfer) {
        // Incoming transfer → shown in the VIEWED account's currency, which
        // is what the user cares about. Look up the viewed account from any
        // tx that references it; fall back to the source currency if we
        // can't find a reference.
        allAccounts
            .firstOrNull { it.account.id == viewedAccountId }
            ?.account?.currency?.symbol
            ?: tx.account.currency.symbol
    } else {
        tx.account.currency.symbol
    }
    // Primary line = user note if present, else context-appropriate label
    // (transfer counterparty or category title). Mirrors the Transactions
    // tab's convention so rows read the same regardless of where the user
    // sees them.
    val note = tx.description?.trim().orEmpty().takeIf { it.isNotEmpty() }
    val contextualLabel: String = when {
        isTransfer -> {
            val otherTitle = resolveTransferCounterpartyTitle(tx, viewedAccountId, allAccounts)
            if (isIncomingTransfer) "From $otherTitle" else "To $otherTitle"
        }
        else -> localizedCategoryTitle(tx.subcategory)
    }
    val primary = note ?: contextualLabel
    val secondary = if (note != null) {
        // Note is primary → show context + date on the secondary line so
        // the user can still identify category/counterparty at a glance.
        "$contextualLabel · ${formatShortDate(tx.date)}"
    } else {
        formatShortDate(tx.date)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = if (isTransfer) "↔" else tx.subcategory.resolveEmoji(),
            fontSize = 20.sp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = primary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = secondary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Text(
            text = "$sign${displayAmount.formatWithCommas()} $displayCurrencySymbol",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = amountColor
        )
    }
}

/** True if `tx` is a TRANSFER whose destination (per the "transfer_to_X"
 *  subcategory id convention) is [accountId]. */
private fun isIncomingTransferTo(tx: Transaction, accountId: Int): Boolean {
    if (tx.subcategory.type != CategoryType.TRANSFER) return false
    val destId = tx.subcategory.id.removePrefix("transfer_to_").toIntOrNull() ?: return false
    return destId == accountId
}

/** Finds the other-side account's title for a transfer row rendered on the
 *  viewed account's sheet. Falls back to "another account" if we can't
 *  resolve the title from any available transaction. */
private fun resolveTransferCounterpartyTitle(
    tx: Transaction,
    viewedAccountId: Int,
    allTxs: List<Transaction>
): String {
    val isSource = tx.account.id == viewedAccountId
    return if (isSource) {
        // We are the source → counterparty is the destination.
        val destId = tx.subcategory.id.removePrefix("transfer_to_").toIntOrNull()
        allTxs.firstOrNull { it.account.id == destId }?.account?.title ?: "another account"
    } else {
        // We are the destination → counterparty is the source.
        tx.account.title
    }
}

private fun formatShortDate(date: LocalDate): String {
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$month ${date.dayOfMonth}, ${date.year}"
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
