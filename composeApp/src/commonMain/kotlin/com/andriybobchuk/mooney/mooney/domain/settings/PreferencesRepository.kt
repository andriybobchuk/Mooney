package com.andriybobchuk.mooney.mooney.domain.settings

import com.andriybobchuk.mooney.core.presentation.theme.AppTheme
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    
    fun getUserPreferences(): Flow<UserPreferences>
    
    suspend fun updatePinnedCategories(categoryIds: List<String>)
    
    suspend fun updateDefaultCurrency(currency: String)
    
    suspend fun updateThemeMode(themeMode: ThemeMode)
    
    suspend fun updateAppTheme(appTheme: AppTheme)
    
    suspend fun updateNotificationsEnabled(enabled: Boolean)
    
    suspend fun updateAppLanguage(language: String)

    suspend fun updateDefaultExpenseCategory(categoryId: String)

    suspend fun updateDefaultIncomeCategory(categoryId: String)

    suspend fun updateExcludeTaxesFromTotals(enabled: Boolean)

    suspend fun markOnboardingCompleted()

    suspend fun clearPreferences()

    suspend fun getCurrentPreferences(): UserPreferences
}