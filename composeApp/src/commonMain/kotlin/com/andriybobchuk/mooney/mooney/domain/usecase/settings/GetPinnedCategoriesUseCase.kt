package com.andriybobchuk.mooney.mooney.domain.usecase.settings

import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.usecase.GetCategoriesUseCase
import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository
import com.andriybobchuk.mooney.mooney.domain.settings.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetPinnedCategoriesUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val getCategoriesUseCase: GetCategoriesUseCase
) {
    operator fun invoke(): Flow<List<Category>> {
        return preferencesRepository.getUserPreferences().map { preferences ->
            val allCategories = getCategoriesUseCase()
            val pinnedIds = preferences.pinnedCategories
            val categoryMap = allCategories.associateBy { it.id }
            
            pinnedIds.mapNotNull { id -> categoryMap[id] }
        }
    }
}