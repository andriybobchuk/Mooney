package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Goal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
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
        val accounts = repository.getAllAccounts()
        val totalSavedInBaseCurrency = currencyManagerUseCase.convertAllAccountsToBaseCurrency(accounts)
        val goalAmountInBaseCurrency = currencyManagerUseCase.convertToBaseCurrency(goal.targetAmount, goal.currency)
        
        val progressPercentage = if (goalAmountInBaseCurrency > 0) {
            (totalSavedInBaseCurrency / goalAmountInBaseCurrency * 100).coerceAtMost(100.0)
        } else {
            0.0
        }
        
        val remainingAmount = (goalAmountInBaseCurrency - totalSavedInBaseCurrency).coerceAtLeast(0.0)
        
        // Calculate monthly progress
        val monthlyProgress = calculateMonthlyProgress(totalSavedInBaseCurrency, goal)
        
        return GoalProgressResult(
            savedAmount = totalSavedInBaseCurrency,
            targetAmount = goalAmountInBaseCurrency,
            remainingAmount = remainingAmount,
            progressPercentage = progressPercentage,
            baseCurrency = currencyManagerUseCase.getCurrentBaseCurrency(),
            monthlyProgressPercentage = monthlyProgress
        )
    }
    
    private suspend fun calculateMonthlyProgress(currentTotalSaved: Double, goal: Goal): Double {
        try {
            // Calculate current month's net income (savings after taxes and expenses)
            val monthlyNetSavings = calculateCurrentMonthNetIncome()
            val goalAmountInBaseCurrency = currencyManagerUseCase.convertToBaseCurrency(goal.targetAmount, goal.currency)
            
            // Calculate what percentage of the goal was achieved this month
            if (goalAmountInBaseCurrency > 0) {
                return (monthlyNetSavings / goalAmountInBaseCurrency * 100)
            }
            
            return 0.0
        } catch (e: Exception) {
            return 0.0
        }
    }
    
    suspend fun calculateCurrentMonthNetIncome(): Double {
        try {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val startOfMonth = LocalDate(now.year, now.month, 1)
            val endOfMonth = startOfMonth.plus(1, DateTimeUnit.MONTH)
            
            val transactions = repository.getAllTransactions().first().filterNotNull()
            val currentMonthTransactions = transactions.filter { 
                it.date >= startOfMonth && it.date < endOfMonth 
            }
            
            val baseCurrency = currencyManagerUseCase.getCurrentBaseCurrency()
            val exchangeRates = GlobalConfig.testExchangeRates
            
            // Calculate revenue (income)
            val revenue = currentMonthTransactions
                .filter { it.subcategory.type == CategoryType.INCOME }
                .sumOf { 
                    exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
                }
            
            // Calculate taxes (ZUS + PIT)
            val taxes = currentMonthTransactions
                .filter { 
                    it.subcategory.title.contains("ZUS", ignoreCase = true) || 
                    it.subcategory.title.contains("PIT", ignoreCase = true) 
                }
                .sumOf { 
                    exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
                }
            
            // Calculate expenses (excluding taxes)
            val expenses = currentMonthTransactions
                .filter { 
                    it.subcategory.type == CategoryType.EXPENSE &&
                    !it.subcategory.title.contains("ZUS", ignoreCase = true) &&
                    !it.subcategory.title.contains("PIT", ignoreCase = true)
                }
                .sumOf { 
                    exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
                }
            
            return revenue - taxes - expenses
        } catch (e: Exception) {
            return 0.0
        }
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
        
        // Use the same calculation as in CalculateGoalProgressUseCase for consistency
        val progressCalculator = CalculateGoalProgressUseCase(repository, currencyManagerUseCase)
        val monthlyNetIncome = progressCalculator.calculateCurrentMonthNetIncome()
        
        if (monthlyNetIncome <= 0) {
            return GoalCompletionEstimate.CannotEstimate
        }
        
        val monthsToCompletion = (progress.remainingAmount / monthlyNetIncome).toInt().coerceAtLeast(1)
        val targetDate = calculateTargetDate(monthsToCompletion)
        
        return GoalCompletionEstimate.EstimatedCompletion(
            months = monthsToCompletion,
            targetDate = targetDate,
            monthlySavingsRate = monthlyNetIncome,
            baseCurrency = progress.baseCurrency
        )
    }
    
    private fun calculateTargetDate(monthsToCompletion: Int): LocalDate {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return now.plus(monthsToCompletion, DateTimeUnit.MONTH)
    }
}

data class GoalProgressResult(
    val savedAmount: Double,
    val targetAmount: Double,
    val remainingAmount: Double,
    val progressPercentage: Double,
    val baseCurrency: Currency,
    val monthlyProgressPercentage: Double
)

sealed interface GoalCompletionEstimate {
    object AlreadyCompleted : GoalCompletionEstimate
    object CannotEstimate : GoalCompletionEstimate
    data class EstimatedCompletion(
        val months: Int,
        val targetDate: LocalDate,
        val monthlySavingsRate: Double,
        val baseCurrency: Currency
    ) : GoalCompletionEstimate
}