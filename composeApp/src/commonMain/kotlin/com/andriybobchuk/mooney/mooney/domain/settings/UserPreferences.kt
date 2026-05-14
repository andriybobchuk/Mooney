package com.andriybobchuk.mooney.mooney.domain.settings

import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.core.presentation.theme.AppTheme

data class UserPreferences(
    val pinnedCategories: List<String> = emptyList(),
    val defaultCurrency: String = "USD",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appTheme: AppTheme = AppTheme.BLUE,
    val notificationsEnabled: Boolean = true,
    val version: Int = 1,
    val appLanguage: String = "system",
    val onboardingCompleted: Boolean = false,
    val defaultExpenseCategory: String = "groceries",
    val defaultIncomeCategory: String = "salary",
    val excludeTaxesFromTotals: Boolean = true,
    val exchangeRateSource: ExchangeRateSource = ExchangeRateSource.EXTENDED
)

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class ExchangeRateSource {
    /** exchangerate-api.com — supports all currencies including UAH/RUB/AED, no historical data. */
    EXTENDED,

    /** Frankfurter (ECB) — has historical data but only ~30 currencies; rest use approximate fallback rates. */
    HISTORICAL
}

data class PinnedCategoryPreference(
    val categoryId: String,
    val order: Int
)