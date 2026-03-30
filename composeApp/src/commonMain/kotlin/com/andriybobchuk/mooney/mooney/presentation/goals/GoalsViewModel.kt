package com.andriybobchuk.mooney.mooney.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Goal
import com.andriybobchuk.mooney.mooney.domain.GoalTrackingType
import com.andriybobchuk.mooney.mooney.domain.GoalWithProgress
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.mooney.domain.usecase.DeleteGoalUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetGoalsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.EnrichGoalsWithProgressUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetAccountsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.SaveGoalUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class GoalsState(
    val goals: List<GoalWithProgress> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    val showAddGoalSheet: Boolean = false,
    val editingGoal: Goal? = null,
    val showDeleteDialog: Boolean = false,
    val goalToDelete: Goal? = null
)

sealed interface GoalsAction {
    data object ShowAddGoalSheet : GoalsAction
    data object HideAddGoalSheet : GoalsAction
    data class EditGoal(val goal: Goal) : GoalsAction
    data class ShowDeleteDialog(val goal: Goal) : GoalsAction
    data object HideDeleteDialog : GoalsAction
    data class ConfirmDeleteGoal(val goalId: Int) : GoalsAction
    data class SaveGoal(
        val title: String,
        val targetAmount: Double,
        val currency: Currency,
        val trackingType: GoalTrackingType,
        val accountId: Int?
    ) : GoalsAction
}

class GoalsViewModel(
    private val getGoalsUseCase: GetGoalsUseCase,
    private val saveGoalUseCase: SaveGoalUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
    private val enrichGoalsWithProgressUseCase: EnrichGoalsWithProgressUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsState())
    val state = _uiState
        .onStart {
            loadGoals()
            loadAccounts()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _uiState.value
        )

    fun onAction(action: GoalsAction) {
        when (action) {
            is GoalsAction.ShowAddGoalSheet -> {
                _uiState.update { it.copy(showAddGoalSheet = true, editingGoal = null) }
            }
            is GoalsAction.HideAddGoalSheet -> {
                _uiState.update { it.copy(showAddGoalSheet = false, editingGoal = null) }
            }
            is GoalsAction.EditGoal -> {
                _uiState.update { it.copy(showAddGoalSheet = true, editingGoal = action.goal) }
            }
            is GoalsAction.ShowDeleteDialog -> {
                _uiState.update { it.copy(showDeleteDialog = true, goalToDelete = action.goal) }
            }
            is GoalsAction.HideDeleteDialog -> {
                _uiState.update { it.copy(showDeleteDialog = false, goalToDelete = null) }
            }
            is GoalsAction.ConfirmDeleteGoal -> deleteGoal(action.goalId)
            is GoalsAction.SaveGoal -> saveGoal(
                action.title, action.targetAmount, action.currency,
                action.trackingType, action.accountId
            )
        }
    }

    private fun loadGoals() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                getGoalsUseCase().collect { goals ->
                    val goalsWithProgress = enrichGoalsWithProgressUseCase(goals)
                    _uiState.update {
                        it.copy(goals = goalsWithProgress, isLoading = false)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                analyticsTracker.recordException(e, "Goals")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            try {
                val accounts = getAccountsUseCase().first().filterNotNull()
                _uiState.update { it.copy(accounts = accounts) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                analyticsTracker.recordException(e, "Goals")
            }
        }
    }

    private fun saveGoal(
        title: String,
        targetAmount: Double,
        currency: Currency,
        trackingType: GoalTrackingType,
        accountId: Int?
    ) {
        viewModelScope.launch {
            try {
                saveGoalUseCase(
                    existingGoal = _uiState.value.editingGoal,
                    title = title,
                    targetAmount = targetAmount,
                    currency = currency,
                    trackingType = trackingType,
                    accountId = accountId
                )
                _uiState.update { it.copy(showAddGoalSheet = false, editingGoal = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                analyticsTracker.recordException(e, "Goals")
            }
        }
    }

    private fun deleteGoal(goalId: Int) {
        viewModelScope.launch {
            try {
                deleteGoalUseCase(goalId)
                _uiState.update { it.copy(showDeleteDialog = false, goalToDelete = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                analyticsTracker.recordException(e, "Goals")
                _uiState.update { it.copy(showDeleteDialog = false, goalToDelete = null) }
            }
        }
    }
}
