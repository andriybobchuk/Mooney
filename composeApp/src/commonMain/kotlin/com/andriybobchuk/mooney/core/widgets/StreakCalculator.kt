package com.andriybobchuk.mooney.core.widgets

import com.andriybobchuk.mooney.mooney.domain.Transaction
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * Counts consecutive days ending "today" where the user recorded at least one
 * transaction. If the user logs today, the streak starts at 1. Skip a day and
 * it resets to 0 as soon as "today" is no longer covered.
 *
 * We use *any* transaction (including income and transfers) rather than only
 * expenses — the goal is "user engaged with Mooney today," not "user spent
 * money today." Missing an income entry shouldn't kill the streak.
 *
 * Yesterday grace period:
 *  - If the user hasn't logged today (yet) but did log yesterday, the streak
 *    counts back from yesterday. This prevents the anxiety-inducing "your
 *    streak reset at midnight before you had a chance to open the app."
 *    Same behavior every mainstream streak app uses (Duolingo, Snap).
 */
object StreakCalculator {
    fun calculate(
        transactions: List<Transaction>,
        today: LocalDate = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
    ): Int {
        if (transactions.isEmpty()) return 0
        val activeDays = transactions.map { it.date }.toSet()

        // Anchor: today if user logged today, else yesterday (grace window).
        val anchor = when {
            today in activeDays -> today
            today.minus(DatePeriod(days = 1)) in activeDays ->
                today.minus(DatePeriod(days = 1))
            else -> return 0
        }

        // Count backwards from the anchor until we hit a day without a tx.
        var cursor = anchor
        var count = 0
        while (cursor in activeDays) {
            count++
            cursor = cursor.minus(DatePeriod(days = 1))
        }
        return count
    }
}
