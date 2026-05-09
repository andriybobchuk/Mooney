package com.andriybobchuk.mooney.core.analytics

sealed interface AnalyticsEvent {
    val name: String
    val params: Map<String, String> get() = emptyMap()

    // Feature Adoption
    data object RefreshExchangeRates : AnalyticsEvent {
        override val name = "refresh_exchange_rates"
    }

    data class CycleCurrencyDisplay(val newCurrency: String) : AnalyticsEvent {
        override val name = "cycle_currency_display"
        override val params = mapOf("new_currency" to newCurrency)
    }

    data object ExportData : AnalyticsEvent {
        override val name = "export_data"
    }

    data class ImportData(val success: Boolean, val transactionCount: Int, val accountCount: Int) : AnalyticsEvent {
        override val name = "import_data"
        override val params = mapOf(
            "success" to success.toString(),
            "transaction_count" to transactionCount.toString(),
            "account_count" to accountCount.toString()
        )
    }

    data object ReconcileAccount : AnalyticsEvent {
        override val name = "reconcile_account"
    }

    // Settings & Preferences
    data class ChangeTheme(val theme: String) : AnalyticsEvent {
        override val name = "change_theme"
        override val params = mapOf("theme" to theme)
    }

    data class ChangeLanguage(val language: String) : AnalyticsEvent {
        override val name = "change_language"
        override val params = mapOf("language" to language)
    }

    data class ChangeDefaultCurrency(val currency: String) : AnalyticsEvent {
        override val name = "change_default_currency"
        override val params = mapOf("currency" to currency)
    }

    data class ToggleUserCurrency(val currency: String, val enabled: Boolean) : AnalyticsEvent {
        override val name = "toggle_user_currency"
        override val params = mapOf("currency" to currency, "enabled" to enabled.toString())
    }

    data class AddCustomCategory(val type: String) : AnalyticsEvent {
        override val name = "add_custom_category"
        override val params = mapOf("type" to type)
    }

    data object DeleteCustomCategory : AnalyticsEvent {
        override val name = "delete_custom_category"
    }

    data class CompleteOnboarding(val currency: String) : AnalyticsEvent {
        override val name = "complete_onboarding"
        override val params = mapOf("currency" to currency)
    }

    data class DefaultsVersionApplied(val version: Int, val source: String) : AnalyticsEvent {
        override val name = "defaults_version_applied"
        override val params = mapOf("version" to version.toString(), "source" to source)
    }

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
