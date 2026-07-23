package com.andriybobchuk.mooney.core.analytics

/**
 * The Mooney analytics event catalog.
 *
 * Events here are deliberately narrow — each one answers a specific business
 * question and feeds directly into a funnel chart in the Firebase console.
 * Adding noise events here makes every dashboard worse, so prefer a [Analytics.log]
 * breadcrumb (Crashlytics) for low-signal UI actions.
 *
 * Four funnels:
 *
 *  1. ACTIVATION — does a new install reach first value?
 *      onboarding_complete → first_account_created → first_transaction_created
 *
 *  2. ENGAGEMENT — what proves the app is being used for its purpose?
 *      transaction_added · account_added · recurring_added · goal_added
 *      custom_category_added · historical_month_viewed · csv_exported · csv_imported
 *
 *  3. MONETIZATION — where do users drop off in the upgrade flow?
 *      feature_limit_hit → paywall_viewed → subscribe_tap → subscribe_result
 *      (plus paywall_dismissed and restore_purchases_tap for context)
 *
 *  4. REVIEW & FEEDBACK — is the rating prompt actually generating quality reviews?
 *      review_preprompt_shown → review_preprompt_response → feedback_submitted
 */
sealed interface AnalyticsEvent {
    val name: String
    val params: Map<String, String> get() = emptyMap()

    // ───────────────────────── Activation ─────────────────────────

    /** First-launch onboarding finished. Currency is the base they picked. */
    data class OnboardingComplete(val currency: String) : AnalyticsEvent {
        override val name = "onboarding_complete"
        override val params = mapOf("currency" to currency)
    }

    /** Fires once ever, the first time the user creates an account. */
    data object FirstAccountCreated : AnalyticsEvent {
        override val name = "first_account_created"
    }

    /** Fires once ever, the first time the user creates a transaction. */
    data object FirstTransactionCreated : AnalyticsEvent {
        override val name = "first_transaction_created"
    }

    /**
     * The north star. Fires once ever when the user hits our activation
     * definition: ≥3 transactions across ≥2 distinct days. This is the
     * boundary between "installed" and "actually using the app" — every
     * retention / monetization number segments off it.
     */
    data object Activated : AnalyticsEvent {
        override val name = "activated"
    }

    // ───────────────────────── Engagement ─────────────────────────

    /** Every transaction add (not just the first). Type lets us see expense/income/transfer mix. */
    data class TransactionAdded(val type: String, val currency: String) : AnalyticsEvent {
        override val name = "transaction_added"
        override val params = mapOf("type" to type, "currency" to currency)
    }

    /** Every account add. */
    data class AccountAdded(val currency: String, val isLiability: Boolean) : AnalyticsEvent {
        override val name = "account_added"
        override val params = mapOf(
            "currency" to currency,
            "is_liability" to isLiability.toString()
        )
    }

    /** A recurring transaction was scheduled. */
    data class RecurringAdded(val frequency: String) : AnalyticsEvent {
        override val name = "recurring_added"
        override val params = mapOf("frequency" to frequency)
    }

    /** A financial goal was created. */
    data class GoalAdded(val tracking: String) : AnalyticsEvent {
        override val name = "goal_added"
        override val params = mapOf("tracking" to tracking)
    }

    /** Custom (user-defined) transaction category added. */
    data class CustomCategoryAdded(val type: String) : AnalyticsEvent {
        override val name = "custom_category_added"
        override val params = mapOf("type" to type)
    }

    /** User navigated to a past month — strong power-user signal. */
    data class HistoricalMonthViewed(val monthsBack: Int) : AnalyticsEvent {
        override val name = "historical_month_viewed"
        override val params = mapOf("months_back_bucket" to bucketMonths(monthsBack))

        private fun bucketMonths(n: Int): String = when {
            n <= 1 -> "0-1"
            n <= 3 -> "2-3"
            n <= 12 -> "4-12"
            else -> "13+"
        }
    }

    /** Existing transaction was edited — measures friction / correction rate. */
    data class TransactionEdited(val type: String) : AnalyticsEvent {
        override val name = "transaction_edited"
        override val params = mapOf("type" to type)
    }

    /** Existing transaction was deleted. Together with `edited` = "oops rate". */
    data class TransactionDeleted(val type: String) : AnalyticsEvent {
        override val name = "transaction_deleted"
        override val params = mapOf("type" to type)
    }

    /**
     * One-time per feature per install. Fires the first time the user does
     * a meaningful thing with a feature (adds a goal, sets a budget,
     * enables notifications, imports CSV, etc.). Lets us build an
     * "adoption %" chart per feature without noisy repeated events.
     *
     * `feature` values: "goal", "recurring", "budget", "csv_import",
     * "notifications", "app_lock", "asset_market_value".
     */
    data class FeatureAdopted(val feature: String) : AnalyticsEvent {
        override val name = "feature_adopted"
        override val params = mapOf("feature" to feature)
    }

    /**
     * Budget was set on a category. Amount is bucketed in base currency;
     * we never log raw amounts as event params. `category_type` is the
     * root category id so we can see whether users budget food, subs, etc.
     */
    data class BudgetSet(val categoryType: String, val amountBucket: String) : AnalyticsEvent {
        override val name = "budget_set"
        override val params = mapOf(
            "category_type" to categoryType,
            "amount_bucket" to amountBucket
        )
    }

    /** Budget removed / cleared. */
    data class BudgetRemoved(val categoryType: String) : AnalyticsEvent {
        override val name = "budget_removed"
        override val params = mapOf("category_type" to categoryType)
    }

