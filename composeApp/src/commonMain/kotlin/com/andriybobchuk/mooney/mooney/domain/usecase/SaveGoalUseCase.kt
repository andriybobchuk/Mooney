package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Goal

class SaveGoalUseCase(
    private val addGoalUseCase: AddGoalUseCase
) {
    suspend operator fun invoke(
        existingGoal: Goal?,
        emoji: String,
        title: String,
        description: String,
        targetAmount: Double,
        currency: Currency
    ) {
        val goal = existingGoal?.copy(
            emoji = emoji,
            title = title,
            description = description,
            targetAmount = targetAmount,
            currency = currency
        ) ?: Goal(
            id = 0,
            emoji = emoji,
            title = title,
            description = description,
            targetAmount = targetAmount,
            currency = currency,
            groupName = "General"
        )

        addGoalUseCase(goal)
    }
}
