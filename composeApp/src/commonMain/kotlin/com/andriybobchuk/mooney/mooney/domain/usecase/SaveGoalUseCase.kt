package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Goal
import com.andriybobchuk.mooney.mooney.domain.GoalTrackingType

class SaveGoalUseCase(
    private val addGoalUseCase: AddGoalUseCase
) {
    suspend operator fun invoke(
        existingGoal: Goal?,
        title: String,
        targetAmount: Double,
        currency: Currency,
        trackingType: GoalTrackingType,
        accountId: Int?
    ) {
        val goal = existingGoal?.copy(
            title = title,
            targetAmount = targetAmount,
            currency = currency,
            trackingType = trackingType,
            accountId = accountId
        ) ?: Goal(
            id = 0,
            emoji = "",
            title = title,
            description = "",
            targetAmount = targetAmount,
            currency = currency,
            trackingType = trackingType,
            accountId = accountId
        )
        addGoalUseCase(goal)
    }
}
