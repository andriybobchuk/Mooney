package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CoreRepository

class GetMostUsedCategoriesUseCase(
    private val repository: CoreRepository
) {
    private val fallbackIds = listOf(
        "groceries", "software", "joy_dates", "health_medications",
        "sport_gym", "transport_taxi", "spotify", "eating_out"
    )

    suspend operator fun invoke(limit: Int = 8): List<Category> {
        val databaseCategories = repository.getMostUsedCategories(limit)

        return if (databaseCategories.size >= limit) {
            databaseCategories.take(limit)
        } else {
            val fallbackCategories = fallbackIds.mapNotNull { repository.getCategoryById(it) }
            (databaseCategories + fallbackCategories)
                .distinctBy { it.id }
                .take(limit)
        }
    }
}