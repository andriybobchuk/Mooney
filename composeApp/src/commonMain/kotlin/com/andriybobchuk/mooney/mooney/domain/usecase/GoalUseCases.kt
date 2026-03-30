package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Goal
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.GoalTrackingType
import com.andriybobchuk.mooney.mooney.domain.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.toLocalDateTime

class AddGoalUseCase(
    private val repository: CoreRepository
) {
    suspend operator fun invoke(goal: Goal) {
        repository.upsertGoal(goal)
    }
}

class DeleteGoalUseCase(
    private val repository: CoreRepository
) {
    suspend operator fun invoke(goalId: Int) {
        repository.deleteGoal(goalId)
    }
}

class GetGoalsUseCase(
    private val repository: CoreRepository
) {
    operator fun invoke(): Flow<List<Goal>> {
        return repository.getAllGoals()
    }
}

class CalculateGoalProgressUseCase(
    private val repository: CoreRepository,
    private val currencyManagerUseCase: CurrencyManagerUseCase
) {
    suspend operator fun invoke(goal: Goal): GoalProgressResult {
        val baseCurrency = GlobalConfig.baseCurrency
        val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
        val accounts = repository.getAllAccounts().first().filterNotNull()

        val currentAmount = when (goal.trackingType) {
            GoalTrackingType.ACCOUNT -> {
                val account = accounts.find { it.id == goal.accountId }
                if (account != null) {
                    if (account.currency != baseCurrency) {
                        exchangeRates.convert(account.amount, account.currency, baseCurrency)
                    } else {
                        account.amount
                    }
                } else 0.0
            }
            GoalTrackingType.NET_WORTH -> {
                accounts.sumOf { account ->
                    val amountInBase = if (account.currency != baseCurrency) {
                        exchangeRates.convert(account.amount, account.currency, baseCurrency)
                    } else {
                        account.amount
                    }
                    if (account.isLiability) -amountInBase else amountInBase
                }
            }
            GoalTrackingType.TOTAL_ASSETS -> {
                accounts.filter { !it.isLiability }.sumOf { account ->
                    if (account.currency != baseCurrency) {
                        exchangeRates.convert(account.amount, account.currency, baseCurrency)
                    } else {
                        account.amount
                    }
                }
            }
        }

        val targetInBase = if (goal.currency != baseCurrency) {
            exchangeRates.convert(goal.targetAmount, goal.currency, baseCurrency)
        } else {
            goal.targetAmount
        }

        val progressPercentage = if (targetInBase > 0) {
            (currentAmount / targetInBase * 100).coerceAtLeast(0.0)
        } else 0.0

        val remainingAmount = (targetInBase - currentAmount).coerceAtLeast(0.0)

        return GoalProgressResult(
            savedAmount = currentAmount,
            targetAmount = targetInBase,
            remainingAmount = remainingAmount,
            progressPercentage = progressPercentage,
            baseCurrency = baseCurrency
        )
    }
}