    /**
     * Yellow "you've already spent X of your Y budget" warning appeared in
     * the Add Transaction sheet. Fires once per sheet-open. Measures how
     * often the budget signal is actually seen.
     */
    data class BudgetLimitExceededSeen(val categoryType: String) : AnalyticsEvent {
        override val name = "budget_limit_exceeded_seen"
        override val params = mapOf("category_type" to categoryType)
    }

    /**
     * User picked which account is primary for expenses / income. Measures
     * split-primary UX adoption. `role` is "expense", "income", or "both".
     */
    data class PrimaryAccountSet(val role: String) : AnalyticsEvent {
        override val name = "primary_account_set"
        override val params = mapOf("role" to role)
    }

    /**
     * User configured notifications. `mode` = "off" / "daily" / "weekly",
     * `time_bucket` = "morning" / "midday" / "evening" / "night". Bucketed
     * time reduces cardinality for the console dashboards.
     */
    data class NotificationConfigured(val mode: String, val timeBucket: String) : AnalyticsEvent {
        override val name = "notification_configured"
        override val params = mapOf("mode" to mode, "time_bucket" to timeBucket)
    }

    /** App was opened by tapping the reminder notification (attribution). */
    data object NotificationOpened : AnalyticsEvent {
        override val name = "notification_opened"
    }

    /** User tapped the swap-accounts button on a Transfer. Measures new UI adoption. */
    data object TransferSwapUsed : AnalyticsEvent {
        override val name = "transfer_swap_used"
    }

    data object CsvExported : AnalyticsEvent {
        override val name = "csv_exported"
    }

    data class CsvImported(val success: Boolean, val transactionCount: Int) : AnalyticsEvent {
        override val name = "csv_imported"
        override val params = mapOf(
            "success" to success.toString(),
            "transaction_count_bucket" to bucketCount(transactionCount)
        )
    }

    // ───────────────────────── Monetization ─────────────────────────

    /** User tried to add something past the free-tier limit (accounts / custom categories). */
    data class FeatureLimitHit(val limit: String) : AnalyticsEvent {
        override val name = "feature_limit_hit"
        override val params = mapOf("limit" to limit)
    }

    /** Paywall sheet was rendered. Trigger tells us which path got them here. */
    data class PaywallViewed(val trigger: String) : AnalyticsEvent {
        override val name = "paywall_viewed"
        override val params = mapOf("trigger" to trigger)
    }

    /** Paywall sheet dismissed without purchasing. */
    data class PaywallDismissed(val trigger: String) : AnalyticsEvent {
        override val name = "paywall_dismissed"
        override val params = mapOf("trigger" to trigger)
    }

    /** User tapped the Subscribe button (intent — not yet a purchase). */
    data class SubscribeTap(val productId: String, val trigger: String) : AnalyticsEvent {
        override val name = "subscribe_tap"
        override val params = mapOf("product_id" to productId, "trigger" to trigger)
    }

    /** Outcome of the StoreKit/Play Billing call. */
    data class SubscribeResult(val status: String, val productId: String) : AnalyticsEvent {
        override val name = "subscribe_result"
        override val params = mapOf("status" to status, "product_id" to productId)
    }

    data class RestorePurchasesTap(val success: Boolean) : AnalyticsEvent {
        override val name = "restore_purchases_tap"
        override val params = mapOf("success" to success.toString())
    }

    // ───────────────────────── Review & feedback ─────────────────────────

    /** "Enjoying Mooney?" pre-prompt was shown to the user. */
    data class ReviewPrepromptShown(val source: String) : AnalyticsEvent {
        override val name = "review_preprompt_shown"
        override val params = mapOf("source" to source)
    }

    /** How they answered the pre-prompt. */
    data class ReviewPrepromptResponse(val response: String) : AnalyticsEvent {
        override val name = "review_preprompt_response"
        override val params = mapOf("response" to response)
    }

// ───────────────────────── Misc (keep narrow) ─────────────────────────

    /** Base-currency switches are rare and meaningful — keep as event. */
    data class ChangeDefaultCurrency(val currency: String) : AnalyticsEvent {
        override val name = "change_default_currency"
        override val params = mapOf("currency" to currency)
    }

    /** Fun signal for who's exploring the app. */
    data object DeveloperOptionsUnlocked : AnalyticsEvent {
        override val name = "developer_options_unlocked"
    }

    // ───────────────────────── Internal / debug only ─────────────────────────

    /**
     * Fires when the bundled defaults version is bumped (e.g., category tree
     * update from Remote Config). Useful for verifying remote rollouts.
     */
    data class DefaultsVersionApplied(val version: Int, val source: String) : AnalyticsEvent {
        override val name = "defaults_version_applied"
        override val params = mapOf("version" to version.toString(), "source" to source)
    }

    /** Periodic snapshot of which bundled defaults categories are being used. */
    data class CategoryUsageSnapshot(
        val totalDefaults: Int,
        val usedDefaults: Int,
        val unusedDefaults: Int
    ) : AnalyticsEvent {
        override val name = "category_usage_snapshot"
        override val params = mapOf(
            "total" to totalDefaults.toString(),
            "used" to usedDefaults.toString(),
            "unused" to unusedDefaults.toString()
        )
    }
}

internal fun bucketCount(n: Int): String = when {
    n <= 0 -> "0"
    n <= 10 -> "1-10"
    n <= 50 -> "11-50"
    n <= 200 -> "51-200"
    n <= 1000 -> "201-1000"
    else -> "1000+"
}
