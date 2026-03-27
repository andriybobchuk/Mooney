package com.andriybobchuk.mooney.mooney.presentation.settings

import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.UserCurrency
import com.andriybobchuk.mooney.mooney.domain.settings.ThemeMode

data class SettingsState(
    val isLoading: Boolean = true,
    val allCategories: List<Category> = emptyList(),
    val pinnedCategoryIds: Set<String> = emptySet(),
    val pinnedCategories: List<Category> = emptyList(),
    val currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val defaultCurrency: Currency = Currency.PLN,
    val availableCurrencies: List<Currency> = Currency.entries,
    val userCurrencies: List<UserCurrency> = emptyList(),
    val error: String? = null,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val appLanguage: String = "system"
) {
    val maxPinnedCategories: Int = 5
    val canAddMorePinned: Boolean = pinnedCategoryIds.size < maxPinnedCategories
}