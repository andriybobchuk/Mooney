package com.andriybobchuk.mooney.mooney.presentation.assets

import com.andriybobchuk.mooney.mooney.domain.Currency

/**
 * Country-specific adult net-worth percentile thresholds, expressed in each
 * country's local currency. Used by the Net Worth flex sheet to render a
 * "Top X% in <Country>" label.
 *
 * Estimates blend 2024 Credit Suisse / UBS Global Wealth Report data with
 * 2025–2026 GDP-per-capita / inflation adjustments. They're rough — meant
 * for a fun social flex, not financial advice.
 *
 * Anchor points are the bottom of each tier:
 *   p10  → 10th percentile of net worth (bottom 10%)
 *   p25  → 25th percentile
 *   p50  → median
 *   p75, p90, p95, p99, p999
 *
 * A value below p10 yields a percentile linearly interpolated from 0..p10.
 * A value above p999 caps at 99.9.
 */
internal data class CountryWealthProfile(
    val country: String,
    val flag: String,
    val p10: Double,
    val p25: Double,
    val p50: Double,
    val p75: Double,
    val p90: Double,
    val p95: Double,
    val p99: Double,
    val p999: Double
) {
    fun thresholds(): List<Pair<Double, Double>> = listOf(
        10.0 to p10,
        25.0 to p25,
        50.0 to p50,
        75.0 to p75,
        90.0 to p90,
        95.0 to p95,
        99.0 to p99,
        99.9 to p999
    )
}

/**
 * Maps the user's base currency to a country profile. We don't ask the user
 * for their country explicitly — the base currency is a good-enough proxy
 * because most people pick the currency of the country they live in.
 */
internal val WealthPercentiles: Map<Currency, CountryWealthProfile> = mapOf(
    Currency.PLN to CountryWealthProfile(
        country = "Poland", flag = "🇵🇱",
        p10 = 5_000.0, p25 = 30_000.0, p50 = 150_000.0,
        p75 = 400_000.0, p90 = 900_000.0, p95 = 1_500_000.0,
        p99 = 5_000_000.0, p999 = 20_000_000.0
    ),
    Currency.USD to CountryWealthProfile(
        country = "the US", flag = "🇺🇸",
        p10 = 5_000.0, p25 = 30_000.0, p50 = 200_000.0,
        p75 = 700_000.0, p90 = 1_800_000.0, p95 = 3_500_000.0,
        p99 = 15_000_000.0, p999 = 50_000_000.0
    ),
    Currency.EUR to CountryWealthProfile(
        country = "the Eurozone", flag = "🇪🇺",
        p10 = 3_000.0, p25 = 15_000.0, p50 = 90_000.0,
        p75 = 300_000.0, p90 = 750_000.0, p95 = 1_400_000.0,
        p99 = 5_000_000.0, p999 = 20_000_000.0
    ),
    Currency.UAH to CountryWealthProfile(
        country = "Ukraine", flag = "🇺🇦",
        p10 = 10_000.0, p25 = 50_000.0, p50 = 250_000.0,
        p75 = 800_000.0, p90 = 2_500_000.0, p95 = 5_000_000.0,
        p99 = 20_000_000.0, p999 = 80_000_000.0
    ),
    Currency.GBP to CountryWealthProfile(
        country = "the UK", flag = "🇬🇧",
        p10 = 5_000.0, p25 = 35_000.0, p50 = 200_000.0,
        p75 = 600_000.0, p90 = 1_400_000.0, p95 = 2_500_000.0,
        p99 = 10_000_000.0, p999 = 40_000_000.0
    ),
    Currency.CHF to CountryWealthProfile(
        country = "Switzerland", flag = "🇨🇭",
        p10 = 8_000.0, p25 = 50_000.0, p50 = 280_000.0,
        p75 = 900_000.0, p90 = 2_500_000.0, p95 = 5_000_000.0,
        p99 = 20_000_000.0, p999 = 80_000_000.0
    ),
    Currency.CZK to CountryWealthProfile(
        country = "Czechia", flag = "🇨🇿",
        p10 = 40_000.0, p25 = 250_000.0, p50 = 1_200_000.0,
        p75 = 3_500_000.0, p90 = 8_000_000.0, p95 = 15_000_000.0,
        p99 = 50_000_000.0, p999 = 200_000_000.0
    ),
    Currency.SEK to CountryWealthProfile(
        country = "Sweden", flag = "🇸🇪",
        p10 = 25_000.0, p25 = 150_000.0, p50 = 700_000.0,
        p75 = 2_500_000.0, p90 = 6_000_000.0, p95 = 11_000_000.0,
        p99 = 40_000_000.0, p999 = 150_000_000.0
    ),
    Currency.NOK to CountryWealthProfile(
        country = "Norway", flag = "🇳🇴",
        p10 = 30_000.0, p25 = 200_000.0, p50 = 1_000_000.0,
        p75 = 3_000_000.0, p90 = 7_000_000.0, p95 = 13_000_000.0,
        p99 = 50_000_000.0, p999 = 200_000_000.0
    ),
    Currency.DKK to CountryWealthProfile(
        country = "Denmark", flag = "🇩🇰",
        p10 = 25_000.0, p25 = 150_000.0, p50 = 800_000.0,
        p75 = 2_500_000.0, p90 = 6_000_000.0, p95 = 11_000_000.0,
        p99 = 40_000_000.0, p999 = 150_000_000.0
    ),
    Currency.JPY to CountryWealthProfile(
        country = "Japan", flag = "🇯🇵",
        p10 = 400_000.0, p25 = 2_500_000.0, p50 = 10_000_000.0,
        p75 = 35_000_000.0, p90 = 85_000_000.0, p95 = 150_000_000.0,
        p99 = 500_000_000.0, p999 = 2_000_000_000.0
    ),
    Currency.CAD to CountryWealthProfile(
        country = "Canada", flag = "🇨🇦",
        p10 = 8_000.0, p25 = 50_000.0, p50 = 260_000.0,
        p75 = 700_000.0, p90 = 1_800_000.0, p95 = 3_200_000.0,
        p99 = 12_000_000.0, p999 = 50_000_000.0
    ),
    Currency.AUD to CountryWealthProfile(
        country = "Australia", flag = "🇦🇺",
        p10 = 15_000.0, p25 = 80_000.0, p50 = 380_000.0,
        p75 = 1_000_000.0, p90 = 2_500_000.0, p95 = 4_500_000.0,
        p99 = 15_000_000.0, p999 = 60_000_000.0
    ),
    Currency.TRY to CountryWealthProfile(
        country = "Türkiye", flag = "🇹🇷",
        p10 = 40_000.0, p25 = 250_000.0, p50 = 1_200_000.0,
        p75 = 4_000_000.0, p90 = 10_000_000.0, p95 = 20_000_000.0,
        p99 = 80_000_000.0, p999 = 300_000_000.0
    ),
    Currency.BRL to CountryWealthProfile(
        country = "Brazil", flag = "🇧🇷",
        p10 = 1_500.0, p25 = 8_000.0, p50 = 50_000.0,
        p75 = 180_000.0, p90 = 500_000.0, p95 = 1_200_000.0,
        p99 = 5_000_000.0, p999 = 25_000_000.0
    ),
    Currency.RUB to CountryWealthProfile(
        country = "Russia", flag = "🇷🇺",
        p10 = 50_000.0, p25 = 300_000.0, p50 = 1_500_000.0,
        p75 = 5_000_000.0, p90 = 12_000_000.0, p95 = 25_000_000.0,
        p99 = 100_000_000.0, p999 = 500_000_000.0
    ),
    Currency.AED to CountryWealthProfile(
        country = "the UAE", flag = "🇦🇪",
        p10 = 8_000.0, p25 = 50_000.0, p50 = 250_000.0,
        p75 = 800_000.0, p90 = 2_000_000.0, p95 = 4_000_000.0,
        p99 = 20_000_000.0, p999 = 100_000_000.0
    )
)

