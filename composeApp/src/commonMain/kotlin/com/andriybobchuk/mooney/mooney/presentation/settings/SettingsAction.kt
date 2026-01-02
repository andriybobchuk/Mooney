package com.andriybobchuk.mooney.mooney.presentation.settings

import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.settings.ThemeMode

sealed interface SettingsAction {
    data object OnBackClick : SettingsAction
    
    data class OnCategorySelectionToggle(val category: Category) : SettingsAction
    
    data class OnPinnedCategoriesReorder(val fromIndex: Int, val toIndex: Int) : SettingsAction
    
    data class OnThemeModeChange(val themeMode: ThemeMode) : SettingsAction
    
    data class OnNotificationsToggle(val enabled: Boolean) : SettingsAction
    
    data class OnDefaultCurrencyChange(val currency: String) : SettingsAction
}