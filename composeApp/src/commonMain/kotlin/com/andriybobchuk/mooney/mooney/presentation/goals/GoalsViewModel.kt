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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class GoalsState(
    val goals: List<GoalWithProgress> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    /**
     * Stays `true` until the AppDataCache has emitted its first snapshot —
     * the only window where the user could see a flashed empty state before
     * data arrives. Drives both the shimmer and the empty-state gate.
     */
    val isInitialLoading: Boolean = true,
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
    private val saveGoalUseCase: SaveGoalUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
    private val enrichGoalsWithProgressUseCase: EnrichGoalsWithProgressUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val appDataCache: com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache
) : ViewModel() {

    // Always start with isInitialLoading=true. Seeding from cache.isReady
    // let a warm-cache screen entry skip the shimmer and paint the empty-
    // state CTA directly — user reported the "no shimmer, just empty" flash
    // on Transactions and the same anti-pattern lived here too. Shimmer →
    // content (or empty) is the required order.
    private val _uiState = MutableStateFlow(GoalsState(isInitialLoading = true))
    val state = _uiState
        .onStart { observeFromCache() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _uiState.value
        )

    private fun observeFromCache() {
        // Goals + accounts come from the singleton app cache, so tab switches
        // and re-entries paint the previous snapshot on the very first frame.
        appDataCache.snapshot.onEach { snapshot ->
            if (!snapshot.isReady) return@onEach
            try {
                val enriched = enrichGoalsWithProgressUseCase(snapshot.goals)
                _uiState.update {
                    it.copy(
                        goals = enriched,
                        accounts = snapshot.accounts,
                        isLoading = false,
                        isInitialLoading = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                analyticsTracker.recordException(e, "Goals")
                _uiState.update { it.copy(isLoading = false, isInitialLoading = false) }
            }
        }.launchIn(viewModelScope)
    }

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

    private fun saveGoal(
        title: String,
        targetAmount: Double,
        currency: Currency,
        trackingType: GoalTrackingType,
        accountId: Int?
    ) {
        viewModelScope.launch {
            try {
                val wasEdit = _uiState.value.editingGoal != null
                saveGoalUseCase(
                    existingGoal = _uiState.value.editingGoal,
                    title = title,
                    targetAmount = targetAmount,
                    currency = currency,
                    trackingType = trackingType,
                    accountId = accountId
                )
                if (!wasEdit) {
                    analyticsTracker.trackEvent(
                        com.andriybobchuk.mooney.core.analytics.AnalyticsEvent.GoalAdded(trackingType.name)
                    )
                    // First-ever goal → one-time adoption signal. `goals.isEmpty()`
                    // reflects state _before_ this add (cache hasn't refreshed
                    // yet), so this is a reliable one-shot without needing a
                    // DataStore flag.
                    if (_uiState.value.goals.isEmpty()) {
                        analyticsTracker.trackEvent(
                            com.andriybobchuk.mooney.core.analytics.AnalyticsEvent.FeatureAdopted("goal")
                        )
                    }
                }
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
