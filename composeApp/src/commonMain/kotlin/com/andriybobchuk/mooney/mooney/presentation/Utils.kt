package com.andriybobchuk.mooney.mooney.presentation

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
