package com.andriybobchuk.mooney.mooney.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class MonthKey(val year: Int, val month: Int) {
    fun toDisplayString(): String = "${monthName(month)} $year"

    fun toShortDisplayString(): String = shortMonthName(month)

    fun firstDay(): LocalDate = LocalDate(year, month, 1)

    fun firstDayOfNextMonth(): LocalDate {
        val nextMonth = if (month == 12) 1 else month + 1
        val nextYear = if (month == 12) year + 1 else year
        return LocalDate(nextYear, nextMonth, 1)
    }

    fun previousMonth(): MonthKey {
        val prevMonth = if (month == 1) 12 else month - 1
        val prevYear = if (month == 1) year - 1 else year
        return MonthKey(prevYear, prevMonth)
    }

    fun monthsAgo(count: Int): MonthKey {
        var result = this
        repeat(count) {
            result = result.previousMonth()
        }
        return result
    }

    private fun monthName(m: Int): String = when (m) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Invalid"
    }

    private fun shortMonthName(m: Int): String = when (m) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> "???"
    }

    companion object {
        fun current(): MonthKey {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            return MonthKey(now.year, now.monthNumber)
        }
    }
}
