package com.andriybobchuk.mooney.mooney.domain.usecase.settings

import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository

class UpdatePinnedCategoriesUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(categoryIds: List<String>) {
        require(categoryIds.size <= 5) { "Cannot pin more than 5 categories" }
        preferencesRepository.updatePinnedCategories(categoryIds)
    }
}