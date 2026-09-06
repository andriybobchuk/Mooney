package com.andriybobchuk.mooney.core.platform

import com.andriybobchuk.mooney.di.initKoin
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository
import com.andriybobchuk.mooney.mooney.domain.usecase.AddTransactionUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateMonthlyAnalyticsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateNetWorthUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.CurrencyManagerUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.mp.KoinPlatformTools

/**
 * Entry point for platform-native "add transaction" surfaces — iOS App Intents
 * / Siri Shortcuts today, Android quick-add tiles tomorrow. Isolates the
 * "parse loose strings → resolve to real domain objects → validate → persist"
 * logic in one place so Swift doesn't have to touch any Kotlin business logic
 * beyond a single suspend call.
 *
 * Every field except `amount` is optional and resolves to a sensible default
 * so a one-parameter Shortcut ("Add expense 12.50") does the right thing.
 */
class TransactionIntentHandler(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val repository: CoreRepository,
    private val preferencesRepository: PreferencesRepository,
    private val calculateMonthlyAnalyticsUseCase: CalculateMonthlyAnalyticsUseCase,
    private val calculateNetWorthUseCase: CalculateNetWorthUseCase,
    private val currencyManagerUseCase: CurrencyManagerUseCase
) {

    // ---- read-only balance queries (Get Balance / Get Spent This Month intents) ----

    /**
     * Total expenses for the current calendar month, converted to the user's
     * base currency. Returns a pre-formatted string ready for Siri to read
     * back ("You've spent 1,234 zł this month").
     */
    suspend fun getSpentThisMonth(): BalanceIntentResult {
        val today = kotlinx.datetime.Clock.System.now()
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        val start = kotlinx.datetime.LocalDate(today.year, today.monthNumber, 1)
        val end = if (today.monthNumber == 12) {
            kotlinx.datetime.LocalDate(today.year + 1, 1, 1)
        } else {
            kotlinx.datetime.LocalDate(today.year, today.monthNumber + 1, 1)
        }
        val baseCurrency = GlobalConfig.baseCurrency
        return try {
            val result = calculateMonthlyAnalyticsUseCase(start, end, baseCurrency)
            BalanceIntentResult.success(
                amount = result.totalExpenses,
                formatted = "${result.totalExpenses.formatWithCommas()} ${baseCurrency.symbol}"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            BalanceIntentResult.error("Couldn't compute this month's total: ${e.message ?: "unknown error"}")
        }
    }

    /**
     * Total income for the current calendar month, base-currency-converted.
     */
    suspend fun getIncomeThisMonth(): BalanceIntentResult {
        val today = kotlinx.datetime.Clock.System.now()
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        val start = kotlinx.datetime.LocalDate(today.year, today.monthNumber, 1)
        val end = if (today.monthNumber == 12) {
            kotlinx.datetime.LocalDate(today.year + 1, 1, 1)
        } else {
            kotlinx.datetime.LocalDate(today.year, today.monthNumber + 1, 1)
        }
        val baseCurrency = GlobalConfig.baseCurrency
        return try {
            val result = calculateMonthlyAnalyticsUseCase(start, end, baseCurrency)
            BalanceIntentResult.success(
                amount = result.totalRevenue,
                formatted = "${result.totalRevenue.formatWithCommas()} ${baseCurrency.symbol}"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            BalanceIntentResult.error("Couldn't compute this month's income: ${e.message ?: "unknown error"}")
        }
    }

    /**
     * Current net worth = sum of all account balances (assets − liabilities),
     * converted to base currency via live exchange rates.
     */
    suspend fun getNetWorth(): BalanceIntentResult {
        val baseCurrency = GlobalConfig.baseCurrency
        return try {
            val accounts = repository.getAllAccounts().first().filterNotNull()
            val result = calculateNetWorthUseCase(
                accounts = accounts,
                selectedCurrency = baseCurrency,
                baseCurrency = baseCurrency
            )
            BalanceIntentResult.success(
                amount = result.totalNetWorth,
                formatted = "${result.totalNetWorth.formatWithCommas()} ${result.currency.symbol}"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            BalanceIntentResult.error("Couldn't compute net worth: ${e.message ?: "unknown error"}")
        }
    }

    /**
     * Balance of a single account. If [accountTitle] is null/blank we return
     * the primary account (or first account) so Siri "What's my balance?"
     * always has a sensible default.
     */
    suspend fun getAccountBalance(accountTitle: String?): BalanceIntentResult {
        val allAccounts = try {
            repository.getAllAccounts().first().filterNotNull()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return BalanceIntentResult.error("Couldn't read accounts: ${e.message ?: "unknown error"}")
        }
        if (allAccounts.isEmpty()) {
            return BalanceIntentResult.error("No accounts in Mooney yet.")
        }
        val account = if (accountTitle.isNullOrBlank()) {
            repository.getPrimaryAccount() ?: allAccounts.first()
        } else {
            val needle = accountTitle.trim()
            allAccounts.firstOrNull { it.title.equals(needle, ignoreCase = true) }
                ?: allAccounts.firstOrNull { it.title.contains(needle, ignoreCase = true) }
                ?: return BalanceIntentResult.error("No account named '$needle'.")
        }
        return BalanceIntentResult.success(
            amount = account.amount,
            formatted = "${account.title}: ${account.amount.formatWithCommas()} ${account.currency.symbol}"
        )
    }

    @Suppress("ReturnCount", "LongMethod")
    suspend fun addTransaction(
        amount: Double,
        typeRaw: String,
        categoryId: String?,
        accountTitle: String?,
        description: String?,
        isoDate: String?
    ): TransactionIntentResult {
        if (amount <= 0.0) {
            return TransactionIntentResult.error("Amount must be greater than zero.")
        }

        val type = try {
            CategoryType.valueOf(typeRaw.trim().uppercase())
        } catch (_: IllegalArgumentException) {
            return TransactionIntentResult.error("Unknown transaction type: $typeRaw")
        }
        if (type == CategoryType.TRANSFER) {
            return TransactionIntentResult.error(
                "Transfers can't be added via Shortcuts — open the app."
            )
        }

        val category = resolveCategory(categoryId, type)
            ?: return TransactionIntentResult.error(
                "No matching ${type.name.lowercase()} category" +
                    (categoryId?.let { " for '$it'" } ?: "") + "."
            )
        if (category.type != type) {
            return TransactionIntentResult.error(
                "Category '${category.title}' is a ${category.type.name.lowercase()}, " +
                    "not a ${type.name.lowercase()}."
            )
        }

        val account = resolveAccount(accountTitle)
            ?: return TransactionIntentResult.error(
                "No accounts in Mooney — add one before using Shortcuts."
            )

        val date = try {
            resolveDate(isoDate)
        } catch (_: IllegalArgumentException) {
            return TransactionIntentResult.error(
                "Invalid date: $isoDate (expected yyyy-MM-dd)."
            )
        }

        val tx = Transaction(
            id = 0,
            subcategory = category,
            amount = amount,
            account = account,
            date = date,
            description = description?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.replaceFirstChar { it.uppercase() }
        )

        return try {
            addTransactionUseCase(tx)
            TransactionIntentResult.success(
                "Added ${amount.formatWithCommas()} ${account.currency.symbol} — ${category.title}"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TransactionIntentResult.error(
                "Couldn't save transaction: ${e.message ?: "unknown error"}"
            )
        }
    }

    private suspend fun resolveCategory(categoryId: String?, type: CategoryType): Category? {
        // Explicit ID wins. Otherwise fall back to the user's default for the
        // requested transaction type, which is what the in-app Add sheet
        // pre-selects too — Shortcut behavior matches app behavior.
        val id = categoryId?.trim()?.takeIf { it.isNotEmpty() }
            ?: preferencesRepository.getCurrentPreferences().let { prefs ->
                if (type == CategoryType.EXPENSE) prefs.defaultExpenseCategory
                else prefs.defaultIncomeCategory
            }
        return repository.getCategoryById(id)
    }

    private suspend fun resolveAccount(
        accountTitle: String?
    ): com.andriybobchuk.mooney.mooney.domain.Account? {
        val allAccounts = repository.getAllAccounts().first().filterNotNull()
        if (allAccounts.isEmpty()) return null
        if (accountTitle.isNullOrBlank()) {
            // Prefer explicit primary; otherwise first-added account so the
            // Shortcut doesn't silently fail on new installs that haven't
            // set a primary yet.
            return repository.getPrimaryAccount() ?: allAccounts.first()
        }
        val needle = accountTitle.trim()
        return allAccounts.firstOrNull { it.title.equals(needle, ignoreCase = true) }
            ?: allAccounts.firstOrNull { it.title.contains(needle, ignoreCase = true) }
    }

    private fun resolveDate(isoDate: String?): LocalDate {
        if (isoDate.isNullOrBlank()) {
            return Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
        }
        return LocalDate.parse(isoDate.trim())
    }
}

data class TransactionIntentResult(
    val isSuccess: Boolean,
    val message: String
) {
    companion object {
        fun success(message: String) = TransactionIntentResult(true, message)
        fun error(message: String) = TransactionIntentResult(false, message)
    }
}

/**
 * Result type for read-only balance queries — amount is the raw number
 * (Siri chains can compare / sum this), formatted is the human string.
 */
data class BalanceIntentResult(
    val isSuccess: Boolean,
    val amount: Double,
    val formatted: String,
    val errorMessage: String
) {
    companion object {
        fun success(amount: Double, formatted: String) =
            BalanceIntentResult(true, amount, formatted, "")
        fun error(message: String) =
            BalanceIntentResult(false, 0.0, "", message)
    }
}

/**
 * Top-level Swift-callable resolver.
 *
 * iOS AppIntents run in an unusual lifecycle — the process may be launched by
 * the intent itself, WITHOUT ever mounting the SwiftUI scene (and therefore
 * without calling MainViewController → initKoin). We bootstrap Koin lazily
 * here so the intent can succeed in cold-start scenarios.
 *
 * Idempotent: `initKoin()` is guarded by GlobalContext.getOrNull() so it's
 * a no-op when Koin is already up (the normal warm case).
 */
fun resolveTransactionIntentHandler(): TransactionIntentHandler {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        initKoin()
    }
    return KoinPlatformTools.defaultContext().get().get()
}
