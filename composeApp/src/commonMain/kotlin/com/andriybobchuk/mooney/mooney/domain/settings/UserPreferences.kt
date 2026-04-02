package com.andriybobchuk.mooney.mooney.domain.settings

import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.core.presentation.theme.AppTheme

data class UserPreferences(
    val pinnedCategories: List<String> = emptyList(),
    val defaultCurrency: String = "PLN",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appTheme: AppTheme = AppTheme.BLUE,
    val notificationsEnabled: Boolean = true,
    val version: Int = 1,
    val appLanguage: String = "system",
    val onboardingCompleted: Boolean = false,
    val defaultExpenseCategory: String = "groceries",
    val defaultIncomeCategory: String = "salary"
)

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

data class PinnedCategoryPreference(
    val categoryId: String,
    val order: Int
)