internal data class WealthRank(
    val country: String,
    val flag: String,
    /** "Top X" — small X means richer (top 1% is richer than top 50%). */
    val topPercent: Double
)

/**
 * Compute the user's wealth rank vs the country tied to their base currency.
 * Returns null if we don't have data for that currency or net worth is non-positive.
 *
 * Interpolates linearly between anchor brackets, so the result varies smoothly
 * with net worth rather than snapping at thresholds.
 */
@Suppress("ReturnCount")
internal fun computeWealthRank(netWorth: Double, baseCurrency: Currency): WealthRank? {
    if (netWorth <= 0.0) return null
    val profile = WealthPercentiles[baseCurrency] ?: return null
    val anchors = profile.thresholds()  // sorted ascending by threshold value

    // Below the lowest anchor: linearly interpolate from 0..p10
    if (netWorth < anchors.first().second) {
        val (p, threshold) = anchors.first()
        val fraction = netWorth / threshold
        val rank = (fraction * p).coerceIn(0.0, p)
        return WealthRank(profile.country, profile.flag, topPercent = 100.0 - rank)
    }

    // Above the highest anchor
    if (netWorth >= anchors.last().second) {
        return WealthRank(profile.country, profile.flag, topPercent = 100.0 - 99.9)
    }

    // Find the bracket the value falls in and interpolate
    for (i in 0 until anchors.size - 1) {
        val (pLow, vLow) = anchors[i]
        val (pHigh, vHigh) = anchors[i + 1]
        if (netWorth >= vLow && netWorth < vHigh) {
            val t = (netWorth - vLow) / (vHigh - vLow)
            val rank = pLow + t * (pHigh - pLow)
            return WealthRank(profile.country, profile.flag, topPercent = 100.0 - rank)
        }
    }
    return null
}

/**
 * Render the rank as a flex-worthy short string:
 *   topPercent 1   → "Top 1%"
 *   topPercent 5   → "Top 5%"
 *   topPercent 32  → "Top 32%"
 *   topPercent 0.1 → "Top 0.1%"
 */
internal fun WealthRank.formatTopPercent(): String {
    val tp = topPercent
    val rounded = when {
        tp < 1.0 -> {
            val oneDecimal = (tp * 10).toInt() / 10.0
            if (oneDecimal == 0.0) "0.1" else oneDecimal.toString()
        }
        tp < 10.0 -> tp.toInt().coerceAtLeast(1).toString()
        else -> tp.toInt().toString()
    }
    return "Top $rounded%"
}
