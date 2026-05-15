package com.andriybobchuk.mooney.mooney.domain

fun Double.formatWithCommas(): String {
    val rounded = (this * 100).toLong()
    val integerPart = (rounded / 100).toString()
    val decimalPart = (rounded % 100).toString().padStart(2, '0')

    val withCommas = integerPart
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()

    return "$withCommas.$decimalPart"
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
