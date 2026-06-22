package com.andriybobchuk.mooney.mooney.presentation.settings

import com.andriybobchuk.mooney.core.data.database.AssetCategoryEntity
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.UserCurrency
import com.andriybobchuk.mooney.mooney.domain.settings.ExchangeRateSource
import com.andriybobchuk.mooney.mooney.domain.settings.ThemeMode

data class SettingsState(
    val isLoading: Boolean = true,
    val allCategories: List<Category> = emptyList(),
    val pinnedCategoryIds: Set<String> = emptySet(),
    val pinnedCategories: List<Category> = emptyList(),
    val currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val defaultCurrency: Currency = Currency.USD,
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
    val primaryAccountId: Int? = null,
    val restoreMessage: String? = null,
    val currencyInsightsEnabled: Boolean = false,
    val isUpdatingCategories: Boolean = false,
    val exchangeRateSource: ExchangeRateSource = ExchangeRateSource.EXTENDED,
    val developerOptionsEnabled: Boolean = false,
    /** Local override for Pro plan — used by the Developer Plan toggle. */
    val devForcePremium: Boolean = false,
    /** Dev opt-in: when true, the Transactions screen shows the multi-widget pager again. */
    val widgetPagerEnabled: Boolean = false,
    /**
     * Dev kill-switch for all ads. Overrides every other gate in
     * [com.andriybobchuk.mooney.core.ads.AdEligibilityUseCase] so the dev can
     * preview the ad-free UX inside the same build that normally serves
     * ads. Persists across launches.
     */
    val adsDisabled: Boolean = false,
    /**
     * Developer Options: force-show ads — bypasses new-user grace + banner
     * cooldown so every eligible placement fills immediately. Persists.
     */
    val adsForceShow: Boolean = false,
    /** "OFF" / "DAILY" / "WEEKLY" — drives the Reminders sheet selection. */
    val reminderMode: String = "OFF",
    /** Local-time hour for the reminder (0–23). */
    val reminderHour: Int = 20,
    /** Local-time minute for the reminder (0–59). */
    val reminderMinute: Int = 0,
    /** ISO weekday for WEEKLY reminders: 1 = Mon … 7 = Sun. */
    val reminderWeekday: Int = 7
) {
    val maxPinnedCategories: Int = 5
    val canAddMorePinned: Boolean = pinnedCategoryIds.size < maxPinnedCategories
}
