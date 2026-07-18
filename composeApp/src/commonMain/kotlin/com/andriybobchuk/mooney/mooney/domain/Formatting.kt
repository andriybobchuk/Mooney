package com.andriybobchuk.mooney.mooney.domain

/**
 * Formats a Double as "1,234.56" with:
 *   - proper rounding to 2 decimals (not truncation — 21.10 stayed 21.09 before
 *     because `(21.10 * 100).toLong()` bit-hits 2109.999… and truncates to 2109),
 *   - a single leading minus for negatives (was rendering "-21.-10" because
 *     both quotient and remainder inherited the sign),
 *   - comma every 3 digits in the integer part.
 */
fun Double.formatWithCommas(): String {
    // Guard against NaN / infinities — they'd blow up the round path below.
    if (!this.isFinite()) return "0.00"
    val negative = this < 0
    val absValue = kotlin.math.abs(this)
    val rounded = kotlin.math.round(absValue * 100).toLong()
    val integerPart = (rounded / 100).toString()
    val decimalPart = (rounded % 100).toString().padStart(2, '0')

    val withCommas = integerPart
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()

    val prefix = if (negative && rounded > 0L) "-" else ""
    return "$prefix$withCommas.$decimalPart"
}

fun Int.formatWithCommas(): String {
    return this.toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
}

fun Double.formatToPlainString(): String {
    val rounded = kotlin.math.round(this * 100) / 100
    val parts = rounded.toString().split(".")
    val integerPart = parts[0]
    val decimalPart = parts.getOrNull(1)?.padEnd(2, '0')?.take(2) ?: "00"
    return "$integerPart.$decimalPart"
}

/**
 * Parses user-entered amount strings tolerantly across locales.
 *
 * iOS / Android keyboards show different decimal separators based on the device
 * region — `.` in US English, `,` in PL/UA/RU/DE/FR/etc. Some users also type
 * thousand separators when copy-pasting. We accept all of:
 *   "1234"      → 1234.0
 *   "1234.56"   → 1234.56
 *   "1234,56"   → 1234.56
 *   "1,234.56"  → 1234.56     (US thousands)
 *   "1.234,56"  → 1234.56     (EU thousands)
 *   "1 234,56"  → 1234.56     (space thousands)
 *   "-42.5"     → -42.5
 *
 * Heuristic: the LAST `.` or `,` in the string is treated as the decimal
 * separator; every other `.` / `,` / whitespace is treated as a grouping
 * separator and stripped. Returns null on anything unparseable.
 */
fun String.parseAmountInput(): Double? {
    val cleaned = this.trim().filterNot { it.isWhitespace() }
    if (cleaned.isEmpty()) return null

    val lastDot = cleaned.lastIndexOf('.')
    val lastComma = cleaned.lastIndexOf(',')
    val decimalIdx = maxOf(lastDot, lastComma)

    val normalized = if (decimalIdx < 0) {
        cleaned  // no decimal separator at all — pure integer
    } else {
        val intPart = cleaned.substring(0, decimalIdx).filter { it != '.' && it != ',' }
        val fracPart = cleaned.substring(decimalIdx + 1)
        if (intPart.isEmpty()) "0.$fracPart" else "$intPart.$fracPart"
    }
    return normalized.toDoubleOrNull()
}

fun Double.formatToShortString(): String {
    val absValue = kotlin.math.abs(this)
    val sign = if (this < 0) "-" else ""

    return when {
        absValue >= 1_000_000 -> {
            val value = (absValue / 1_000_000 * 10).toInt() / 10.0
            "$sign${value}M"
        }
        absValue >= 1_000 -> {
            val value = (absValue / 1_000 * 10).toInt() / 10.0
            "$sign${value}k"
        }
        else -> "$sign${absValue.toInt()}"
    }
}
