package com.andriybobchuk.mooney.mooney.presentation.assets

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.datetime.LocalDate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import mooney.composeapp.generated.resources.cancel
import mooney.composeapp.generated.resources.close
import mooney.composeapp.generated.resources.flex_history_empty
import mooney.composeapp.generated.resources.flex_monthly_history
import mooney.composeapp.generated.resources.in_country_suffix
import mooney.composeapp.generated.resources.net_worth_label
import mooney.composeapp.generated.resources.save
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
    /** Historical monthly net-worth points (oldest → newest). */
    history: List<com.andriybobchuk.mooney.mooney.domain.usecase.networth.GetNetWorthHistoryUseCase.Point>,
    /** Called when the user confirms a manual net-worth value for the
     *  given month-end — the ViewModel upserts a manual-override snapshot. */
    onEditMonth: (date: kotlinx.datetime.LocalDate, newValueInBase: Double) -> Unit,
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
            history = history,
            onEditMonth = onEditMonth,
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
    history: List<com.andriybobchuk.mooney.mooney.domain.usecase.networth.GetNetWorthHistoryUseCase.Point>,
    onEditMonth: (kotlinx.datetime.LocalDate, Double) -> Unit,
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

        // Scrollable hero + history. Previously vertically-centered — moved
        // to a top-anchored verticalScroll so the history section fits
        // below the hero without compressing it off-screen when there are
        // many months. Hero still reads as "big number first" because of
        // the top-inset padding + hero typography sizing.
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = topInset + 56.dp)
                // Reserve space at the bottom so the last history row isn't
                // obscured by the pinned share button.
                .padding(bottom = bottomInset + 140.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
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

            // Base-currency label ("PLN", "USD") removed per user feedback —
            // the symbol is already appended to the hero number, so repeating
            // the currency code below added noise. Share text still names the
            // currency explicitly in [buildShareText].

            // Wealth-rank chip ("TOP x% in Poland") removed per user feedback —
            // the comparison framing wasn't landing for everyone and the chip
            // crowded the hero. Share text still includes it (see
            // buildShareText) so social-share flexing isn't affected.

            if (otherCurrencies.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
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

            // History section — sparkline + monthly list with inline edit.
            Spacer(Modifier.height(44.dp))
            HistorySection(
                history = history,
                baseCurrency = baseCurrency,
                onEditMonth = onEditMonth
            )
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

/**
 * Monthly timeline list + sparkline + inline-edit affordance. Sits below
 * the hero + foreign currencies inside the scrollable Flex content.
 *
 * - Sparkline (if 2+ points): mini Canvas line chart showing the overall
 *   trajectory. Deliberately decorative — no axis labels, the list below
 *   carries the hard numbers.
 * - Rows: one per captured month, newest first. Each row has a pencil
 *   icon that opens an edit dialog so the user can override the stored
 *   value with their own recollection ("my net worth was really 45k that
 *   month, not 42k").
 * - "Estimated" / "Manual" chip on rows marked as such so the user can
 *   tell at-a-glance which numbers came from a reconstruction vs. a live
 *   snapshot vs. their own override.
 */
@Composable
private fun HistorySection(
    history: List<com.andriybobchuk.mooney.mooney.domain.usecase.networth.GetNetWorthHistoryUseCase.Point>,
    baseCurrency: Currency,
    onEditMonth: (LocalDate, Double) -> Unit
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    var editingPoint by remember {
        mutableStateOf<com.andriybobchuk.mooney.mooney.domain.usecase.networth.GetNetWorthHistoryUseCase.Point?>(null)
    }

    // Section title — divider pulled per redesign; the hero's displayLarge
    // already provides enough visual separation above.
    Text(
        text = stringResource(Res.string.flex_monthly_history),
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        ),
        color = onBg.copy(alpha = 0.75f)
    )
    Spacer(Modifier.height(16.dp))

    if (history.isEmpty()) {
        Text(
            text = stringResource(Res.string.flex_history_empty),
            style = MaterialTheme.typography.bodySmall,
            color = onBg.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        return
    }

    // Horizontally-scrollable bar chart. Each bar is tappable → opens the
    // edit dialog for that month (replaces the old long amount list that
    // sat below the chart). Six bars fit on screen; the rest scroll into
    // view. Month at the bottom, value on top of the bar.
    NetWorthGrowthChart(
        points = history,
        onBarClick = { editingPoint = it },
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .padding(vertical = 6.dp)
    )
    @Suppress("UNUSED_VARIABLE") val _bc = baseCurrency

    val target = editingPoint
    if (target != null) {
        EditMonthDialog(
            point = target,
            baseCurrency = baseCurrency,
            onDismiss = { editingPoint = null },
            onConfirm = { newValue ->
                onEditMonth(target.date, newValue)
                editingPoint = null
            }
        )
    }
}

/**
 * Horizontally-scrollable monthly net-worth bar chart.
 *
 * - **Fixed slot width per bar** (screen width / 6 at render time) so six
 *   bars fit on-screen with the rest scrolling into view. More months
 *   than that → the user scrolls, which is the whole point of ditching
 *   the long amount list underneath.
 * - **Baseline anchored at `min * 0.9`** so a 48k→50k→52k sequence doesn't
 *   render as three identical full-height bars (that was the "cumulative
 *   function is not that visible" bug).
 * - **Taps open the edit dialog** for that month. This replaces the old
 *   pencil-per-row affordance; the chart is now both the data display
 *   AND the editing surface.
 * - Amount sits on top of each bar, month label underneath. The newest
 *   bar renders at full opacity + bold label so "right now" reads first.
 */
@Composable
private fun NetWorthGrowthChart(
    points: List<com.andriybobchuk.mooney.mooney.domain.usecase.networth.GetNetWorthHistoryUseCase.Point>,
    onBarClick: (com.andriybobchuk.mooney.mooney.domain.usecase.networth.GetNetWorthHistoryUseCase.Point) -> Unit,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val scrollState = rememberScrollState()
    val values = points.map { it.netWorthInBase }
    val minV = values.min()
    val maxV = values.max()
    val baseline = (minV - (maxV - minV).coerceAtLeast(1.0) * 0.1).coerceAtLeast(0.0)
    val range = (maxV - baseline).coerceAtLeast(1.0)
    val latestIndex = points.lastIndex

    LaunchedEffect(points.size) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    // Mirrors the expenses breakdown chart's layout: Row with
    // verticalAlignment = Bottom, each column arrangement Bottom with a
    // fixed-Dp bar height. Bars grow UP from a shared baseline — no more
    // Column.fillMaxHeight / fillMaxHeight(fraction) which created the
    // "centered triangle" layout and clipped labels at the top for the
    // tallest bars.
    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        points.forEachIndexed { index, point ->
            val norm = ((point.netWorthInBase - baseline) / range).toFloat().coerceIn(0f, 1f)
            val isLatest = index == latestIndex
            val targetBarHeight = (norm * 84f).dp.coerceAtLeast(4.dp)
            val barHeight by animateDpAsState(
                targetValue = targetBarHeight,
                animationSpec = tween(durationMillis = 300),
                label = "netWorthBarHeight"
            )
            Column(
                modifier = Modifier
                    .width(50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onBarClick(point) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = point.netWorthInBase.toCompactString(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isLatest) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = if (isLatest) 1f else 0.6f
                            ),
                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                        )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = shortMonthLabel(point.date),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isLatest) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isLatest) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/** Mini helper — "Oct" from a LocalDate without pulling in a locale lib. */
private fun shortMonthLabel(date: LocalDate): String =
    date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }

/** Compact money format: 12.3k / 1.4M / 320. Used for the delta callout
 *  so a +$2,340 doesn't crowd the chart. */
private fun Double.toCompactString(): String {
    val abs = kotlin.math.abs(this)
    return when {
        abs >= 1_000_000 -> "${(this / 1_000_000).formatWithOneDecimal()}M"
        abs >= 1_000 -> "${(this / 1_000).formatWithOneDecimal()}k"
        else -> kotlin.math.round(this).toInt().toString()
    }
}

private fun Double.formatWithOneDecimal(): String {
    val rounded = kotlin.math.round(this * 10) / 10
    val whole = rounded.toInt()
    val tenth = kotlin.math.round(kotlin.math.abs(rounded - whole) * 10).toInt()
    return if (tenth == 0) whole.toString() else "$whole.$tenth"
}

/** Width/height-fraction modifier isn't in the public Compose API as a
 *  simple extension — this wraps the common idiom (`fillMaxHeight(fraction)`
 *  but the fraction is live). */
@Composable
private fun Modifier.fillMaxHeightFraction(fraction: Float): Modifier =
    this.then(Modifier.fillMaxHeight(fraction.coerceIn(0f, 1f)))

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditMonthDialog(
    point: com.andriybobchuk.mooney.mooney.domain.usecase.networth.GetNetWorthHistoryUseCase.Point,
    baseCurrency: Currency,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var input by remember(point) {
        mutableStateOf(point.netWorthInBase.formatWithCommas())
    }
    val parsed = input
        .replace(" ", "")
        .replace(",", ".")
        .toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(formatMonthYear(point.date)) },
        text = {
            Column {
                Text(
                    text = "Enter net worth at end of ${formatMonthYear(point.date)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { raw ->
                        input = raw.filter { it.isDigit() || it == '.' || it == ',' || it == ' ' }
                    },
                    label = { Text("Net worth (${baseCurrency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = parsed != null && parsed != point.netWorthInBase
            ) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        }
    )
}

/** "October 2026" formatter — kept local so we don't drag in a locale lib
 *  for a single use site. English month names are fine for a shareable
 *  flex surface. */
private fun formatMonthYear(date: LocalDate): String {
    val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$month ${date.year}"
}
