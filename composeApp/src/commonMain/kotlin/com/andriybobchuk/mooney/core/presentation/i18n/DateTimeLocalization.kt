package com.andriybobchuk.mooney.core.presentation.i18n

import androidx.compose.runtime.Composable
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.month_long_01
import mooney.composeapp.generated.resources.month_long_02
import mooney.composeapp.generated.resources.month_long_03
import mooney.composeapp.generated.resources.month_long_04
import mooney.composeapp.generated.resources.month_long_05
import mooney.composeapp.generated.resources.month_long_06
import mooney.composeapp.generated.resources.month_long_07
import mooney.composeapp.generated.resources.month_long_08
import mooney.composeapp.generated.resources.month_long_09
import mooney.composeapp.generated.resources.month_long_10
import mooney.composeapp.generated.resources.month_long_11
import mooney.composeapp.generated.resources.month_long_12
import mooney.composeapp.generated.resources.month_short_01
import mooney.composeapp.generated.resources.month_short_02
import mooney.composeapp.generated.resources.month_short_03
import mooney.composeapp.generated.resources.month_short_04
import mooney.composeapp.generated.resources.month_short_05
import mooney.composeapp.generated.resources.month_short_06
import mooney.composeapp.generated.resources.month_short_07
import mooney.composeapp.generated.resources.month_short_08
import mooney.composeapp.generated.resources.month_short_09
import mooney.composeapp.generated.resources.month_short_10
import mooney.composeapp.generated.resources.month_short_11
import mooney.composeapp.generated.resources.month_short_12
import mooney.composeapp.generated.resources.weekday_long_1
import mooney.composeapp.generated.resources.weekday_long_2
import mooney.composeapp.generated.resources.weekday_long_3
import mooney.composeapp.generated.resources.weekday_long_4
import mooney.composeapp.generated.resources.weekday_long_5
import mooney.composeapp.generated.resources.weekday_long_6
import mooney.composeapp.generated.resources.weekday_long_7
import mooney.composeapp.generated.resources.weekday_short_1
import mooney.composeapp.generated.resources.weekday_short_2
import mooney.composeapp.generated.resources.weekday_short_3
import mooney.composeapp.generated.resources.weekday_short_4
import mooney.composeapp.generated.resources.weekday_short_5
import mooney.composeapp.generated.resources.weekday_short_6
import mooney.composeapp.generated.resources.weekday_short_7
import org.jetbrains.compose.resources.stringResource

/**
 * Returns the localized month name for the given 1-12 month number.
 * Used everywhere we render a month label in the UI — replaces hardcoded
 * English "Jan"/"January" strings scattered across the codebase.
 */
@Composable
fun localizedMonthName(monthNumber: Int, short: Boolean = false): String {
    val safe = monthNumber.coerceIn(1, 12)
    return if (short) {
        when (safe) {
            1 -> stringResource(Res.string.month_short_01)
            2 -> stringResource(Res.string.month_short_02)
            3 -> stringResource(Res.string.month_short_03)
            4 -> stringResource(Res.string.month_short_04)
            5 -> stringResource(Res.string.month_short_05)
            6 -> stringResource(Res.string.month_short_06)
            7 -> stringResource(Res.string.month_short_07)
            8 -> stringResource(Res.string.month_short_08)
            9 -> stringResource(Res.string.month_short_09)
            10 -> stringResource(Res.string.month_short_10)
            11 -> stringResource(Res.string.month_short_11)
            else -> stringResource(Res.string.month_short_12)
        }
    } else {
        when (safe) {
            1 -> stringResource(Res.string.month_long_01)
            2 -> stringResource(Res.string.month_long_02)
            3 -> stringResource(Res.string.month_long_03)
            4 -> stringResource(Res.string.month_long_04)
            5 -> stringResource(Res.string.month_long_05)
            6 -> stringResource(Res.string.month_long_06)
            7 -> stringResource(Res.string.month_long_07)
            8 -> stringResource(Res.string.month_long_08)
            9 -> stringResource(Res.string.month_long_09)
            10 -> stringResource(Res.string.month_long_10)
            11 -> stringResource(Res.string.month_long_11)
            else -> stringResource(Res.string.month_long_12)
        }
    }
}

/**
 * Returns the localized weekday name. ISO 1=Monday..7=Sunday.
 */
@Composable
fun localizedWeekdayName(weekdayIso: Int, short: Boolean = false): String {
    val safe = weekdayIso.coerceIn(1, 7)
    return if (short) {
        when (safe) {
            1 -> stringResource(Res.string.weekday_short_1)
            2 -> stringResource(Res.string.weekday_short_2)
            3 -> stringResource(Res.string.weekday_short_3)
            4 -> stringResource(Res.string.weekday_short_4)
            5 -> stringResource(Res.string.weekday_short_5)
            6 -> stringResource(Res.string.weekday_short_6)
            else -> stringResource(Res.string.weekday_short_7)
        }
    } else {
        when (safe) {
            1 -> stringResource(Res.string.weekday_long_1)
            2 -> stringResource(Res.string.weekday_long_2)
            3 -> stringResource(Res.string.weekday_long_3)
            4 -> stringResource(Res.string.weekday_long_4)
            5 -> stringResource(Res.string.weekday_long_5)
            6 -> stringResource(Res.string.weekday_long_6)
            else -> stringResource(Res.string.weekday_long_7)
        }
    }
}
