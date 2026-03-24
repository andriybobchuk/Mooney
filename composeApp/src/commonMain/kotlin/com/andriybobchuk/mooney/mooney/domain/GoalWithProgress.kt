package com.andriybobchuk.mooney.mooney.domain

import com.andriybobchuk.mooney.mooney.domain.usecase.GoalCompletionEstimate
import com.andriybobchuk.mooney.mooney.domain.usecase.GoalProgressResult

data class GoalWithProgress(
    val goal: Goal,
    val progress: GoalProgressResult?,
    val completionEstimate: GoalCompletionEstimate?
)
