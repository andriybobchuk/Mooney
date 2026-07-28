package com.andriybobchuk.mooney.core.widgets

import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.cache.AppDataSnapshot
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Turns the app's live domain snapshot into the flat `WidgetDataSnapshot`
 * that widget hosts read. Pure function of the inputs — no side effects, no
 * suspension. The writer decides when to run it and where to persist the
 * result.
 *
 * Behaves as a use case (single `invoke`) but lives outside the `usecase`
 * package because it's widget-tier plumbing rather than a user-facing action.
 */
class BuildWidgetSnapshotUseCase {

    /**
     * @param app the live app snapshot (accounts / transactions / categories)
     * @param rates current exchange rates in the app's base currency
     * @param baseCurrency user's chosen base currency (for symbol + code)
     * @param nowMs current wall-clock for `lastUpdatedMs` — injected so tests
     *   can pin time and so the test double lines up with the local `today`
     * @param generation the monotonic counter kept by the caller; we don't
     *   own that counter because it needs to survive process restarts
     */
    @Suppress("LongMethod")
    operator fun invoke(
        app: AppDataSnapshot,
        rates: ExchangeRates,
        baseCurrency: Currency,
        nowMs: Long = Clock.System.now().toEpochMilliseconds(),
        generation: Long,
        today: LocalDate = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
    ): WidgetDataSnapshot {
        // Net worth — sum of `includeInNetWorth` accounts, converted.
        val netWorthBase = app.accounts
            .filter { it.includeInNetWorth }
            .sumOf { acc ->
                val signed = if (acc.isLiability) -acc.amount else acc.amount
                rates.convert(signed, acc.currency, baseCurrency)
            }

        // Today's spending — same-day expenses only, converted, then
        // absolute-valued because expenses are stored as positive amounts on
        // an EXPENSE category.
        val todayTxns = app.transactions.filter { it.date == today }
        val todaySpendingBase = todayTxns
            .filter { it.subcategory.type == CategoryType.EXPENSE }
            .sumOf { tx -> rates.convert(tx.amount, tx.account.currency, baseCurrency) }

        // Top category today — group by ROOT category so "Costco" and "Milk"
        // both roll up to "Food". Users think in top-level categories on
        // small surfaces like widgets.
        val todayTopCategory: WidgetCategoryLine? = todayTxns
            .filter { it.subcategory.type == CategoryType.EXPENSE }
            .groupBy { it.subcategory.getRoot() }
            .mapValues { (_, txs) ->
                txs.sumOf { rates.convert(it.amount, it.account.currency, baseCurrency) }
            }
            .maxByOrNull { it.value }
            ?.let { (cat, amount) ->
                WidgetCategoryLine(
                    id = cat.id,
                    title = cat.title,
                    emoji = cat.resolveEmoji(),
                    amountBase = amount
                )
            }

        // This-month running totals.
        val currentMonth = MonthKey.current()
        val monthTxns = app.transactions.filter {
            MonthKey(it.date.year, it.date.monthNumber) == currentMonth
        }
        val monthSpendingBase = monthTxns
            .filter { it.subcategory.type == CategoryType.EXPENSE }
            .sumOf { tx -> rates.convert(tx.amount, tx.account.currency, baseCurrency) }
        val monthIncomeBase = monthTxns
            .filter { it.subcategory.type == CategoryType.INCOME }
            .sumOf { tx -> rates.convert(tx.amount, tx.account.currency, baseCurrency) }

        // Budget progress — only categories with a monthlyLimit, ranked by
        // how close (or how far past) they are.
        val budgetLines = buildBudgetLines(app.transactions, monthTxns, rates, baseCurrency)

        // Streak.
        val streak = StreakCalculator.calculate(app.transactions, today)

        // Mooley mood — over-budget beats worried beats streak.
        val mood = deriveMood(budgetLines, streak)

        return WidgetDataSnapshot(
            netWorthBase = netWorthBase,
            baseCurrencySymbol = baseCurrency.symbol,
            baseCurrencyCode = baseCurrency.name,
            todaySpendingBase = todaySpendingBase,
            todayTopCategory = todayTopCategory,
            monthSpendingBase = monthSpendingBase,
            monthIncomeBase = monthIncomeBase,
            budgetLines = budgetLines,
            streakDays = streak,
            mooleyMood = mood,
            lastUpdatedMs = nowMs,
            updateGeneration = generation
        )
    }

    /**
     * Groups this-month transactions by ROOT category (subcategory rolls up
     * to parent — Food+Restaurants both counted against "Food" budget) and
     * keeps only categories with a `monthlyLimit`. Sorted by highest %
     * consumed first, so the most attention-worthy line always renders.
     */
    private fun buildBudgetLines(
        allTxns: List<Transaction>,
        monthTxns: List<Transaction>,
        rates: ExchangeRates,
        baseCurrency: Currency
    ): List<WidgetBudgetLine> {
        // Discover every root category that has a monthlyLimit anywhere in
        // its subtree — categories are trees, but the limit lives on the
        // root (or on the leaf itself if it's a top-level category).
        val budgetedRoots: Set<Category> = buildSet {
            allTxns.forEach { tx ->
                val root = tx.subcategory.getRoot()
                if (root.monthlyLimit != null) add(root)
                val direct = tx.subcategory
                if (direct.monthlyLimit != null && direct.getRoot() != root) add(direct)
            }
        }
        if (budgetedRoots.isEmpty()) return emptyList()

        return budgetedRoots
            .map { root ->
                val spent = monthTxns
                    .filter {
                        it.subcategory.type == CategoryType.EXPENSE &&
                            (it.subcategory == root || it.subcategory.getRoot() == root)
                    }
                    .sumOf { tx -> rates.convert(tx.amount, tx.account.currency, baseCurrency) }
                WidgetBudgetLine(
                    id = root.id,
                    title = root.title,
                    emoji = root.resolveEmoji(),
                    spentBase = spent,
                    limitBase = root.monthlyLimit ?: 0.0
                )
            }
            .sortedByDescending { it.progress }
            .take(MAX_BUDGET_LINES)
    }

    private fun deriveMood(
        budgetLines: List<WidgetBudgetLine>,
        streak: Int
    ): WidgetMooleyMood {
        // Over-budget wins over every other signal — it's the most
        // actionable state and the reason the character exists.
        if (budgetLines.any { it.isOverBudget }) return WidgetMooleyMood.OVER_BUDGET
        if (budgetLines.any { it.progress >= WORRIED_THRESHOLD }) return WidgetMooleyMood.WORRIED
        if (streak >= HAPPY_STREAK_THRESHOLD) return WidgetMooleyMood.HAPPY
        return WidgetMooleyMood.NEUTRAL
    }

    companion object {
        const val MAX_BUDGET_LINES: Int = 3
        const val WORRIED_THRESHOLD: Float = 0.80f
        const val HAPPY_STREAK_THRESHOLD: Int = 3
    }
}
