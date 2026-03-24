package com.andriybobchuk.mooney.mooney.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Goal
import com.andriybobchuk.mooney.mooney.domain.GoalWithProgress
import com.andriybobchuk.mooney.mooney.domain.usecase.DeleteGoalUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetGoalsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.CurrencyManagerUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.EnrichGoalsWithProgressUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.SaveGoalUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoalsState(
    val goals: List<GoalWithProgress> = emptyList(),
    val currentGoalIndex: Int = 0,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val showAddGoalSheet: Boolean = false,
    val editingGoal: Goal? = null,
    val showDeleteDialog: Boolean = false,
    val goalToDelete: Goal? = null
)

sealed interface GoalsAction {
    object LoadGoals : GoalsAction
    object RefreshExchangeRates : GoalsAction
    data class SwipeToGoal(val index: Int) : GoalsAction
    object ShowAddGoalSheet : GoalsAction
    object HideAddGoalSheet : GoalsAction
    data class EditGoal(val goal: Goal) : GoalsAction
    object CancelEditGoal : GoalsAction
    data class ShowDeleteDialog(val goal: Goal) : GoalsAction
    object HideDeleteDialog : GoalsAction
    data class ConfirmDeleteGoal(val goalId: Int) : GoalsAction
    data class SaveGoal(
        val emoji: String,
        val title: String,
        val description: String,
        val targetAmount: Double,
        val currency: Currency
    ) : GoalsAction
}

class GoalsViewModel(
    private val getGoalsUseCase: GetGoalsUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
    private val currencyManagerUseCase: CurrencyManagerUseCase,
    private val enrichGoalsWithProgressUseCase: EnrichGoalsWithProgressUseCase,
    private val saveGoalUseCase: SaveGoalUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsState())
    val state = _uiState
        .onStart { onAction(GoalsAction.LoadGoals) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _uiState.value
        )

    fun onAction(action: GoalsAction) {
        when (action) {
            is GoalsAction.LoadGoals -> loadGoals()
            is GoalsAction.RefreshExchangeRates -> refreshExchangeRates()
            is GoalsAction.SwipeToGoal -> {
                _uiState.update { it.copy(currentGoalIndex = action.index) }
            }
            is GoalsAction.ShowAddGoalSheet -> {
                _uiState.update { it.copy(showAddGoalSheet = true, editingGoal = null) }
            }
            is GoalsAction.HideAddGoalSheet -> {
                _uiState.update { it.copy(showAddGoalSheet = false, editingGoal = null) }
            }
            is GoalsAction.EditGoal -> {
                _uiState.update { it.copy(showAddGoalSheet = true, editingGoal = action.goal) }
            }
            is GoalsAction.CancelEditGoal -> {
                _uiState.update { it.copy(showAddGoalSheet = false, editingGoal = null) }
            }
            is GoalsAction.ShowDeleteDialog -> {
                _uiState.update { it.copy(showDeleteDialog = true, goalToDelete = action.goal) }
            }
            is GoalsAction.HideDeleteDialog -> {
                _uiState.update { it.copy(showDeleteDialog = false, goalToDelete = null) }
            }
            is GoalsAction.ConfirmDeleteGoal -> deleteGoal(action.goalId)
            is GoalsAction.SaveGoal -> saveGoal(
                action.emoji, action.title, action.description,
                action.targetAmount, action.currency
            )
        }
    }

    private fun loadGoals() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false) }

            try {
                getGoalsUseCase().collect { goals ->
                    val goalsWithProgress = enrichGoalsWithProgressUseCase(goals)

                    _uiState.update {
                        it.copy(
                            goals = goalsWithProgress,
                            isLoading = false,
                            currentGoalIndex = if (goalsWithProgress.isNotEmpty() && it.currentGoalIndex >= goalsWithProgress.size) 0 else it.currentGoalIndex
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isError = true) }
            }
        }
    }

    private fun saveGoal(emoji: String, title: String, description: String, targetAmount: Double, currency: Currency) {
        viewModelScope.launch {
            try {
                saveGoalUseCase(
                    existingGoal = _uiState.value.editingGoal,
                    emoji = emoji,
                    title = title,
                    description = description,
                    targetAmount = targetAmount,
                    currency = currency
                )
                _uiState.update { it.copy(showAddGoalSheet = false, editingGoal = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isError = true) }
            }
        }
    }

    private fun deleteGoal(goalId: Int) {
        viewModelScope.launch {
            try {
                deleteGoalUseCase(goalId)
                _uiState.update { it.copy(showDeleteDialog = false, goalToDelete = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isError = true, showDeleteDialog = false, goalToDelete = null) }
            }
        }
    }

    private fun refreshExchangeRates() {
        viewModelScope.launch {
            try {
                currencyManagerUseCase.refreshExchangeRates()
                loadGoals()
            } catch (e: Exception) {
                _uiState.update { it.copy(isError = true) }
            }
        }
    }
}
