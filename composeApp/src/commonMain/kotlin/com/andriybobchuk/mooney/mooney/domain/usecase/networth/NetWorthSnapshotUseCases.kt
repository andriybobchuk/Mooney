package com.andriybobchuk.mooney.mooney.domain.usecase.networth

import com.andriybobchuk.mooney.core.data.database.NetWorthSnapshotDao
import com.andriybobchuk.mooney.core.data.database.NetWorthSnapshotEntity
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.usecase.CurrencyManagerUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * Captures the net worth of EVERY account at a given date, using the
 * supplied per-account amounts (so the caller can pass either CURRENT
 * balances for a live snapshot OR historically-reconstructed balances for
 * a backfill).
 *
 * Writes one row per account via a single Upsert — the unique
 * (accountId, date) index on the table guarantees idempotency: if a
 * snapshot for that pair exists, it's overwritten with the fresh numbers.
 */
class WriteNetWorthSnapshotUseCase(
    private val snapshotDao: NetWorthSnapshotDao,
    private val currencyManagerUseCase: CurrencyManagerUseCase
) {
    suspend operator fun invoke(
        snapshotDate: LocalDate,
        amountsByAccount: Map<Account, Double>,
        isReconstructed: Boolean
    ) {
        if (amountsByAccount.isEmpty()) return
        val baseCurrency = GlobalConfig.baseCurrency
        val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
        val now = Clock.System.now().toString()
        val rows = amountsByAccount.map { (account, amount) ->
            val baseAmount = if (account.currency != baseCurrency) {
                exchangeRates.convert(amount, account.currency, baseCurrency)
            } else {
                amount
            }
            val signedBase = if (account.isLiability) -baseAmount else baseAmount
            NetWorthSnapshotEntity(
                date = snapshotDate.toString(),
                accountId = account.id,
                originalCurrency = account.currency.name,
                originalAmount = amount,
                baseCurrency = baseCurrency.name,
                baseAmount = signedBase,
                isReconstructed = isReconstructed,
                createdAt = now
            )
        }
        snapshotDao.upsertAll(rows)
    }
}

/**
 * Idempotent catch-up job for net-worth snapshots. Call on app open (or
 * any time the user visits a net-worth-adjacent surface). Walks every
 * month-end strictly in the past that doesn't already have a snapshot
 * and writes one, using backward-reconstructed balances for accuracy.
 *
 * Scope limits:
 * - Looks back [MAX_BACKFILL_MONTHS] at most — avoids writing a dozen
 *   years of made-up snapshots the first time a long-time user opens the
 *   app after this feature ships.
 * - Only writes COMPLETED months (never the current in-progress month).
 *
 * Reconstruction math: `historical_balance = current_balance - net_delta`
 * where `net_delta` is the signed sum of transactions dated STRICTLY
 * AFTER `snapshotDate`. For transfers, both legs are undone from the
 * respective accounts. Market-value assets (vehicle, real estate) use
 * their CURRENT amount for all historical months — we have no price
 * history to reconstruct from, and this is an approximation flagged via
 * `isReconstructed = true` so the UI can label it as estimated.
 */