class EstimateGoalCompletionUseCase(
    private val calculateGoalProgressUseCase: CalculateGoalProgressUseCase,
    private val repository: CoreRepository,
    private val currencyManagerUseCase: CurrencyManagerUseCase
) {
    suspend operator fun invoke(goal: Goal): GoalCompletionEstimate {
        val progress = calculateGoalProgressUseCase(goal)

        if (progress.progressPercentage >= 100.0) {
            return GoalCompletionEstimate.AlreadyCompleted
        }

        val baseCurrency = GlobalConfig.baseCurrency
        val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
        val allTransactions = repository.getAllTransactions().first().filterNotNull()

        val now = kotlinx.datetime.Clock.System.now()
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        val sixMonthsAgo = kotlinx.datetime.LocalDate(
            year = if (now.monthNumber <= 6) now.year - 1 else now.year,
            monthNumber = if (now.monthNumber <= 6) now.monthNumber + 6 else now.monthNumber - 6,
            dayOfMonth = 1
        )

        // Filter transactions relevant to this goal's tracking type
        val relevantTransactions = allTransactions.filter { txn ->
            txn.date >= sixMonthsAgo && txn.subcategory.type != CategoryType.TRANSFER && when (goal.trackingType) {
                GoalTrackingType.ACCOUNT -> txn.account.id == goal.accountId
                GoalTrackingType.NET_WORTH -> true
                GoalTrackingType.TOTAL_ASSETS -> !txn.account.isLiability
            }
        }

        if (relevantTransactions.isEmpty()) {
            return GoalCompletionEstimate.InProgress(
                remainingAmount = progress.remainingAmount,
                baseCurrency = baseCurrency,
                monthlySavingsRate = 0.0,
                estimatedMonths = null,
                estimatedCompletionLabel = null,
                timeline = emptyList()
            )
        }

        // Monthly net savings = income - expenses (amounts are always positive, type determines direction)
        val monthlyNets = relevantTransactions.groupBy {
            "${it.date.year}-${it.date.monthNumber.toString().padStart(2, '0')}"
        }.map { (_, txns) ->
            txns.sumOf { txn ->
                val amountInBase = if (txn.account.currency != baseCurrency) {
                    exchangeRates.convert(txn.amount, txn.account.currency, baseCurrency)
                } else {
                    txn.amount
                }
                when (txn.subcategory.type) {
                    CategoryType.INCOME -> amountInBase
                    CategoryType.EXPENSE -> -amountInBase
                    CategoryType.TRANSFER -> 0.0
                }
            }
        }

        val avgMonthlySavings = if (monthlyNets.isNotEmpty()) monthlyNets.average() else 0.0

        if (avgMonthlySavings <= 0) {
            return GoalCompletionEstimate.InProgress(
                remainingAmount = progress.remainingAmount,
                baseCurrency = baseCurrency,
                monthlySavingsRate = avgMonthlySavings,
                estimatedMonths = null,
                estimatedCompletionLabel = null,
                timeline = emptyList()
            )
        }

        val estimatedMonths = kotlin.math.ceil(progress.remainingAmount / avgMonthlySavings).toInt()
            .coerceAtLeast(1)

        // Estimated completion month label (e.g., "June 2026")
        val completionDate = kotlinx.datetime.LocalDate(
            year = if (now.monthNumber + estimatedMonths > 12) now.year + (now.monthNumber + estimatedMonths - 1) / 12 else now.year,
            monthNumber = (now.monthNumber + estimatedMonths - 1) % 12 + 1,
            dayOfMonth = 1
        )
        val completionLabel = "${completionDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${completionDate.year}"

        // Build monthly projection timeline (up to 12 months)
        val timelineMonths = estimatedMonths.coerceAtMost(12)
        val timeline = (1..timelineMonths).map { month ->
            val projectedAmount = (progress.savedAmount + avgMonthlySavings * month)
                .coerceAtMost(progress.targetAmount)
            val monthDate = kotlinx.datetime.LocalDate(
                year = if (now.monthNumber + month > 12) now.year + (now.monthNumber + month - 1) / 12 else now.year,
                monthNumber = (now.monthNumber + month - 1) % 12 + 1,
                dayOfMonth = 1
            )
            MonthProjection(
                monthLabel = monthDate.month.name.take(3).lowercase()
                    .replaceFirstChar { it.uppercase() },
                year = monthDate.year,
                projectedAmount = projectedAmount,
                isCompletionMonth = month == estimatedMonths
            )
        }

        return GoalCompletionEstimate.InProgress(
            remainingAmount = progress.remainingAmount,
            baseCurrency = baseCurrency,
            monthlySavingsRate = avgMonthlySavings,
            estimatedMonths = estimatedMonths,
            estimatedCompletionLabel = completionLabel,
            timeline = timeline
        )
    }
}

data class GoalProgressResult(
    val savedAmount: Double,
    val targetAmount: Double,
    val remainingAmount: Double,
    val progressPercentage: Double,
    val baseCurrency: Currency
)

data class MonthProjection(
    val monthLabel: String,
    val year: Int,
    val projectedAmount: Double,
    val isCompletionMonth: Boolean
)

sealed interface GoalCompletionEstimate {
    data object AlreadyCompleted : GoalCompletionEstimate
    data object CannotEstimate : GoalCompletionEstimate
    data class InProgress(
        val remainingAmount: Double,
        val baseCurrency: Currency,
        val monthlySavingsRate: Double,
        val estimatedMonths: Int?,
        val estimatedCompletionLabel: String?,
        val timeline: List<MonthProjection>
    ) : GoalCompletionEstimate
}
