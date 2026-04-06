package com.andriybobchuk.mooney.mooney.presentation.settings

import com.andriybobchuk.mooney.core.data.database.AssetCategoryEntity
import com.andriybobchuk.mooney.mooney.domain.Account
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
    val appLanguage: String = "system",
    val showPaywall: Boolean = false,
    val isPurchasing: Boolean = false,
    val purchaseError: String? = null,
    val excludeTaxesFromTotals: Boolean = true,
    val assetCategories: List<AssetCategoryEntity> = emptyList(),
    val defaultExpenseCategoryId: String = "groceries",
    val defaultIncomeCategoryId: String = "salary",
    val accounts: List<Account> = emptyList(),
    val primaryAccountId: Int? = null
) {
    val maxPinnedCategories: Int = 5
    val canAddMorePinned: Boolean = pinnedCategoryIds.size < maxPinnedCategories
}
