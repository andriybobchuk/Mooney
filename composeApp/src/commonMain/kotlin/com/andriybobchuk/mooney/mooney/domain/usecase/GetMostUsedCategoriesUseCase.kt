package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.data.CategoryDataSource
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CoreRepository

class GetMostUsedCategoriesUseCase(
    private val repository: CoreRepository
) {
    suspend operator fun invoke(limit: Int = 8): List<Category> {
        // Get the actual most used categories from database
        val databaseCategories = repository.getMostUsedCategories(limit)
        
        // If database has insufficient data, supplement with popular defaults
        return if (databaseCategories.size >= limit) {
            databaseCategories.take(limit)
        } else {
            val fallbackCategories = listOf(
                CategoryDataSource.groceries,
                CategoryDataSource.businessSub.first { it.id == "software" },
                CategoryDataSource.joySub.first { it.id == "joy_dates" },
                CategoryDataSource.healthSub.first { it.id == "health_medications" },
                CategoryDataSource.sportSub.first { it.id == "sport_gym" },
                CategoryDataSource.transportSub.first { it.id == "transport_taxi" },
                CategoryDataSource.subscriptionsSub.first { it.id == "spotify" },
                CategoryDataSource.beveragesSub.first { it.id == "eating_out" }
            )
            
            // Combine database results with fallback, avoid duplicates
            val combined = (databaseCategories + fallbackCategories)
                .distinctBy { it.id }
                .take(limit)
            
            combined
        }
    }
}