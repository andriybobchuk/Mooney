package com.andriybobchuk.mooney.mooney.presentation.assets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas

/**
 * Full-screen "flex" overlay shown via long-press on the net-worth header.
 * Designed to be screenshot-friendly and shareable — bold typography, gradient
 * background with floating blurred shapes, conversions in the user's currencies,
 * and a Spotify-style share button at the bottom.
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
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        dragHandle = null,
        // No rounded corners — we want this to feel like a full-screen "card".
        shape = androidx.compose.ui.graphics.RectangleShape,
        // Let our content render edge-to-edge under status/nav bars; we pad
        // the actual interactive rows ourselves below.
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
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary

    // Slow drift of the gradient orbs gives a living "ambient" feel.
    val transition = rememberInfiniteTransition(label = "flexOrbs")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // Fully opaque base — the gradient sits on top so nothing of the
            // screen behind bleeds through.
            .background(accent)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent,
                        accent.blend(Color.Black, 0.15f),
                        accent.blend(Color(0xFF000022), 0.3f)
                    )
                )
            )
    ) {
        val screenWidthDp = maxWidth.value.toInt()
        // Decorative orbs — offset (not padding) so we can use negative values
        // to anchor them partway off-screen for the floating ambient feel.
        FlexOrb(
            color = onAccent.copy(alpha = 0.25f),
            size = 280.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (drift * 60 - 80).dp, y = (drift * 40 - 60).dp)
        )
        FlexOrb(
            color = Color(0xFFFFE08A).copy(alpha = 0.20f),
            size = 220.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (drift * -40 + 40).dp, y = (drift * 80 + 40).dp)
        )
        FlexOrb(
            color = Color(0xFF34D399).copy(alpha = 0.18f),
            size = 320.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (drift * 30 + 80).dp, y = (drift * 60).dp)
        )

        // Top bar: close button (padded down by the status-bar inset so it
        // doesn't sit under the notch / time indicator).
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
                    contentDescription = "Close",
                    tint = onAccent
                )
            }
        }

        // Center: the flex itself
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Net Worth",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 4.sp
                ),
                color = onAccent.copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // The hero amount — sized to fit the screen width
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
                color = onAccent,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = baseCurrency.name,
                style = MaterialTheme.typography.labelLarge,
                color = onAccent.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )

            val rank = computeWealthRank(totalInBaseCurrency, baseCurrency)
            if (rank != null) {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(onAccent.copy(alpha = 0.18f))
                        .border(
                            width = 1.dp,
                            color = onAccent.copy(alpha = 0.35f),
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
                        color = onAccent
                    )
                    Text(
                        text = " in ${rank.country}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = onAccent.copy(alpha = 0.85f)
                    )
                }
            }

            if (otherCurrencies.isNotEmpty()) {
                Spacer(Modifier.height(36.dp))

                // Subtle divider
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(onAccent.copy(alpha = 0.4f), RoundedCornerShape(1.dp))
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
                            color = onAccent.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "${converted.formatWithCommas()} ${currency.symbol}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            color = onAccent.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "  ${currency.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = onAccent.copy(alpha = 0.45f),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Bottom: share button + watermark (padded above the home indicator).
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
                    .background(onAccent)
                    .clickable {
                        onShareClick(
                            buildShareText(totalInBaseCurrency, baseCurrency, otherCurrencies, exchangeRates)
                        )
                    }
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Share",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = accent
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Mooney",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp
                ),
                color = onAccent.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun FlexOrb(color: Color, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    // Soft glow built from concentric translucent circles instead of Modifier.blur
    // (blur has runtime support issues on some platforms / iOS CMP).
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color.copy(alpha = color.alpha * 0.25f))
        )
        Box(
            modifier = Modifier
                .size(size * 0.75f)
                .clip(CircleShape)
                .background(color.copy(alpha = color.alpha * 0.45f))
        )
        Box(
            modifier = Modifier
                .size(size * 0.5f)
                .clip(CircleShape)
                .background(color)
        )
    }
}

private fun buildShareText(
    totalInBase: Double,
    baseCurrency: Currency,
    otherCurrencies: List<Currency>,
    exchangeRates: ExchangeRates
): String = buildString {
    appendLine("My net worth:")
    appendLine("${totalInBase.formatWithCommas()} ${baseCurrency.symbol} ${baseCurrency.name}")
    val rank = computeWealthRank(totalInBase, baseCurrency)
    if (rank != null) {
        appendLine("${rank.flag} ${rank.formatTopPercent()} in ${rank.country}")
    }
    if (otherCurrencies.isNotEmpty()) {
        appendLine()
        otherCurrencies.forEach { c ->
            val converted = exchangeRates.convert(totalInBase, baseCurrency, c)
            appendLine("≈ ${converted.formatWithCommas()} ${c.symbol} ${c.name}")
        }
    }
    appendLine()
    append("Tracked with Mooney 💰")
}

private fun Color.blend(other: Color, ratio: Float): Color {
    val r = ratio.coerceIn(0f, 1f)
    return Color(
        red = red * (1 - r) + other.red * r,
        green = green * (1 - r) + other.green * r,
        blue = blue * (1 - r) + other.blue * r,
        alpha = alpha
    )
}
