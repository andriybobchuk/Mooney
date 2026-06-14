package com.andriybobchuk.mooney.mooney.presentation.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.presentation.designsystem.components.EnhancedMeshBackground
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.close
import mooney.composeapp.generated.resources.in_country_suffix
import mooney.composeapp.generated.resources.net_worth_label
import mooney.composeapp.generated.resources.share
import mooney.composeapp.generated.resources.share_my_net_worth
import mooney.composeapp.generated.resources.share_tracked_with
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen "flex" overlay shown via long-press on the net-worth header.
 * Designed to be screenshot-friendly and shareable — same mesh background as
 * the paywall for visual coherence across hero surfaces. Uses inverseSurface
 * for the share button so it stands out cleanly in both light and dark modes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthFlexSheet(
    totalInBaseCurrency: Double,
    baseCurrency: Currency,
    otherCurrencies: List<Currency>,
    exchangeRates: ExchangeRates,
    onShareClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Use the regular surface background so the mesh-blob colors render
        // properly. The previous version set containerColor = primary, which
        // washed out the mesh entirely in light mode.
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null,
        shape = RectangleShape,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        FlexSheetContent(
            totalInBaseCurrency = totalInBaseCurrency,
            baseCurrency = baseCurrency,
            otherCurrencies = otherCurrencies,
            exchangeRates = exchangeRates,
            onShareClick = onShareClick,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun FlexSheetContent(
    totalInBaseCurrency: Double,
    baseCurrency: Currency,
    otherCurrencies: List<Currency>,
    exchangeRates: ExchangeRates,
    onShareClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val shareHeader = stringResource(Res.string.share_my_net_worth)
    val shareFooter = stringResource(Res.string.share_tracked_with)
    val inCountryTemplate = stringResource(Res.string.in_country_suffix, "%1\$s")

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidthDp = maxWidth.value.toInt()

        // Mesh background — same as paywall.
        EnhancedMeshBackground(modifier = Modifier.fillMaxSize())

        // Top bar: close button, padded below the status bar inset.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topInset)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.close),
                    tint = onBg.copy(alpha = 0.75f)
                )
            }
        }

        // Center hero: net worth + rank pill + foreign conversions.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(Res.string.net_worth_label),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 4.sp
                ),
                color = onBg.copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val heroText = "${totalInBaseCurrency.formatWithCommas()} ${baseCurrency.symbol}"
            val heroFontSize = when {
                heroText.length <= 8 -> 64.sp
                heroText.length <= 12 -> 54.sp
                heroText.length <= 16 -> 44.sp
                else -> 34.sp
            }.let { if (screenWidthDp < 360) 34.sp else it }

            Text(
                text = heroText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = heroFontSize,
                    fontWeight = FontWeight.Black
                ),
                color = onBg,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = baseCurrency.name,
                style = MaterialTheme.typography.labelLarge,
                color = onBg.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )

            val rank = computeWealthRank(totalInBaseCurrency, baseCurrency)
            if (rank != null) {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(onBg.copy(alpha = 0.12f))
                        .border(
                            width = 1.dp,
                            color = onBg.copy(alpha = 0.25f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rank.flag,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = rank.formatTopPercent(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = onBg
                    )
                    Text(
                        text = " ${stringResource(Res.string.in_country_suffix, rank.country)}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = onBg.copy(alpha = 0.85f)
                    )
                }
            }

            if (otherCurrencies.isNotEmpty()) {
                Spacer(Modifier.height(36.dp))

                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(onBg.copy(alpha = 0.3f), RoundedCornerShape(1.dp))
                )

                Spacer(Modifier.height(20.dp))

                otherCurrencies.forEach { currency ->
                    val converted = exchangeRates.convert(totalInBaseCurrency, baseCurrency, currency)
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "≈ ",
                            style = MaterialTheme.typography.titleMedium,
                            color = onBg.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "${converted.formatWithCommas()} ${currency.symbol}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            color = onBg.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "  ${currency.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = onBg.copy(alpha = 0.45f),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Share button + watermark, padded above the home indicator.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomInset + 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .widthIn(min = 220.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.inverseSurface)
                    .clickable {
                        onShareClick(
                            buildShareText(
                                totalInBaseCurrency,
                                baseCurrency,
                                otherCurrencies,
                                exchangeRates,
                                shareHeader = shareHeader,
                                shareFooter = shareFooter,
                                inCountryTemplate = inCountryTemplate
                            )
                        )
                    }
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(Res.string.share),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Mooney",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp
                ),
                color = onBg.copy(alpha = 0.6f)
            )
        }
    }
}

private fun buildShareText(
    totalInBase: Double,
    baseCurrency: Currency,
    otherCurrencies: List<Currency>,
    exchangeRates: ExchangeRates,
    shareHeader: String,
    shareFooter: String,
    inCountryTemplate: String
): String = buildString {
    appendLine(shareHeader)
    appendLine("${totalInBase.formatWithCommas()} ${baseCurrency.symbol} ${baseCurrency.name}")
    val rank = computeWealthRank(totalInBase, baseCurrency)
    if (rank != null) {
        val inCountry = inCountryTemplate.replace("%1\$s", rank.country)
        appendLine("${rank.flag} ${rank.formatTopPercent()} $inCountry")
    }
    if (otherCurrencies.isNotEmpty()) {
        appendLine()
        otherCurrencies.forEach { c ->
            val converted = exchangeRates.convert(totalInBase, baseCurrency, c)
            appendLine("≈ ${converted.formatWithCommas()} ${c.symbol} ${c.name}")
        }
    }
    appendLine()
    append(shareFooter)
}
