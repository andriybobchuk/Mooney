package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Goal
import com.andriybobchuk.mooney.mooney.domain.GoalWithProgress

class EnrichGoalsWithProgressUseCase(
    private val calculateGoalProgressUseCase: CalculateGoalProgressUseCase,
    private val estimateGoalCompletionUseCase: EstimateGoalCompletionUseCase
) {
    suspend operator fun invoke(goals: List<Goal>): List<GoalWithProgress> {
        return goals.map { goal ->
            val progress = try {
                calculateGoalProgressUseCase(goal)
            } catch (_: Exception) {
                null
            }

            val estimate = try {
                estimateGoalCompletionUseCase(goal)
            } catch (_: Exception) {
                GoalCompletionEstimate.CannotEstimate
            }

            GoalWithProgress(goal, progress, estimate)
        }.sortedBy { it.goal.targetAmount }
    }
}