class CatchUpNetWorthSnapshotsUseCase(
    private val repository: CoreRepository,
    private val snapshotDao: NetWorthSnapshotDao,
    private val writeSnapshot: WriteNetWorthSnapshotUseCase
) {
    companion object {
        /** How far back the first-ever run reconstructs. 12 months is enough
         *  for a year-over-year chart; more than that is cosmetic. */
        const val MAX_BACKFILL_MONTHS = 12
    }

    suspend operator fun invoke() {
        try {
            val accounts = repository.getAllAccounts().first().filterNotNull()
            if (accounts.isEmpty()) return
            val allTxs = repository.getAllTransactions().first().filterNotNull()

            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            val targetMonthEnds = pastMonthEnds(today, MAX_BACKFILL_MONTHS)
            if (targetMonthEnds.isEmpty()) return

            val latestExisting = snapshotDao.getLatestSnapshotDate()?.let { raw ->
                runCatching { LocalDate.parse(raw) }.getOrNull()
            }
            // Only process month-ends we haven't captured yet. The latest
            // existing snapshot acts as a watermark: anything on or before
            // that date is skipped.
            val toProcess = targetMonthEnds.filter { monthEnd ->
                latestExisting == null || monthEnd > latestExisting
            }
            for (monthEnd in toProcess) {
                val amounts = reconstructBalancesAt(monthEnd, accounts, allTxs)
                writeSnapshot(
                    snapshotDate = monthEnd,
                    amountsByAccount = amounts,
                    isReconstructed = true
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Snapshot catch-up is best-effort; swallowing here is deliberate
            // so a transient failure (e.g. DAO contention during migration)
            // doesn't crash app launch. The next catch-up run will retry.
        }
    }

    /**
     * Returns all month-end dates strictly BEFORE [today], up to [maxMonths]
     * back, oldest first.
     */
    private fun pastMonthEnds(today: LocalDate, maxMonths: Int): List<LocalDate> {
        val out = mutableListOf<LocalDate>()
        // Start from the END of the month BEFORE the current one.
        var cursorYear = today.year
        var cursorMonth = today.monthNumber
        for (i in 0 until maxMonths) {
            cursorMonth -= 1
            if (cursorMonth < 1) {
                cursorMonth = 12
                cursorYear -= 1
            }
            out += lastDayOfMonth(cursorYear, cursorMonth)
        }
        return out.reversed() // oldest first
    }

    private fun lastDayOfMonth(year: Int, month: Int): LocalDate {
        // Compute by taking the 1st of NEXT month and stepping one day back.
        val (nextYear, nextMonth) = if (month == 12) (year + 1) to 1 else year to (month + 1)
        val firstOfNext = LocalDate(nextYear, nextMonth, 1)
        return firstOfNext.minus(1, kotlinx.datetime.DateTimeUnit.DAY)
    }

    /**
     * For each account, compute what its balance WAS on [snapshotDate]
     * by undoing every transaction dated strictly after that date.
     */
    private fun reconstructBalancesAt(
        snapshotDate: LocalDate,
        accounts: List<Account>,
        allTxs: List<Transaction>
    ): Map<Account, Double> {
        val txsAfter = allTxs.filter { tx ->
            runCatching { tx.date > snapshotDate }.getOrDefault(false)
        }
        val deltaByAccountId = accounts.associate { it.id to 0.0 }.toMutableMap()

        txsAfter.forEach { tx ->
            val sourceId = tx.account.id
            when (tx.subcategory.type) {
                CategoryType.EXPENSE -> {
                    // current = historical - amount → historical = current + amount
                    deltaByAccountId[sourceId] =
                        (deltaByAccountId[sourceId] ?: 0.0) + tx.amount
                }
                CategoryType.INCOME -> {
                    // current = historical + amount → historical = current - amount
                    deltaByAccountId[sourceId] =
                        (deltaByAccountId[sourceId] ?: 0.0) - tx.amount
                }
                CategoryType.TRANSFER -> {
                    // Outgoing leg: source balance went DOWN by amount.
                    deltaByAccountId[sourceId] =
                        (deltaByAccountId[sourceId] ?: 0.0) + tx.amount
                    // Incoming leg: destination went UP by destinationAmount
                    // (cross-currency) or amount (same-currency).
                    val destId = tx.subcategory.id
                        .removePrefix("transfer_to_").toIntOrNull()
                    if (destId != null && destId in deltaByAccountId) {
                        val creditAmount = tx.destinationAmount ?: tx.amount
                        deltaByAccountId[destId] =
                            (deltaByAccountId[destId] ?: 0.0) - creditAmount
                    }
                }
            }
        }

        return accounts.associateWith { account ->
            account.amount + (deltaByAccountId[account.id] ?: 0.0)
        }
    }
}

/**
 * Read-only query — exposes the full snapshot history for the net-worth
 * timeline UI. One point per month-end date.
 *
 * Convention: a row with `accountId = 0` is a USER-ENTERED manual override.
 * Room's autoGenerate starts at 1 so this id never collides with real
 * accounts. When a manual row exists for a date, it OVERRIDES any
 * per-account snapshots we computed for that same date — the user knows
 * better than our backward-reconstruction heuristic.
 */
class GetNetWorthHistoryUseCase(
    private val snapshotDao: NetWorthSnapshotDao
) {
    data class Point(
        val date: LocalDate,
        val netWorthInBase: Double,
        /** True = backward-reconstructed from current balances; false =
         *  live-captured OR user-entered manual override. */
        val isReconstructed: Boolean,
        /** True = user manually typed this value in the Flex screen. */
        val isManual: Boolean
    )

    suspend operator fun invoke(): List<Point> {
        val rows = snapshotDao.observeAll().first()
        if (rows.isEmpty()) return emptyList()
        return rows
            .groupBy { it.date }
            .map { (dateStr, groupRows) ->
                val manual = groupRows.firstOrNull { it.accountId == MANUAL_ACCOUNT_ID }
                if (manual != null) {
                    Point(
                        date = LocalDate.parse(dateStr),
                        netWorthInBase = manual.baseAmount,
                        isReconstructed = false,
                        isManual = true
                    )
                } else {
                    Point(
                        date = LocalDate.parse(dateStr),
                        netWorthInBase = groupRows.sumOf { it.baseAmount },
                        isReconstructed = groupRows.all { it.isReconstructed },
                        isManual = false
                    )
                }
            }
            .sortedBy { it.date }
    }
}

/**
 * User-facing write: "my net worth at end of month X was Y". The Flex
 * screen's inline-edit affordance calls this. Upserts a single row with
 * `accountId = MANUAL_ACCOUNT_ID`, which [GetNetWorthHistoryUseCase]
 * treats as the authoritative value for that date — ignoring any
 * per-account snapshots for the same date.
 */
class SetManualNetWorthUseCase(
    private val snapshotDao: NetWorthSnapshotDao
) {
    suspend operator fun invoke(monthEnd: LocalDate, netWorthInBase: Double) {
        val baseCurrency = GlobalConfig.baseCurrency
        snapshotDao.upsertAll(
            listOf(
                NetWorthSnapshotEntity(
                    date = monthEnd.toString(),
                    accountId = MANUAL_ACCOUNT_ID,
                    originalCurrency = baseCurrency.name,
                    originalAmount = netWorthInBase,
                    baseCurrency = baseCurrency.name,
                    baseAmount = netWorthInBase,
                    isReconstructed = false,
                    createdAt = Clock.System.now().toString()
                )
            )
        )
    }
}

/** Sentinel accountId for user-entered manual-override snapshot rows.
 *  Zero is never a real account id (Room autoGenerate starts at 1). */
const val MANUAL_ACCOUNT_ID: Int = 0
